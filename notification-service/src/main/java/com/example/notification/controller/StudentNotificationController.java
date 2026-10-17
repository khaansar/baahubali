package com.example.notification.controller;

import com.example.notification.dto.InboxNotificationResponse;
import com.example.notification.entity.Notification;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.service.InAppNotificationStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

@RestController @RequestMapping("/notifications") @RequiredArgsConstructor
public class StudentNotificationController {

    private final NotificationRepository repository; private final InAppNotificationStream inAppNotificationStream;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader("X-User-Id") String userId) {
        return inAppNotificationStream.subscribe(userId);
    }

    @GetMapping
    public List<InboxNotificationResponse> list(@RequestHeader("X-User-Id") String userId) {
        return repository.findTop50ByUserIdAndChannelOrderByCreatedAtDesc(userId, Notification.Channel.IN_APP).stream().map(InboxNotificationResponse::from).toList();
    }

    @GetMapping("/unread-count")
    public long unreadCount(@RequestHeader("X-User-Id") String userId) {
        return repository.countByUserIdAndChannelAndReadAtIsNull(userId, Notification.Channel.IN_APP);
    }

    @PatchMapping("/{id}/read") @Transactional
    public ResponseEntity<Void> markRead(@PathVariable String id, @RequestHeader("X-User-Id") String userId) {
        return repository.findByIdAndUserIdAndChannel(id, userId, Notification.Channel.IN_APP).map(notification -> {
            if (notification.getReadAt() == null) { notification.setReadAt(Instant.now()); repository.save(notification); }
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}