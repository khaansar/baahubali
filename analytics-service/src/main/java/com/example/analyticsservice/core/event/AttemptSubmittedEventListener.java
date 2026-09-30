package com.example.analyticsservice.core.event;

import com.example.analyticsservice.contract.AnalyticsKafkaTopics;
import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.core.exception.InvalidAttemptEventException;
import com.example.analyticsservice.core.service.AnalyticsOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

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
            throw new InvalidAttemptEventException(
                    "Unable to deserialize attempt analytics event"
            );
        }

        log.info(
                "Received attempt analytics event, attemptId={}",
                event.attemptId()
        );

        orchestrator.process(event);
    }
}