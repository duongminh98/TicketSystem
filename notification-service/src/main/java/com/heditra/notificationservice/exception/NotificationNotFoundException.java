package com.heditra.notificationservice.exception;

public class NotificationNotFoundException extends BusinessException {

    public NotificationNotFoundException(Long id) {
        super("NOTIFICATION_NOT_FOUND", "Notification not found with id: " + id);
    }
}
