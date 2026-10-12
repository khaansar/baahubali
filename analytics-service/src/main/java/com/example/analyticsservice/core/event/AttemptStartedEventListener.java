package com.example.analyticsservice.core.event;

import com.example.analyticsservice.contract.AnalyticsKafkaTopics;
import com.example.analyticsservice.contract.AttemptStartedEvent;
import com.example.analyticsservice.core.exception.InvalidAttemptEventException;
import com.example.analyticsservice.core.service.TestAttemptRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttemptStartedEventListener {

    private final TestAttemptRegistryService testAttemptRegistryService;
    private final JsonMapper jsonMapper;

    @KafkaListener(
            topics = AnalyticsKafkaTopics.ATTEMPT_STARTED,
            groupId = "${spring.application.name}",
            properties = "auto.offset.reset=earliest")
    public void onMessage(String payload) {
        AttemptStartedEvent event;
        try {
            event = jsonMapper.readValue(payload, AttemptStartedEvent.class);
        } catch (Exception ex) {
            throw new InvalidAttemptEventException("Unable to deserialize attempt-started event");
        }

        testAttemptRegistryService.register(event.attemptId(), event.testId(), event.userId(), event.startedAt());
        log.debug("Registered attempt start for leaderboard, attemptId={}", event.attemptId());
    }
}
