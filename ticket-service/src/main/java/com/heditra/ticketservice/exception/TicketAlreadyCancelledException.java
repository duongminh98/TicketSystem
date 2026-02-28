package com.heditra.ticketservice.exception;

public class TicketAlreadyCancelledException extends BusinessException {

    public TicketAlreadyCancelledException(Long id) {
        super("TICKET_ALREADY_CANCELLED", "Ticket already cancelled with id: " + id);
    }
}
