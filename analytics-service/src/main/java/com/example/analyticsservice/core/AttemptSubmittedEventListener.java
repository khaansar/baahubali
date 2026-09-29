package com.example.analyticsservice.core;

import com.example.analyticsservice.contract.AnalyticsKafkaTopics;
import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Thin Kafka entry point. Receives the raw JSON and maps it to the analytics contract with the
 * application JsonMapper, so no consumer (de)serializer configuration is needed. Missing
 * contract fields are never defaulted.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttemptSubmittedEventListener {

    private final AnalyticsOrchestrator orchestrator;
    private final JsonMapper jsonMapper;

    @KafkaListener(
            topics = AnalyticsKafkaTopics.ATTEMPT_SUBMITTED,
            groupId = "${spring.application.name}",
            properties = "auto.offset.reset=earliest")
    public void onMessage(String payload) {
        AttemptSubmittedEvent event;
        try {
            event = jsonMapper.readValue(payload, AttemptSubmittedEvent.class);
        } catch (RuntimeException ex) {
            // Undeserializable message can never succeed on retry; skip it.
            log.error("Skipping unreadable attempt-submitted message: {}", ex.getClass().getSimpleName());
            return;
        }

        log.info("Received attempt submitted event, attemptId={}", event.attemptId());
        try {
            orchestrator.process(event);
        } catch (InvalidAttemptEventException ex) {
            // Contract violation (e.g. legacy producer event). Retrying cannot fix it; skip.
            log.error("Skipping invalid event, attemptId={}: {}", event.attemptId(), ex.getMessage());
        }
        // Any other exception propagates so Kafka's error handler retries the record.
    }
}