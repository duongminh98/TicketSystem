package com.heditra.events.payment;

import com.heditra.events.core.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentFailedEvent extends DomainEvent {

    private Long paymentId;
    private Long ticketId;
    private Long userId;
    private BigDecimal amount;
    private String transactionId;
    private String failureReason;

    public PaymentFailedEvent(Long paymentId, Long ticketId, Long userId,
                              BigDecimal amount, String transactionId, String failureReason) {
        super("PaymentFailed", String.valueOf(paymentId));
        this.paymentId = paymentId;
        this.ticketId = ticketId;
        this.userId = userId;
        this.amount = amount;
        this.transactionId = transactionId;
        this.failureReason = failureReason;
    }
}
