package com.heditra.events.payment;

import com.heditra.events.core.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentCompletedEvent extends DomainEvent {

    private Long paymentId;
    private Long ticketId;
    private Long userId;
    private BigDecimal amount;
    private String transactionId;

    public PaymentCompletedEvent(Long paymentId, Long ticketId, Long userId,
                                 BigDecimal amount, String transactionId) {
        super("PaymentCompleted", String.valueOf(paymentId));
        this.paymentId = paymentId;
        this.ticketId = ticketId;
        this.userId = userId;
        this.amount = amount;
        this.transactionId = transactionId;
    }
}
