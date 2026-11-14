package com.example.iam.event;

import com.example.iam.repository.ProcessedAttemptEventRepository;
import com.example.iam.repository.UserRepository;
import com.example.iam.service.CalendarAnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptSubmittedEventConsumerTest {

    @Mock private UserRepository userRepository;
    @Mock private ProcessedAttemptEventRepository processedAttemptEventRepository;
    @Mock private CalendarAnalyticsService calendarAnalyticsService;

    @InjectMocks private AttemptSubmittedEventConsumer consumer;

    @Test
    void recordsActivityOnTheSubmissionDayEvenWhenProcessedLater() {
        Instant submittedAt = Instant.parse("2026-11-13T23:59:30Z");
        when(userRepository.incrementTestsAttemptedCount("user-1")).thenReturn(1);

        consumer.consume(new AttemptSubmittedEvent("evt-1", "att-1", "user-1", "test-1", submittedAt));

        verify(calendarAnalyticsService).recordActivity("user-1", LocalDate.of(2026, 11, 13));
    }

    @Test
    void fallsBackToTodayWhenTheEventHasNoTimestamp() {
        when(userRepository.incrementTestsAttemptedCount("user-1")).thenReturn(1);

        consumer.consume(new AttemptSubmittedEvent("evt-2", "att-2", "user-1", "test-1", null));

        verify(calendarAnalyticsService).recordActivity("user-1", LocalDate.now(ZoneOffset.UTC));
    }
}
