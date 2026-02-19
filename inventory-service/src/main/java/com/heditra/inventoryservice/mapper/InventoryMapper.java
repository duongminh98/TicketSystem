package com.heditra.inventoryservice.mapper;

import com.heditra.inventoryservice.dto.request.CreateInventoryRequest;
import com.heditra.inventoryservice.dto.request.UpdateInventoryRequest;
import com.heditra.inventoryservice.dto.response.InventoryResponse;
import com.heditra.inventoryservice.model.Inventory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryMapper {

    public Inventory toEntity(CreateInventoryRequest request) {
        return Inventory.builder()
                .eventName(request.getEventName())
                .eventDate(request.getEventDate())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .price(request.getPrice())
                .location(request.getLocation())
                .build();
    }

    public InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .eventName(inventory.getEventName())
                .eventDate(inventory.getEventDate())
                .totalSeats(inventory.getTotalSeats())
                .availableSeats(inventory.getAvailableSeats())
                .price(inventory.getPrice())
                .location(inventory.getLocation())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public List<InventoryResponse> toResponseList(List<Inventory> inventories) {
        return inventories.stream().map(this::toResponse).toList();
    }

    public void updateEntityFromRequest(Inventory inventory, UpdateInventoryRequest request) {
        if (request.getEventName() != null) {
            inventory.setEventName(request.getEventName());
        }
        if (request.getEventDate() != null) {
            inventory.setEventDate(request.getEventDate());
        }
        if (request.getTotalSeats() != null) {
            inventory.setTotalSeats(request.getTotalSeats());
        }
        if (request.getPrice() != null) {
            inventory.setPrice(request.getPrice());
        }
        if (request.getLocation() != null) {
            inventory.setLocation(request.getLocation());
        }
    }
}
