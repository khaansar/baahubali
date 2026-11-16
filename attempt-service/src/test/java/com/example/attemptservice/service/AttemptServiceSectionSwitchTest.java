package com.example.attemptservice.service;

import com.example.attemptservice.client.PaymentClient;
import com.example.attemptservice.client.TestServiceClient;
import com.example.attemptservice.config.AttemptTimingProperties;
import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptStatus;
import com.example.attemptservice.exception.AttemptNotFoundException;
import com.example.attemptservice.redis.AttemptRedisHash;
import com.example.attemptservice.redis.AttemptRedisRepository;
import com.example.attemptservice.repository.ActiveAttemptRepository;
import com.example.attemptservice.repository.AttemptAnswerRepository;
import com.example.attemptservice.repository.AttemptRepository;
import com.example.attemptservice.repository.OutboxEventRepository;
import com.example.attemptservice.worker.AttemptFlushWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A section switch must not write the whole hash back, or it can overwrite an answer saved at the same moment. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttemptServiceSectionSwitchTest {

    private static final int DURATION_SEC = 3600;

    @Mock private AttemptRepository attemptRepository;
    @Mock private ActiveAttemptRepository activeAttemptRepository;
    @Mock private AttemptAnswerRepository attemptAnswerRepository;
    @Mock private AttemptRedisRepository attemptRedisRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private AttemptFlushWorker attemptFlushWorker;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private TestServiceClient testServiceClient;
    @Mock private PaymentClient paymentClient;

    @Test
    void switchingSectionsWritesOnlyTheSectionFields() {
        givenAttemptInSection("s-1");
        when(redisTemplate.execute(eq(AttemptService.UPDATE_SECTION_FIELDS), eq(List.of("attempt:a-1")),
                eq("s-2"), anyString(), anyString())).thenReturn(1L);

        service().switchSection("a-1", "u-1", "s-2", false);

        verify(attemptRedisRepository, never()).save(any());
        verify(redisTemplate).execute(eq(AttemptService.UPDATE_SECTION_FIELDS), eq(List.of("attempt:a-1")),
                eq("s-2"), anyString(), anyString());
    }

    @Test
    void switchingAfterTheAttemptLeftRedisFailsInsteadOfRecreatingIt() {
        givenAttemptInSection("s-1");
        when(redisTemplate.execute(eq(AttemptService.UPDATE_SECTION_FIELDS), eq(List.of("attempt:a-1")),
                eq("s-2"), anyString(), anyString())).thenReturn(0L);

        assertThatThrownBy(() -> service().switchSection("a-1", "u-1", "s-2", false))
                .isInstanceOf(AttemptNotFoundException.class);
        verify(attemptRedisRepository, never()).save(any());
    }

    private void givenAttemptInSection(String sectionId) {
        Attempt attempt = Attempt.builder()
                .id("a-1")
                .userId("u-1")
                .testId("t-1")
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now().minusSeconds(60))
                .durationMinutes(DURATION_SEC / 60)
                .build();
        when(attemptRepository.findById("a-1")).thenReturn(Optional.of(attempt));
        when(attemptRedisRepository.findById("a-1")).thenReturn(Optional.of(AttemptRedisHash.builder()
                .attemptId("a-1")
                .userId("u-1")
                .startedAt(Instant.now().minusSeconds(60).getEpochSecond())
                .durationSec(DURATION_SEC)
                .currentSectionId(sectionId)
                .currentSectionStartedAt(Instant.now().minusSeconds(30).getEpochSecond())
                .sectionTimeSpentJson("{}")
                .sectionDurationsJson("{}")
                .answersJson("{\"q-1\":\"A\"}")
                .version(5L)
                .build()));
    }

    private AttemptService service() {
        return new AttemptService(attemptRepository, activeAttemptRepository, attemptAnswerRepository,
                attemptRedisRepository, outboxEventRepository, attemptFlushWorker, redisTemplate,
                new ObjectMapper(), testServiceClient, new AttemptTimingProperties(10), paymentClient);
    }
}
