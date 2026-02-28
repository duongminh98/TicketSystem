package com.heditra.ticketservice.service.impl;

import com.heditra.events.core.EventPublisher;
import com.heditra.events.ticket.TicketCancelledEvent;
import com.heditra.events.ticket.TicketConfirmedEvent;
import com.heditra.events.ticket.TicketCreatedEvent;
import com.heditra.ticketservice.client.InventoryServiceClient;
import com.heditra.ticketservice.dto.request.CreateTicketRequest;
import com.heditra.ticketservice.dto.request.UpdateTicketRequest;
import com.heditra.ticketservice.dto.response.InventoryCheckResponse;
import com.heditra.ticketservice.dto.response.TicketResponse;
import com.heditra.ticketservice.exception.InvalidTicketStatusException;
import com.heditra.ticketservice.exception.TicketAlreadyCancelledException;
import com.heditra.ticketservice.exception.TicketNotFoundException;
import com.heditra.ticketservice.exception.BusinessException;
import com.heditra.ticketservice.document.TicketDocument;
import com.heditra.ticketservice.mapper.TicketMapper;
import com.heditra.ticketservice.service.TicketService;
import com.heditra.ticketservice.model.Ticket;
import com.heditra.ticketservice.model.TicketStatus;
import com.heditra.ticketservice.repository.TicketRepository;
import com.heditra.ticketservice.repository.TicketSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketSearchRepository ticketSearchRepository;
    private final EventPublisher eventPublisher;
    private final InventoryServiceClient inventoryClient;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        log.info("Creating ticket for user {} event {}", request.getUserId(), request.getEventName());

        InventoryCheckResponse inventory = inventoryClient.checkAvailability(request.getEventName());
        if (!inventory.isAvailable()) {
            throw new BusinessException("NO_AVAILABILITY",
                    "No seats available for event: " + request.getEventName());
        }
        if (inventory.getAvailableSeats() != null && inventory.getAvailableSeats() < request.getQuantity()) {
            throw new BusinessException("INSUFFICIENT_SEATS",
                    "Only " + inventory.getAvailableSeats() + " seats available, requested " + request.getQuantity());
        }

        boolean reserved = inventoryClient.reserveSeats(inventory.getId(), request.getQuantity());
        if (!reserved) {
            throw new BusinessException("RESERVATION_FAILED",
                    "Failed to reserve seats for event: " + request.getEventName());
        }

        Ticket ticket = TicketMapper.toEntity(request);
        ticket.setInventoryId(inventory.getId());
        ticket = ticketRepository.save(ticket);

        indexTicketToElasticsearch(ticket);
        publishTicketCreatedEvent(ticket);

        log.info("Ticket created with id: {}", ticket.getId());
        return TicketMapper.toResponse(ticket);
    }

    @Override
    @Cacheable(value = "tickets", key = "#id")
    public TicketResponse getTicketById(Long id) {
        Optional<TicketDocument> doc = ticketSearchRepository.findById(String.valueOf(id));
        if (doc.isPresent()) {
            return TicketMapper.toResponse(doc.get());
        }
        return ticketRepository.findById(id)
                .map(TicketMapper::toResponse)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Override
    public List<TicketResponse> getAllTickets() {
        Pageable page = PageRequest.of(0, 10_000);
        return ticketSearchRepository.findAll(page).getContent().stream()
                .map(TicketMapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> getTicketsByUserId(Long userId) {
        return ticketSearchRepository.findByUserId(userId).stream()
                .map(TicketMapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> getTicketsByStatus(TicketStatus status) {
        return ticketSearchRepository.findByStatus(status.name()).stream()
                .map(TicketMapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> getTicketsByEventName(String eventName) {
        return ticketSearchRepository.findByEventName(eventName).stream()
                .map(TicketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CachePut(value = "tickets", key = "#id")
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new TicketAlreadyCancelledException(id);
        }
        if (ticket.getStatus() == TicketStatus.CONFIRMED) {
            throw new InvalidTicketStatusException("Cannot update a confirmed ticket");
        }

        if (request.getEventName() != null) {
            ticket.setEventName(request.getEventName());
        }
        if (request.getQuantity() != null) {
            ticket.setQuantity(request.getQuantity());
            ticket.setTotalAmount(ticket.getPricePerTicket()
                    .multiply(BigDecimal.valueOf(request.getQuantity())));
        }
        if (request.getPricePerTicket() != null) {
            ticket.setPricePerTicket(request.getPricePerTicket());
            ticket.setTotalAmount(request.getPricePerTicket()
                    .multiply(BigDecimal.valueOf(ticket.getQuantity())));
        }
        if (request.getEventDate() != null) {
            ticket.setEventDate(request.getEventDate());
        }

        ticket = ticketRepository.save(ticket);
        indexTicketToElasticsearch(ticket);
        log.info("Ticket updated: {}", id);
        return TicketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tickets", key = "#id")
    public TicketResponse cancelTicket(Long id, String reason) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new TicketAlreadyCancelledException(id);
        }

        if (ticket.getInventoryId() != null) {
            inventoryClient.releaseSeats(ticket.getInventoryId(), ticket.getQuantity());
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket = ticketRepository.save(ticket);

        indexTicketToElasticsearch(ticket);
        publishTicketCancelledEvent(ticket, reason);

        log.info("Ticket cancelled: {}", id);
        return TicketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    @CachePut(value = "tickets", key = "#id")
    public TicketResponse confirmTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new InvalidTicketStatusException(
                    "Ticket must be PENDING to confirm. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.CONFIRMED);
        ticket = ticketRepository.save(ticket);

        indexTicketToElasticsearch(ticket);
        publishTicketConfirmedEvent(ticket);

        log.info("Ticket confirmed: {}", id);
        return TicketMapper.toResponse(ticket);
    }

    @Override
    @Transactional
    @CacheEvict(value = "tickets", key = "#id")
    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticketRepository.delete(ticket);
        try {
            ticketSearchRepository.deleteById(String.valueOf(id));
        } catch (Exception e) {
            log.warn("Failed to delete ticket from Elasticsearch: {}", e.getMessage());
        }
        log.info("Ticket deleted: {}", id);
    }

    @Override
    public List<TicketResponse> searchTickets(String keyword) {
        return ticketSearchRepository.findByEventNameContaining(keyword).stream()
                .map(TicketMapper::toResponse)
                .toList();
    }

    private void indexTicketToElasticsearch(Ticket ticket) {
        try {
            TicketDocument doc = TicketDocument.builder()
                    .id(String.valueOf(ticket.getId()))
                    .userId(ticket.getUserId())
                    .eventName(ticket.getEventName())
                    .quantity(ticket.getQuantity())
                    .pricePerTicket(ticket.getPricePerTicket() != null ? ticket.getPricePerTicket().doubleValue() : null)
                    .totalAmount(ticket.getTotalAmount() != null ? ticket.getTotalAmount().doubleValue() : null)
                    .status(ticket.getStatus().name())
                    .eventDate(ticket.getEventDate())
                    .bookingDate(ticket.getBookingDate())
                    .createdAt(ticket.getCreatedAt())
                    .updatedAt(ticket.getUpdatedAt())
                    .build();
            ticketSearchRepository.save(doc);
        } catch (Exception e) {
            log.warn("Failed to index ticket to Elasticsearch: {}", e.getMessage());
        }
    }

    private void publishTicketCreatedEvent(Ticket ticket) {
        try {
            TicketCreatedEvent event = new TicketCreatedEvent(
                    ticket.getId(), ticket.getUserId(), ticket.getEventName(),
                    ticket.getQuantity(), ticket.getPricePerTicket(), ticket.getTotalAmount());
            eventPublisher.publish("ticket-events", event);
        } catch (Exception e) {
            log.error("Failed to publish TicketCreatedEvent for ticket {}: {}",
                    ticket.getId(), e.getMessage());
        }
    }

    private void publishTicketCancelledEvent(Ticket ticket, String reason) {
        try {
            TicketCancelledEvent event = new TicketCancelledEvent(
                    ticket.getId(), ticket.getUserId(), ticket.getEventName(),
                    ticket.getQuantity(), reason);
            eventPublisher.publish("ticket-events", event);
        } catch (Exception e) {
            log.error("Failed to publish TicketCancelledEvent for ticket {}: {}",
                    ticket.getId(), e.getMessage());
        }
    }

    private void publishTicketConfirmedEvent(Ticket ticket) {
        try {
            TicketConfirmedEvent event = new TicketConfirmedEvent(
                    ticket.getId(), ticket.getUserId(), ticket.getEventName());
            eventPublisher.publish("ticket-events", event);
        } catch (Exception e) {
            log.error("Failed to publish TicketConfirmedEvent for ticket {}: {}",
                    ticket.getId(), e.getMessage());
        }
    }
}
