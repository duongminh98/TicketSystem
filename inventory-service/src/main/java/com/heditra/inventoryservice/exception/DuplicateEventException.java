package com.heditra.inventoryservice.exception;

public class DuplicateEventException extends BusinessException {

    public DuplicateEventException(String eventName) {
        super("DUPLICATE_EVENT", "Event already exists: " + eventName);
    }
}
