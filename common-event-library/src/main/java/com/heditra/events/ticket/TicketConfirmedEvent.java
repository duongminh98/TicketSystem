package com.heditra.events.ticket;

import com.heditra.events.core.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketConfirmedEvent extends DomainEvent {

    private Long ticketId;
    private Long userId;
    private String eventName;

    public TicketConfirmedEvent(Long ticketId, Long userId, String eventName) {
        super("TicketConfirmed", String.valueOf(ticketId));
        this.ticketId = ticketId;
        this.userId = userId;
        this.eventName = eventName;
    }
}
