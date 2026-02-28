package com.heditra.ticketservice.exception;

public class InvalidTicketStatusException extends BusinessException {

    public InvalidTicketStatusException(String message) {
        super("INVALID_TICKET_STATUS", message);
    }
}
