package com.example.notification.dto;

public record EmailNotificationRequest(
        String recipient,
        String subject,
        String body
) {
}