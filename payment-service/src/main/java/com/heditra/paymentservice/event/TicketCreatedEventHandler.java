package com.heditra.paymentservice.event;

import com.heditra.events.ticket.TicketCreatedEvent;
import com.heditra.paymentservice.mapper.PaymentMapper;
import com.heditra.paymentservice.model.Payment;
import com.heditra.paymentservice.model.PaymentMethod;
import com.heditra.paymentservice.model.PaymentStatus;
import com.heditra.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class TicketCreatedEventHandler {

    private final PaymentRepository paymentRepository;

    @KafkaListener(topics = "ticket-events", groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleTicketEvent(Object event) {
        if (event instanceof TicketCreatedEvent ticketCreated) {
            handleTicketCreated(ticketCreated);
        } else {
            log.debug("Ignoring non-TicketCreatedEvent: {}", event.getClass().getSimpleName());
        }
    }

    @Transactional
    protected void handleTicketCreated(TicketCreatedEvent event) {
        log.info("Received TicketCreatedEvent: ticketId={}, userId={}, amount={}",
                event.getTicketId(), event.getUserId(), event.getTotalAmount());

        if (event.getTicketId() == null || event.getUserId() == null || event.getTotalAmount() == null) {
            log.warn("Invalid TicketCreatedEvent: missing required fields");
            return;
        }

        if (paymentRepository.findByTicketId(event.getTicketId()).isPresent()) {
            log.warn("Payment already exists for ticket {}, skipping", event.getTicketId());
            return;
        }

        Payment payment = Payment.builder()
                .ticketId(event.getTicketId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .transactionId(UUID.randomUUID().toString())
                .build();

        paymentRepository.save(payment);
        log.info("Payment record auto-created for ticket {}: paymentId={}", event.getTicketId(), payment.getId());
    }
}
