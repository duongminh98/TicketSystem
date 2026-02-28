package com.heditra.notificationservice.event;

import com.heditra.events.payment.PaymentCompletedEvent;
import com.heditra.events.ticket.TicketCreatedEvent;
import com.heditra.events.user.UserCreatedEvent;
import com.heditra.notificationservice.model.Notification;
import com.heditra.notificationservice.model.NotificationStatus;
import com.heditra.notificationservice.model.NotificationType;
import com.heditra.notificationservice.repository.NotificationRepository;
import com.heditra.notificationservice.service.EmailService;
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
public class NotificationEventHandler {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @KafkaListener(topics = "user-created", groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent: userId={}", event.getUserId());

        if (event.getUserId() == null) {
            log.warn("Invalid UserCreatedEvent: missing userId");
            return;
        }

        Notification notification = createAndSend(
                event.getUserId(),
                "Welcome! Your account has been created successfully.",
                NotificationType.EMAIL);

        log.info("Welcome notification sent to user {}: notificationId={}", event.getUserId(), notification.getId());
    }

    @KafkaListener(topics = "ticket-events", groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleTicketEvent(Object event) {
        if (event instanceof TicketCreatedEvent ticketCreated) {
            handleTicketCreated(ticketCreated);
        } else {
            log.debug("Ignoring non-TicketCreatedEvent from ticket-events: {}", event.getClass().getSimpleName());
        }
    }

    private void handleTicketCreated(TicketCreatedEvent event) {
        log.info("Received TicketCreatedEvent: ticketId={}, userId={}", event.getTicketId(), event.getUserId());

        if (event.getUserId() == null || event.getTicketId() == null) {
            log.warn("Invalid TicketCreatedEvent: missing required fields");
            return;
        }

        Notification notification = createAndSend(
                event.getUserId(),
                "Your ticket has been booked successfully. Ticket ID: " + event.getTicketId(),
                NotificationType.EMAIL);

        log.info("Ticket booking notification sent to user {}: notificationId={}",
                event.getUserId(), notification.getId());
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handlePaymentEvent(Object event) {
        if (event instanceof PaymentCompletedEvent paymentCompleted) {
            handlePaymentCompleted(paymentCompleted);
        } else {
            log.debug("Ignoring non-PaymentCompletedEvent from payment-events: {}", event.getClass().getSimpleName());
        }
    }

    private void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: paymentId={}, userId={}", event.getPaymentId(), event.getUserId());

        if (event.getUserId() == null || event.getPaymentId() == null) {
            log.warn("Invalid PaymentCompletedEvent: missing required fields");
            return;
        }

        Notification notification = createAndSend(
                event.getUserId(),
                "Payment processed successfully. Thank you! Transaction: " + event.getTransactionId(),
                NotificationType.EMAIL);

        log.info("Payment notification sent to user {}: notificationId={}",
                event.getUserId(), notification.getId());
    }

    private Notification createAndSend(Long userId, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .message(message)
                .type(type)
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        boolean sent = executeNotificationSending(notification);
        notification.setStatus(sent ? NotificationStatus.SENT : NotificationStatus.FAILED);
        return notificationRepository.save(notification);
    }

    private boolean executeNotificationSending(Notification notification) {
        log.info("Sending {} notification to user {}: {}",
                notification.getType(), notification.getUserId(), notification.getMessage());
        return emailService.sendNotification(notification);
    }
}
