package com.heditra.inventoryservice.controller;

import com.heditra.inventoryservice.dto.request.CreateInventoryRequest;
import com.heditra.inventoryservice.dto.request.UpdateInventoryRequest;
import com.heditra.inventoryservice.dto.response.ApiResponse;
import com.heditra.inventoryservice.dto.response.InventoryResponse;
import com.heditra.inventoryservice.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Event inventory management APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @Operation(summary = "Create a new event inventory")
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {
        InventoryResponse inventory = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventory, "Inventory created successfully"));
    }

    @GetMapping("/event/{eventName}")
    @Operation(summary = "Get inventory by event name")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByEventName(
            @PathVariable String eventName) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventoryByEventName(eventName)));
    }

    @GetMapping
    @Operation(summary = "Get all inventory")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventory() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllInventory()));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available events (seats > 0, future date)")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAvailableEvents() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAvailableEvents()));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get events by date range")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getEventsByDateRange(start, end)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inventory by ID")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventoryById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inventory")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable Long id, @Valid @RequestBody UpdateInventoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.updateInventory(id, request), "Inventory updated successfully"));
    }

    @PostMapping("/{id}/reserve")
    @Operation(summary = "Reserve seats (distributed lock)")
    public ResponseEntity<ApiResponse<Boolean>> reserveSeats(
            @PathVariable Long id, @RequestParam int quantity) {
        boolean result = inventoryService.reserveSeats(id, quantity);
        String message = result ? "Seats reserved successfully" : "Failed to reserve seats";
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release reserved seats (distributed lock)")
    public ResponseEntity<ApiResponse<Boolean>> releaseSeats(
            @PathVariable Long id, @RequestParam int quantity) {
        boolean result = inventoryService.releaseSeats(id, quantity);
        String message = result ? "Seats released successfully" : "Failed to release seats";
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete inventory")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }
}

