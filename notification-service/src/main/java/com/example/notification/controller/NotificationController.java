package com.example.notification.controller;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.entity.Notification;
import com.example.notification.repository.NotificationAttemptRepository;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable String notificationId
    ) {
        return notificationRepository.findById(notificationId)
                .map(notification -> ResponseEntity.ok(
                        NotificationResponse.from(notification)
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{notificationId}/attempts")
    public ResponseEntity<List<?>> getAttempts(
            @PathVariable String notificationId
    ) {
        return ResponseEntity.ok(
                notificationAttemptRepository
                        .findByNotificationIdOrderByAttemptNumberAsc(notificationId)
        );
    }
}
