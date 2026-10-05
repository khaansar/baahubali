package com.example.notification.consumer;

import com.example.notification.event.DomainEvent;
import com.example.notification.service.NotificationOrchestrator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationOrchestrator notificationOrchestrator;

    @KafkaListener(
            topics = {
                    "iam-events",
                    "payment-events",
                    "test-events",
                    "platform-events"
            },
            groupId = "notification-service",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        try {
            DomainEvent event = objectMapper.copy()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(message, DomainEvent.class);

            notificationOrchestrator.process(event);
        } catch (Exception e) {
            log.error("Failed to process notification domain event", e);
            throw new IllegalStateException(
                    "Notification event processing failed",
                    e
            );
        }
    }
}
