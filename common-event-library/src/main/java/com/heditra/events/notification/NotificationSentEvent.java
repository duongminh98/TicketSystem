package com.heditra.events.notification;

import com.heditra.events.core.DomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationSentEvent extends DomainEvent {

    private Long notificationId;
    private Long userId;
    private String notificationType;
    private String recipient;
    private String subject;
    private String message;
    private Boolean successful;

    public NotificationSentEvent(Long notificationId, Long userId, String notificationType,
                                 String recipient, String subject, String message, Boolean successful) {
        super("NotificationSent", String.valueOf(notificationId));
        this.notificationId = notificationId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
        this.successful = successful;
    }
}
