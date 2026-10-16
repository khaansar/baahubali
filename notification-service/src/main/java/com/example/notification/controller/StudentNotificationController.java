package com.example.notification.controller;

import com.example.notification.dto.InboxNotificationResponse;
import com.example.notification.entity.Notification;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class StudentNotificationController {

    private final NotificationRepository repository;

    @GetMapping
    public List<InboxNotificationResponse> list(
            @RequestHeader("X-User-Id") String userId
    ) {
        return repository
                .findTop50ByUserIdAndChannelOrderByCreatedAtDesc(
                        userId,
                        Notification.Channel.IN_APP
                )
                .stream()
                .map(InboxNotificationResponse::from)
                .toList();
    }

    @GetMapping("/unread-count")
    public long unreadCount(
            @RequestHeader("X-User-Id") String userId
    ) {
        return repository.countByUserIdAndChannelAndReadAtIsNull(
                userId,
                Notification.Channel.IN_APP
        );
    }

    @PatchMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> markRead(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        return repository.findByIdAndUserIdAndChannel(
                        id,
                        userId,
                        Notification.Channel.IN_APP
                )
                .map(notification -> {
                    if (notification.getReadAt() == null) {
                        notification.setReadAt(Instant.now());
                        repository.save(notification);
                    }
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}