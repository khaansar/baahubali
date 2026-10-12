package com.example.attemptservice.worker;

import com.example.attemptservice.entity.OutboxEvent;
import com.example.attemptservice.event.AttemptStartedEvent;
import com.example.attemptservice.event.AttemptSubmittedEvent;
import com.example.attemptservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final String ATTEMPT_SUBMITTED_TOPIC = "attempt-submitted-events";
    private static final String ATTEMPT_STARTED_TOPIC = "attempt-started-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishEvents() {
        for (OutboxEvent outboxEvent : outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                if ("ATTEMPT_STARTED".equals(outboxEvent.getEventType())) {
                    AttemptStartedEvent event = objectMapper.readValue(outboxEvent.getPayload(), AttemptStartedEvent.class);
                    kafkaTemplate.send(ATTEMPT_STARTED_TOPIC, event.userId(), event).get();
                } else {
                    AttemptSubmittedEvent event = objectMapper.readValue(outboxEvent.getPayload(), AttemptSubmittedEvent.class);
                    kafkaTemplate.send(ATTEMPT_SUBMITTED_TOPIC, event.userId(), event).get();
                }

                outboxEvent.setPublishedAt(Instant.now());
                outboxEventRepository.save(outboxEvent);
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}", outboxEvent.getId(), e);
            }
        }
    }
}
