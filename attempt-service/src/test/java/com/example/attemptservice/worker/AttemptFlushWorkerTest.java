package com.example.attemptservice.worker;

import com.example.attemptservice.redis.AttemptRedisHash;
import com.example.attemptservice.redis.AttemptRedisRepository;
import com.example.attemptservice.repository.AttemptAnswerRepository;
import com.example.attemptservice.repository.AttemptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The flush must never write its copy of the hash back to Redis: an answer saved while the
 * flush ran would be overwritten by that older copy and lost after the student saw it saved.
 */
@ExtendWith(MockitoExtension.class)
class AttemptFlushWorkerTest {

    @Mock private AttemptRedisRepository attemptRedisRepository;
    @Mock private AttemptAnswerRepository attemptAnswerRepository;
    @Mock private AttemptRepository attemptRepository;
    @Mock private StringRedisTemplate redisTemplate;

    @Test
    void flushClearsOnlyTheDirtyFlagAndOnlyForTheVersionItFlushed() {
        when(attemptRedisRepository.findAll()).thenReturn(List.of(dirtyHash(7L)));
        when(attemptAnswerRepository.findByAttemptId("a-1")).thenReturn(List.of());

        worker().flushDirtyHashes();

        verify(attemptRedisRepository, never()).save(any());
        verify(redisTemplate).execute(
                eq(AttemptFlushWorker.CLEAR_DIRTY_IF_UNCHANGED),
                eq(List.of("attempt:a-1")),
                eq("7"));
    }

    @Test
    void aFailedFlushLeavesTheHashDirtyForTheNextRun() {
        when(attemptRedisRepository.findAll()).thenReturn(List.of(dirtyHash(7L)));
        when(attemptAnswerRepository.findByAttemptId("a-1")).thenThrow(new IllegalStateException("db down"));

        worker().flushDirtyHashes();

        verify(attemptRedisRepository, never()).save(any());
        verify(redisTemplate, never()).execute(any(), anyList(), any(Object[].class));
    }

    private AttemptFlushWorker worker() {
        return new AttemptFlushWorker(
                attemptRedisRepository, attemptAnswerRepository, attemptRepository, new ObjectMapper(), redisTemplate);
    }

    private static AttemptRedisHash dirtyHash(long version) {
        return AttemptRedisHash.builder()
                .attemptId("a-1")
                .dirtyFlag(true)
                .version(version)
                .answersJson("{\"q-1\":\"A\"}")
                .build();
    }
}
