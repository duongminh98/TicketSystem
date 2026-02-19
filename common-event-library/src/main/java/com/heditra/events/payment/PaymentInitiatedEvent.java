package com.heditra.events.payment;

import com.heditra.events.core.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentInitiatedEvent extends DomainEvent {

    private Long paymentId;
    private Long ticketId;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;

    public PaymentInitiatedEvent(Long paymentId, Long ticketId, Long userId,
                                 BigDecimal amount, String paymentMethod, String transactionId) {
        super("PaymentInitiated", String.valueOf(paymentId));
        this.paymentId = paymentId;
        this.ticketId = ticketId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
    }
}
