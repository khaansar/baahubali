package com.example.notification.consumer;

import com.example.notification.event.DomainEvent;
import com.example.notification.service.NotificationOrchestrator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttemptSubmittedEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationOrchestrator notificationOrchestrator;

    @KafkaListener(
            topics = "attempt-submitted-events",
            groupId = "notification-service-attempts",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void consume(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);

            String eventId = text(event, "eventId");
            String attemptId = text(event, "attemptId");
            String userId = text(event, "userId");
            String testId = text(event, "testId");
            String recipient = text(event, "recipient");
            String timestamp = text(event, "timestamp");

            if (recipient == null || recipient.isBlank()) {
                log.warn(
                        "Skipping attempt notification because event has no recipient eventId={}, userId={}, attemptId={}",
                        eventId,
                        userId,
                        attemptId
                );
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("attemptId", attemptId);
            payload.put("testId", testId);
            payload.put("channel", "EMAIL");

            notificationOrchestrator.process(
                    new DomainEvent(
                            eventId,
                            "ATTEMPT_SUBMITTED",
                            1,
                            timestamp == null ? Instant.now() : Instant.parse(timestamp),
                            "attempt-service",
                            userId,
                            null,
                            recipient,
                            payload
                    )
            );
        } catch (Exception e) {
            log.error("Failed to process attempt submitted event", e);
            throw new IllegalStateException(
                    "Attempt notification processing failed",
                    e
            );
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
