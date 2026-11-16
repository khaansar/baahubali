package com.example.attemptservice.service;

import com.example.attemptservice.client.PaymentClient;
import com.example.attemptservice.client.TestServiceClient;
import com.example.attemptservice.config.AttemptTimingProperties;
import com.example.attemptservice.dto.PatchAttemptRequest;
import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptAnswer;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** "Marked for review" is saved with each answer save, flushed to the database and restored. */
@ExtendWith(MockitoExtension.class)
class AttemptReviewMarkTest {

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
    void markingAQuestionAddsItToTheStoredMarks() {
        assertThat(savedMarks("[\"q-2\"]", true)).isEqualTo("[\"q-2\",\"q-1\"]");
    }

    @Test
    void unmarkingAQuestionRemovesItFromTheStoredMarks() {
        assertThat(savedMarks("[\"q-1\",\"q-2\"]", false)).isEqualTo("[\"q-2\"]");
    }

    @Test
    void aSaveWithoutAReviewFlagKeepsTheStoredMarks() {
        assertThat(savedMarks("[\"q-1\"]", null)).isEqualTo("[\"q-1\"]");
    }

    @Test
    void flushStoresTheReviewFlagWithEachAnswer() throws Exception {
        AttemptFlushWorker worker = new AttemptFlushWorker(
                attemptRedisRepository, attemptAnswerRepository, attemptRepository, new ObjectMapper(), redisTemplate);
        when(attemptAnswerRepository.findByAttemptId("a-1")).thenReturn(List.of());

        worker.flushAnswers(AttemptRedisHash.builder()
                .attemptId("a-1")
                .answersJson("{\"q-1\":\"A\",\"q-2\":null}")
                .markedJson("[\"q-2\"]")
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttemptAnswer>> saved = ArgumentCaptor.forClass(List.class);
        verify(attemptAnswerRepository).saveAll(saved.capture());
        Map<String, Boolean> marks = saved.getValue().stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, AttemptAnswer::isMarkedForReview));
        assertThat(marks).isEqualTo(Map.of("q-1", false, "q-2", true));
    }

    /** Saves q-1 with the given review flag and returns the marks written to Redis. */
    private String savedMarks(String storedMarks, Boolean markedForReview) {
        when(attemptRepository.findByIdForUpdate("a-1")).thenReturn(Optional.of(attempt()));
        when(attemptRedisRepository.findById("a-1")).thenReturn(Optional.of(AttemptRedisHash.builder()
                .attemptId("a-1")
                .userId("u-1")
                .startedAt(Instant.now().getEpochSecond())
                .durationSec(DURATION_SEC)
                .version(3L)
                .answersJson("{}")
                .markedJson(storedMarks)
                .build()));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        PatchAttemptRequest request = new PatchAttemptRequest();
        request.setQuestionId("q-1");
        request.setCurrentQuestionIndex(0);
        request.setVersion(3L);
        request.setMarkedForReview(markedForReview);
        service().patchAttempt("a-1", "u-1", request);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), args.capture());
        Object[] values = args.getValue();
        return (String) values[values.length - 1];
    }

    private AttemptService service() {
        return new AttemptService(attemptRepository, activeAttemptRepository, attemptAnswerRepository,
                attemptRedisRepository, outboxEventRepository, attemptFlushWorker, redisTemplate,
                new ObjectMapper(), testServiceClient, new AttemptTimingProperties(10), paymentClient);
    }

    private static Attempt attempt() {
        return Attempt.builder()
                .id("a-1")
                .userId("u-1")
                .testId("t-1")
                .startedAt(Instant.now())
                .durationMinutes(DURATION_SEC / 60)
                .status(AttemptStatus.IN_PROGRESS)
                .build();
    }
}
