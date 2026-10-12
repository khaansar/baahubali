package com.example.analyticsservice.core.service;

import com.example.analyticsservice.core.repository.TestAttemptRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TestAttemptRegistryService {

    private final TestAttemptRepository testAttemptRepository;

    @Transactional
    public void register(String attemptId, String testId, String userId, Instant startedAt) {
        Instant effectiveStartedAt = startedAt != null ? startedAt : Instant.now();
        LocalDateTime normalizedStartedAt = LocalDateTime.ofInstant(effectiveStartedAt, ZoneOffset.UTC);
        testAttemptRepository.upsertFirstAttemptRecord(attemptId, testId, userId, normalizedStartedAt);
    }
}
