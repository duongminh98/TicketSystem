package com.heditra.events.inventory;

import com.heditra.events.core.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryReservedEvent extends DomainEvent {

    private String eventName;
    private Integer quantity;
    private Long ticketId;
    private String reservationId;

    public InventoryReservedEvent(String eventName, Integer quantity,
                                  Long ticketId, String reservationId) {
        super("InventoryReserved", reservationId);
        this.eventName = eventName;
        this.quantity = quantity;
        this.ticketId = ticketId;
        this.reservationId = reservationId;
    }
}
