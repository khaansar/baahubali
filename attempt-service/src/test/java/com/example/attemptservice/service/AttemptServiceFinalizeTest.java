package com.example.attemptservice.service;

import com.example.attemptservice.client.TestServiceClient;
import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptStatus;
import com.example.attemptservice.redis.AttemptRedisRepository;
import com.example.attemptservice.repository.ActiveAttemptRepository;
import com.example.attemptservice.repository.AttemptAnswerRepository;
import com.example.attemptservice.repository.AttemptRepository;
import com.example.attemptservice.repository.OutboxEventRepository;
import com.example.attemptservice.worker.AttemptFlushWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceFinalizeTest {

    @Mock private AttemptRepository attemptRepository;
    @Mock private ActiveAttemptRepository activeAttemptRepository;
    @Mock private AttemptAnswerRepository attemptAnswerRepository;
    @Mock private AttemptRedisRepository attemptRedisRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private AttemptFlushWorker attemptFlushWorker;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private TestServiceClient testServiceClient;
    @Mock private EntityManager entityManager;

    private AttemptService service;

    @BeforeEach
    void setUp() {
        service = new AttemptService(attemptRepository, activeAttemptRepository, attemptAnswerRepository,
                attemptRedisRepository, outboxEventRepository, attemptFlushWorker, redisTemplate,
                new ObjectMapper(), testServiceClient);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    @Test
    void staleDetachedCopyDoesNotRefinalizeAnAlreadySubmittedAttempt() {
        Attempt stale = attempt(AttemptStatus.IN_PROGRESS);
        when(entityManager.contains(stale)).thenReturn(false);
        when(attemptRepository.findByIdForUpdate("a-1")).thenReturn(Optional.of(attempt(AttemptStatus.SUBMITTED)));

        service.finalizeAttempt(stale, AttemptStatus.EXPIRED);

        verify(outboxEventRepository, never()).save(any());
        verify(attemptRepository, never()).save(any());
        verify(activeAttemptRepository, never()).releaseByAttemptId(any());
    }

    @Test
    void managedCopyIsRefreshedUnderLockBeforeFinalizing() {
        Attempt managed = attempt(AttemptStatus.IN_PROGRESS);
        when(entityManager.contains(managed)).thenReturn(true);
        doAnswer(invocation -> {
            managed.setStatus(AttemptStatus.SUBMITTED);
            return null;
        }).when(entityManager).refresh(managed, LockModeType.PESSIMISTIC_WRITE);

        service.finalizeAttempt(managed, AttemptStatus.EXPIRED);

        assertEquals(AttemptStatus.SUBMITTED, managed.getStatus());
        verify(outboxEventRepository, never()).save(any());
    }

    private static Attempt attempt(AttemptStatus status) {
        return Attempt.builder()
                .id("a-1")
                .userId("u-1")
                .testId("t-1")
                .startedAt(Instant.now().minusSeconds(7200))
                .durationMinutes(60)
                .status(status)
                .build();
    }
}
