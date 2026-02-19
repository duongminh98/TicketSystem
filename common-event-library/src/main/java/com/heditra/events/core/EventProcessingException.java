package com.heditra.events.core;

/**
 * Exception khi xử lý event thất bại.
 * Được throw trong EventHandler, có thể trigger Dead Letter Queue.
 */
public class EventProcessingException extends RuntimeException {

    public EventProcessingException(String message) {
        super(message);
    }

    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
