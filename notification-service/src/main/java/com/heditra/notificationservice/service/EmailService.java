package com.heditra.notificationservice.service;

import com.heditra.notificationservice.client.UserClient;
import com.heditra.notificationservice.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserClient userClient;

    public boolean sendNotification(Notification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            String recipient = userClient.getUserEmail(notification.getUserId());
            if (recipient == null || recipient.isBlank()) {
                log.warn("No email found for user {}, skipping email for notification {}",
                        notification.getUserId(), notification.getId());
                return false;
            }

            message.setTo(recipient);
            message.setSubject("Ticketing Notification");
            message.setText(notification.getMessage());

            mailSender.send(message);
            log.info("Email sent to {} for notification {}", recipient, notification.getId());
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email for notification {}: {}", notification.getId(), ex.getMessage());
            return false;
        }
    }
}

