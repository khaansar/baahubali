package com.example.iam.event;

import com.example.iam.entity.ProcessedAttemptEvent;
import com.example.iam.repository.ProcessedAttemptEventRepository;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.example.iam.service.CalendarAnalyticsService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttemptSubmittedEventConsumer {

    private final UserRepository userRepository;
    private final ProcessedAttemptEventRepository processedAttemptEventRepository;
    private final CalendarAnalyticsService calendarAnalyticsService;

    @KafkaListener(
            topics = "attempt-submitted-events",
            groupId = "iam-service-attempt-stats",
            containerFactory = "attemptSubmittedKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(AttemptSubmittedEvent event) {

        try {
            processedAttemptEventRepository.saveAndFlush(
                    ProcessedAttemptEvent.builder()
                            .eventId(event.eventId())
                            .processedAt(Instant.now())
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            log.debug(
                    "Ignoring duplicate attempt submitted event eventId={}, attemptId={}",
                    event.eventId(),
                    event.attemptId()
            );
            return;
        }

        int updatedRows =
                userRepository.incrementTestsAttemptedCount(event.userId());

        if (updatedRows == 0) {
            throw new IllegalStateException(
                    "No IAM user exists for attempt-submitted event userId="
                            + event.userId()
                            + ", attemptId="
                            + event.attemptId()
            );
        }
        
        calendarAnalyticsService.recordActivity(event.userId(), LocalDate.now(ZoneOffset.UTC));

        log.debug(
                "Incremented tests_attempted_count for userId={}, attemptId={}",
                event.userId(),
                event.attemptId()
        );
    }
}