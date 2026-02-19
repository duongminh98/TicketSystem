package com.heditra.inventoryservice.service;

import com.heditra.inventoryservice.dto.request.CreateInventoryRequest;
import com.heditra.inventoryservice.dto.request.UpdateInventoryRequest;
import com.heditra.inventoryservice.dto.response.InventoryResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryService {

    InventoryResponse createInventory(CreateInventoryRequest request);

    InventoryResponse getInventoryById(Long id);

    InventoryResponse getInventoryByEventName(String eventName);

    List<InventoryResponse> getAllInventory();

    List<InventoryResponse> getAvailableEvents();

    List<InventoryResponse> getEventsByDateRange(LocalDateTime start, LocalDateTime end);

    InventoryResponse updateInventory(Long id, UpdateInventoryRequest request);

    boolean reserveSeats(Long inventoryId, int quantity);

    boolean releaseSeats(Long inventoryId, int quantity);

    void deleteInventory(Long id);
}
