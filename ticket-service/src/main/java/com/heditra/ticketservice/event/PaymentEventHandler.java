package com.heditra.ticketservice.event;

import com.heditra.events.payment.PaymentCompletedEvent;
import com.heditra.events.payment.PaymentFailedEvent;
import com.heditra.ticketservice.client.InventoryServiceClient;
import com.heditra.ticketservice.exception.TicketNotFoundException;
import com.heditra.ticketservice.model.Ticket;
import com.heditra.ticketservice.model.TicketStatus;
import com.heditra.ticketservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class PaymentEventHandler {

    private final TicketRepository ticketRepository;
    private final InventoryServiceClient inventoryClient;

    @KafkaListener(topics = "payment-events", groupId = "ticket-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentEvent(Object event) {
        if (event instanceof PaymentCompletedEvent completed) {
            handlePaymentCompleted(completed);
        } else if (event instanceof PaymentFailedEvent failed) {
            handlePaymentFailed(failed);
        } else {
            log.debug("Ignoring unhandled payment event type: {}", event.getClass().getSimpleName());
        }
    }

    @Transactional
    protected void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Payment completed for ticket {}, transactionId: {}",
                event.getTicketId(), event.getTransactionId());

        Ticket ticket = ticketRepository.findById(event.getTicketId())
                .orElseThrow(() -> new TicketNotFoundException(event.getTicketId()));

        if (ticket.getStatus() != TicketStatus.PENDING) {
            log.warn("Ticket {} is not PENDING (status={}), skipping confirmation",
                    ticket.getId(), ticket.getStatus());
            return;
        }

        ticket.setStatus(TicketStatus.CONFIRMED);
        ticketRepository.save(ticket);
        log.info("Ticket {} confirmed after payment", ticket.getId());
    }

    @Transactional
    protected void handlePaymentFailed(PaymentFailedEvent event) {
        log.warn("Payment failed for ticket {}, reason: {}",
                event.getTicketId(), event.getFailureReason());

        Ticket ticket = ticketRepository.findById(event.getTicketId())
                .orElseThrow(() -> new TicketNotFoundException(event.getTicketId()));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            log.warn("Ticket {} already cancelled, skipping", ticket.getId());
            return;
        }

        if (ticket.getInventoryId() != null) {
            inventoryClient.releaseSeats(ticket.getInventoryId(), ticket.getQuantity());
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
        log.info("Ticket {} cancelled after payment failure", ticket.getId());
    }
}
