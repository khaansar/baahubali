package com.example.testservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "test-events";

    public void publishTestDeletedEvent(String testId) {
        String payload = "DELETED," + testId;
        kafkaTemplate.send(TOPIC, payload);
    }
}