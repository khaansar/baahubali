package com.example.attemptservice.service;

import com.example.attemptservice.client.PaymentClient;
import com.example.attemptservice.client.TestServiceClient;
import com.example.attemptservice.config.AttemptTimingProperties;
import com.example.attemptservice.dto.PatchAttemptRequest;
import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptStatus;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Answer saves just after the deadline are accepted within the grace period, and never after finalization. */
@ExtendWith(MockitoExtension.class)
class AttemptServiceSaveGraceTest {

    private static final int DURATION_SEC = 3600;
    private static final long GRACE_SEC = 10;

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
    void saveShortlyAfterTheDeadlineIsAcceptedWithinGrace() {
        givenAttempt(AttemptStatus.IN_PROGRESS, 5);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        Long version = service().patchAttempt("a-1", "u-1", request());

        assertThat(version).isEqualTo(4L);
    }

    @Test
    void saveAfterTheGracePeriodIsRejected() {
        givenAttempt(AttemptStatus.IN_PROGRESS, GRACE_SEC + 5);

        assertThatThrownBy(() -> service().patchAttempt("a-1", "u-1", request()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.GONE));
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any(Object[].class));
    }

    @Test
    void saveToAFinalizedAttemptIsRejectedEvenWithinGrace() {
        when(attemptRepository.findByIdForUpdate("a-1"))
                .thenReturn(Optional.of(attempt(AttemptStatus.EXPIRED)));

        assertThatThrownBy(() -> service().patchAttempt("a-1", "u-1", request()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.GONE));
        verify(attemptRedisRepository, never()).findById(any());
    }

    @Test
    void graceOutsideTheAllowedRangeFailsAtStartup() {
        assertThatThrownBy(() -> new AttemptTimingProperties(-1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ATTEMPT_SAVE_GRACE_SECONDS");
        assertThatThrownBy(() -> new AttemptTimingProperties(121))
                .isInstanceOf(IllegalStateException.class);
    }

    private void givenAttempt(AttemptStatus status, long secondsPastDeadline) {
        when(attemptRepository.findByIdForUpdate("a-1")).thenReturn(Optional.of(attempt(status)));
        AttemptRedisHash hash = AttemptRedisHash.builder()
                .attemptId("a-1")
                .userId("u-1")
                .startedAt(Instant.now().getEpochSecond() - DURATION_SEC - secondsPastDeadline)
                .durationSec(DURATION_SEC)
                .version(3L)
                .answersJson("{}")
                .build();
        when(attemptRedisRepository.findById("a-1")).thenReturn(Optional.of(hash));
    }

    private AttemptService service() {
        return new AttemptService(attemptRepository, activeAttemptRepository, attemptAnswerRepository,
                attemptRedisRepository, outboxEventRepository, attemptFlushWorker, redisTemplate,
                new ObjectMapper(), testServiceClient, new AttemptTimingProperties(GRACE_SEC), paymentClient);
    }

    private static PatchAttemptRequest request() {
        PatchAttemptRequest request = new PatchAttemptRequest();
        request.setQuestionId("q-1");
        request.setSelectedOption("A");
        request.setCurrentQuestionIndex(0);
        request.setVersion(3L);
        return request;
    }

    private static Attempt attempt(AttemptStatus status) {
        return Attempt.builder()
                .id("a-1")
                .userId("u-1")
                .testId("t-1")
                .startedAt(Instant.now().minusSeconds(DURATION_SEC))
                .durationMinutes(DURATION_SEC / 60)
                .status(status)
                .build();
    }
}
