package com.heditra.events.inventory;

import com.heditra.events.core.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryReleasedEvent extends DomainEvent {

    private String eventName;
    private Integer quantity;
    private Long ticketId;
    private String reservationId;
    private String releaseReason;

    public InventoryReleasedEvent(String eventName, Integer quantity,
                                  Long ticketId, String reservationId, String releaseReason) {
        super("InventoryReleased", reservationId);
        this.eventName = eventName;
        this.quantity = quantity;
        this.ticketId = ticketId;
        this.reservationId = reservationId;
        this.releaseReason = releaseReason;
    }
}
