package com.example.testservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "test-events";

    public void publishTestDeletedEvent(String testId) {
        com.example.testservice.event.DomainEvent event = new com.example.testservice.event.DomainEvent(
                java.util.UUID.randomUUID().toString(),
                "TEST_DELETED",
                1,
                java.time.Instant.now(),
                "TEST",
                testId
        );
        kafkaTemplate.send(TOPIC, testId, event);
    }
}