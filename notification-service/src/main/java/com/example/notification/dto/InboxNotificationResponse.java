package com.example.notification.dto;

import com.example.notification.entity.Notification;

import java.time.Instant;

public record InboxNotificationResponse(
        String id,
        String eventType,
        String title,
        String message,
        String actionUrl,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
    public static InboxNotificationResponse from(Notification n) {
        return new InboxNotificationResponse(
                n.getId(),
                n.getEventType(),
                n.getTitle(),
                n.getMessage(),
                n.getActionUrl(),
                n.getReadAt() != null,
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}