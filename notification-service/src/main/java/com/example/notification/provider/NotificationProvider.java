package com.example.notification.provider;

import com.example.notification.entity.Notification;

public interface NotificationProvider {

    Notification.Channel getChannel();

    DeliveryResult send(
            Notification notification,
            String subject,
            String body
    );

    record DeliveryResult(
            boolean successful,
            String provider,
            String providerMessageId,
            String errorCode,
            String errorMessage
    ) {
        public static DeliveryResult success(
                String provider,
                String providerMessageId
        ) {
            return new DeliveryResult(
                    true,
                    provider,
                    providerMessageId,
                    null,
                    null
            );
        }

        public static DeliveryResult failure(
                String provider,
                String errorCode,
                String errorMessage
        ) {
            return new DeliveryResult(
                    false,
                    provider,
                    null,
                    errorCode,
                    errorMessage
            );
        }
    }
}
