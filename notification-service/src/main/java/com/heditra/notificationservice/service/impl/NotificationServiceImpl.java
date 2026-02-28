package com.heditra.notificationservice.service.impl;

import com.heditra.notificationservice.dto.request.CreateNotificationRequest;
import com.heditra.notificationservice.dto.response.NotificationResponse;
import com.heditra.notificationservice.exception.NotificationNotFoundException;
import com.heditra.notificationservice.mapper.NotificationMapper;
import com.heditra.notificationservice.model.Notification;
import com.heditra.notificationservice.model.NotificationStatus;
import com.heditra.notificationservice.model.NotificationType;
import com.heditra.notificationservice.repository.NotificationRepository;
import com.heditra.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        log.info("Creating notification for user {}", request.getUserId());
        Notification notification = NotificationMapper.toEntity(request);
        notification = notificationRepository.save(notification);
        log.info("Notification created: id={}", notification.getId());
        return NotificationMapper.toResponse(notification);
    }

    @Override
    public NotificationResponse getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .map(NotificationMapper::toResponse)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    @Override
    public List<NotificationResponse> getAllNotifications() {
        return NotificationMapper.toResponseList(notificationRepository.findAll());
    }

    @Override
    public List<NotificationResponse> getNotificationsByUserId(Long userId) {
        return NotificationMapper.toResponseList(notificationRepository.findByUserId(userId));
    }

    @Override
    public List<NotificationResponse> getNotificationsByStatus(NotificationStatus status) {
        return NotificationMapper.toResponseList(notificationRepository.findByStatus(status));
    }

    @Override
    public List<NotificationResponse> getNotificationsByType(NotificationType type) {
        return NotificationMapper.toResponseList(notificationRepository.findByType(type));
    }

    @Override
    @Transactional
    public NotificationResponse sendNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        boolean sent = executeNotificationSending(notification);

        if (sent) {
            notification.setStatus(NotificationStatus.SENT);
            log.info("Notification {} sent successfully to user {}", id, notification.getUserId());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("Notification {} failed to send to user {}", id, notification.getUserId());
        }

        notification = notificationRepository.save(notification);
        return NotificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public void deleteNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notificationRepository.delete(notification);
        log.info("Notification deleted: {}", id);
    }

    /**
     * Stub notification sending — always returns true.
     * Replace with real email/SMS/push integration in production.
     */
    private boolean executeNotificationSending(Notification notification) {
        log.info("Sending {} notification to user {}: {}",
                notification.getType(), notification.getUserId(), notification.getMessage());
        return true;
    }
}
