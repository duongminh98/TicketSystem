package com.heditra.notificationservice.service;

import com.heditra.notificationservice.dto.request.CreateNotificationRequest;
import com.heditra.notificationservice.dto.response.NotificationResponse;
import com.heditra.notificationservice.model.NotificationStatus;
import com.heditra.notificationservice.model.NotificationType;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(CreateNotificationRequest request);

    NotificationResponse getNotificationById(Long id);

    List<NotificationResponse> getAllNotifications();

    List<NotificationResponse> getNotificationsByUserId(Long userId);

    List<NotificationResponse> getNotificationsByStatus(NotificationStatus status);

    List<NotificationResponse> getNotificationsByType(NotificationType type);

    NotificationResponse sendNotification(Long id);

    void deleteNotification(Long id);
}
