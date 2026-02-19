package com.heditra.inventoryservice.service.impl;

import com.heditra.events.core.EventPublisher;
import com.heditra.events.inventory.InventoryReleasedEvent;
import com.heditra.events.inventory.InventoryReservedEvent;
import com.heditra.inventoryservice.dto.request.CreateInventoryRequest;
import com.heditra.inventoryservice.dto.request.UpdateInventoryRequest;
import com.heditra.inventoryservice.dto.response.InventoryResponse;
import com.heditra.inventoryservice.exception.DuplicateEventException;
import com.heditra.inventoryservice.exception.InsufficientInventoryException;
import com.heditra.inventoryservice.exception.InventoryNotFoundException;
import com.heditra.inventoryservice.mapper.InventoryMapper;
import com.heditra.inventoryservice.model.Inventory;
import com.heditra.inventoryservice.repository.InventoryRepository;
import com.heditra.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private static final String LOCK_KEY_PREFIX = "inventory:lock:";
    private static final long WAIT_TIME = 10;
    private static final long LEASE_TIME = 5;

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final EventPublisher eventPublisher;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (inventoryRepository.existsByEventName(request.getEventName())) {
            throw new DuplicateEventException(request.getEventName());
        }

        Inventory inventory = inventoryMapper.toEntity(request);
        Inventory saved = inventoryRepository.save(inventory);

        log.info("Inventory created: id={}, event={}", saved.getId(), saved.getEventName());
        return inventoryMapper.toResponse(saved);
    }

    @Override
    @Cacheable(value = "inventory", key = "#id")
    public InventoryResponse getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException(id));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Cacheable(value = "inventory", key = "'event:' + #eventName")
    public InventoryResponse getInventoryByEventName(String eventName) {
        Inventory inventory = inventoryRepository.findByEventName(eventName)
                .orElseThrow(() -> new InventoryNotFoundException(eventName));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public List<InventoryResponse> getAllInventory() {
        return inventoryMapper.toResponseList(inventoryRepository.findAll());
    }

    @Override
    @Cacheable(value = "availableEvents")
    public List<InventoryResponse> getAvailableEvents() {
        return inventoryMapper.toResponseList(
                inventoryRepository.findAvailableEvents(LocalDateTime.now()));
    }

    @Override
    public List<InventoryResponse> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        return inventoryMapper.toResponseList(
                inventoryRepository.findByEventDateBetween(start, end));
    }

    @Override
    @Transactional
    @CachePut(value = "inventory", key = "#id")
    @CacheEvict(value = "availableEvents", allEntries = true)
    public InventoryResponse updateInventory(Long id, UpdateInventoryRequest request) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException(id));

        inventoryMapper.updateEntityFromRequest(inventory, request);
        Inventory saved = inventoryRepository.save(inventory);

        log.info("Inventory updated: id={}", saved.getId());
        return inventoryMapper.toResponse(saved);
    }

    /**
     * Reserves seats using Redisson distributed lock to prevent race conditions.
     * Lock ensures only one thread can modify availableSeats for a given inventory at a time,
     * even across multiple service instances.
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = {"inventory", "availableEvents"}, allEntries = true)
    public boolean reserveSeats(Long inventoryId, int quantity) {
        String lockKey = LOCK_KEY_PREFIX + inventoryId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    Inventory inventory = inventoryRepository.findById(inventoryId)
                            .orElseThrow(() -> new InventoryNotFoundException(inventoryId));

                    if (inventory.getAvailableSeats() < quantity) {
                        throw new InsufficientInventoryException(
                                inventory.getEventName(), quantity, inventory.getAvailableSeats());
                    }

                    inventory.setAvailableSeats(inventory.getAvailableSeats() - quantity);
                    inventoryRepository.save(inventory);

                    publishInventoryReservedEvent(inventory, quantity);

                    log.info("Reserved {} seats for event '{}', remaining: {}",
                            quantity, inventory.getEventName(), inventory.getAvailableSeats());
                    return true;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                log.warn("Could not acquire lock for inventory {}", inventoryId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted for inventory {}", inventoryId);
            return false;
        }
    }

    /**
     * Releases previously reserved seats, also protected by distributed lock.
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = {"inventory", "availableEvents"}, allEntries = true)
    public boolean releaseSeats(Long inventoryId, int quantity) {
        String lockKey = LOCK_KEY_PREFIX + inventoryId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS)) {
                try {
                    Inventory inventory = inventoryRepository.findById(inventoryId)
                            .orElseThrow(() -> new InventoryNotFoundException(inventoryId));

                    int newAvailable = inventory.getAvailableSeats() + quantity;
                    if (newAvailable > inventory.getTotalSeats()) {
                        log.warn("Cannot release {} seats for event '{}': would exceed total ({})",
                                quantity, inventory.getEventName(), inventory.getTotalSeats());
                        return false;
                    }

                    inventory.setAvailableSeats(newAvailable);
                    inventoryRepository.save(inventory);

                    publishInventoryReleasedEvent(inventory, quantity, "Manual release");

                    log.info("Released {} seats for event '{}', available: {}",
                            quantity, inventory.getEventName(), inventory.getAvailableSeats());
                    return true;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                log.warn("Could not acquire lock for inventory {}", inventoryId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted for inventory {}", inventoryId);
            return false;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"inventory", "availableEvents"}, allEntries = true)
    public void deleteInventory(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException(id));
        inventoryRepository.delete(inventory);
        log.info("Inventory deleted: id={}", id);
    }

    private void publishInventoryReservedEvent(Inventory inventory, int quantity) {
        InventoryReservedEvent event = new InventoryReservedEvent(
                inventory.getEventName(), quantity, null, String.valueOf(inventory.getId()));
        eventPublisher.publish("inventory-reserved", event);
    }

    private void publishInventoryReleasedEvent(Inventory inventory, int quantity, String reason) {
        InventoryReleasedEvent event = new InventoryReleasedEvent(
                inventory.getEventName(), quantity, null, String.valueOf(inventory.getId()), reason);
        eventPublisher.publish("inventory-released", event);
    }
}
