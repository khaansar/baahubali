package com.example.notification.dto;

import com.example.notification.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String eventId,
        String userId,
        String eventType,
        Notification.Channel channel,
        String recipient,
        Notification.Status status,
        int attemptCount,
        Instant createdAt,
        Instant sentAt,
        String lastError
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getUserId(),
                notification.getEventType(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getStatus(),
                notification.getAttemptCount(),
                notification.getCreatedAt(),
                notification.getSentAt(),
                notification.getLastError()
        );
    }
}
