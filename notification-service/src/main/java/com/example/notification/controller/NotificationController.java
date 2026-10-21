package com.example.notification.controller;

import com.example.notification.dto.ApiResponse;
import com.example.notification.dto.NotificationResponse;
import com.example.notification.entity.NotificationAttempt;
import com.example.notification.entity.Notification;
import com.example.notification.repository.NotificationAttemptRepository;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;

    @Value("${app.api.version}")
    private String apiVersion;

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @PathVariable String notificationId
    ) {
        return notificationRepository.findById(notificationId)
                .map(notification -> ResponseEntity.ok(ApiResponse.success(200,
                        "Notification retrieved successfully", NotificationResponse.from(notification), apiVersion)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.failure(404,
                        "Notification not found", "NOTIFICATION_NOT_FOUND", null, apiVersion)));
    }

    @GetMapping("/{notificationId}/attempts")
    public ResponseEntity<ApiResponse<List<NotificationAttempt>>> getAttempts(
            @PathVariable String notificationId
    ) {
        List<NotificationAttempt> attempts = notificationAttemptRepository
                .findByNotificationIdOrderByAttemptNumberAsc(notificationId);
        return ResponseEntity.ok(ApiResponse.success(200,
                "Notification delivery attempts retrieved successfully", attempts, apiVersion));
    }
}
