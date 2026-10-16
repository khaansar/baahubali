package com.example.notification.provider;

import com.example.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationProvider implements NotificationProvider {

    @Override
    public Notification.Channel getChannel() {
        return Notification.Channel.IN_APP;
    }

    @Override
    public DeliveryResult send(
            Notification notification,
            String subject,
            String body
    ) {
        if (notification.getUserId() == null
                || notification.getUserId().isBlank()) {
            return DeliveryResult.failure(
                    "IN_APP",
                    "MISSING_USER_ID",
                    "An in-app notification requires a user ID"
            );
        }

        return DeliveryResult.success(
                "IN_APP",
                notification.getId()
        );
    }
}