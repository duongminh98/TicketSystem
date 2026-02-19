package com.heditra.inventoryservice.exception;

public class InventoryNotFoundException extends BusinessException {

    public InventoryNotFoundException(Long id) {
        super("INVENTORY_NOT_FOUND", "Inventory not found with id: " + id);
    }

    public InventoryNotFoundException(String eventName) {
        super("INVENTORY_NOT_FOUND", "Inventory not found for event: " + eventName);
    }
}
