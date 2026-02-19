package com.heditra.inventoryservice.exception;

public class InsufficientInventoryException extends BusinessException {

    public InsufficientInventoryException(String eventName, int requested, int available) {
        super("INSUFFICIENT_INVENTORY",
                String.format("Insufficient seats for event '%s': requested=%d, available=%d",
                        eventName, requested, available));
    }
}
