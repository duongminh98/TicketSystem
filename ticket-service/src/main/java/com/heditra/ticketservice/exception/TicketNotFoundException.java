package com.heditra.ticketservice.exception;

public class TicketNotFoundException extends BusinessException {

    public TicketNotFoundException(Long id) {
        super("TICKET_NOT_FOUND", "Ticket not found with id: " + id);
    }
}
