package com.example.testservice.service;

import com.example.common.audit.AuditEvent;
import com.example.testservice.dto.event.TestPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_TEST_PUBLISHED = "test.published";
    private static final String TOPIC_AUDIT_EVENTS = "audit.events";

    public void emitTestPublished(TestPublishedEvent event) {
        // Use testId as the Kafka partition key to guarantee ordering of events for the same test
        kafkaTemplate.send(TOPIC_TEST_PUBLISHED, event.testId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to emit test.published event for Test ID {}", event.testId(), ex);
                    } else {
                        log.info("Successfully emitted test.published event for Test ID {}", event.testId());
                    }
                });
    }

    public void emitAuditEvent(AuditEvent event) {
        String key = event.resourceId() != null ? event.resourceId() : event.eventId().toString();

        kafkaTemplate.send(TOPIC_AUDIT_EVENTS, key, event).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to emit audit event {} for action {}", event.eventId(),
                                event.action(), ex
                        );
                    }
                });
    }
}