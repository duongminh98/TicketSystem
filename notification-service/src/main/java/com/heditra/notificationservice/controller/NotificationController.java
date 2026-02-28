package com.heditra.notificationservice.controller;

import com.heditra.notificationservice.dto.request.CreateNotificationRequest;
import com.heditra.notificationservice.dto.response.ApiResponse;
import com.heditra.notificationservice.dto.response.NotificationResponse;
import com.heditra.notificationservice.model.NotificationStatus;
import com.heditra.notificationservice.model.NotificationType;
import com.heditra.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Notification management APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Create a notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        NotificationResponse notification = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(notification, "Notification created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationById(id)));
    }

    @GetMapping
    @Operation(summary = "Get all notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getAllNotifications()));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get notifications by user ID")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByUserId(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsByUserId(userId)));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get notifications by status")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByStatus(
            @PathVariable NotificationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsByStatus(status)));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get notifications by type")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByType(
            @PathVariable NotificationType type) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsByType(type)));
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Send a pending notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.sendNotification(id), "Notification sent successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
