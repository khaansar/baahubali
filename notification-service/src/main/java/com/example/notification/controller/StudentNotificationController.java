package com.example.notification.controller;

import com.example.notification.dto.ApiResponse;
import com.example.notification.dto.InboxNotificationResponse;
import com.example.notification.entity.Notification;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.service.InAppNotificationStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/notifications") @RequiredArgsConstructor
public class StudentNotificationController {

    private final NotificationRepository repository;
    private final InAppNotificationStream inAppNotificationStream;

    @Value("${app.api.version}")
    private String apiVersion;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader("X-User-Id") String userId) {
        return inAppNotificationStream.subscribe(userId);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InboxNotificationResponse>>> list(
            @RequestHeader("X-User-Id") String userId) {
        List<InboxNotificationResponse> notifications = repository
                .findTop50ByUserIdAndChannelOrderByCreatedAtDesc(userId, Notification.Channel.IN_APP)
                .stream()
                .map(InboxNotificationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(200,
                "Notifications retrieved successfully", notifications, apiVersion));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@RequestHeader("X-User-Id") String userId) {
        long count = repository.countByUserIdAndChannelAndReadAtIsNull(userId, Notification.Channel.IN_APP);
        return ResponseEntity.ok(ApiResponse.success(200,
                "Unread notification count retrieved successfully", count, apiVersion));
    }

    @PatchMapping("/{id}/read") @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> markRead(
            @PathVariable String id, @RequestHeader("X-User-Id") String userId) {
        return repository.findByIdAndUserIdAndChannel(id, userId, Notification.Channel.IN_APP)
                .map(notification -> {
                    if (notification.getReadAt() == null) {
                        notification.setReadAt(Instant.now());
                        repository.save(notification);
                    }
                    return ResponseEntity.ok(ApiResponse.success(200,
                            "Notification marked as read", Map.<String, Object>of(), apiVersion));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.failure(404,
                        "Notification not found", "NOTIFICATION_NOT_FOUND", null, apiVersion)));
    }
}
