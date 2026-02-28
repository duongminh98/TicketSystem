package com.heditra.notificationservice.mapper;

import com.heditra.notificationservice.dto.request.CreateNotificationRequest;
import com.heditra.notificationservice.dto.response.NotificationResponse;
import com.heditra.notificationservice.model.Notification;
import com.heditra.notificationservice.model.NotificationStatus;
import com.heditra.notificationservice.model.NotificationType;

import java.util.List;

public final class NotificationMapper {

    private NotificationMapper() {}

    public static Notification toEntity(CreateNotificationRequest request) {
        NotificationType type = NotificationType.EMAIL;
        if (request.getType() != null) {
            try {
                type = NotificationType.valueOf(request.getType().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Notification.builder()
                .userId(request.getUserId())
                .message(request.getMessage())
                .type(type)
                .status(NotificationStatus.PENDING)
                .build();
    }

    public static NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .status(notification.getStatus().name())
                .sentAt(notification.getSentAt())
                .build();
    }

    public static List<NotificationResponse> toResponseList(List<Notification> notifications) {
        return notifications.stream().map(NotificationMapper::toResponse).toList();
    }
}
