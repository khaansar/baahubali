package com.example.analyticsservice.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.SectionAnswerPayload;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class AttemptSubmittedEventListenerTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private AnalyticsOrchestrator orchestrator;
    private AttemptSubmittedEventListener listener;

    @BeforeEach
    void setUp() {
        orchestrator = mock(AnalyticsOrchestrator.class);
        listener = new AttemptSubmittedEventListener(orchestrator, mapper);
    }

    @Test
    void eventReachesOrchestrator() {
        AttemptSubmittedEvent event = new AttemptSubmittedEvent(
                "att-1", "user-1", "cat-1", "series-1", "test-1", 5420L,
                List.of(new SectionAnswerPayload("s1", "Physics", 65.0, 100.0, 30, 20, 5, 5, 80.0, 1800L)));

        listener.onMessage(mapper.writeValueAsString(event));

        verify(orchestrator).process(event);
    }

    @Test
    void unreadableMessageIsSkipped() {
        listener.onMessage("not json");
        verify(orchestrator, never()).process(any());
    }

    @Test
    void legacyEventIsForwardedWithoutInventedDefaults() {
        String legacy = "{\"eventId\":\"e1\",\"attemptId\":\"att-1\",\"userId\":\"u1\","
                + "\"testId\":\"t1\",\"timestamp\":\"2026-01-01T00:00:00Z\"}";

        listener.onMessage(legacy);

        ArgumentCaptor<AttemptSubmittedEvent> captor = ArgumentCaptor.forClass(AttemptSubmittedEvent.class);
        verify(orchestrator).process(captor.capture());
        assertThat(captor.getValue().categoryId()).isNull();
        assertThat(captor.getValue().testSeriesId()).isNull();
        assertThat(captor.getValue().timeTakenSeconds()).isNull();
        assertThat(captor.getValue().sectionAnswers()).isNull();
    }

    @Test
    void invalidEventIsSwallowed() {
        doThrow(new InvalidAttemptEventException("missing")).when(orchestrator).process(any());
        listener.onMessage("{\"attemptId\":\"att-1\"}"); // must not throw
    }

    @Test
    void processingFailurePropagatesForRetry() {
        doThrow(new AnalyticsProcessingException("boom", new RuntimeException())).when(orchestrator).process(any());
        assertThatThrownBy(() -> listener.onMessage("{\"attemptId\":\"att-1\"}"))
                .isInstanceOf(AnalyticsProcessingException.class);
    }
}