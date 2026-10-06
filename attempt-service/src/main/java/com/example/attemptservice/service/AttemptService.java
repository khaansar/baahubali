package com.example.attemptservice.service;

import com.example.attemptservice.client.TestServiceFeignClient;
import com.example.attemptservice.dto.AttemptHistorySummary;
import com.example.attemptservice.dto.AttemptReviewResponse;
import com.example.attemptservice.dto.AttemptStateResponse;
import com.example.attemptservice.dto.PatchAttemptRequest;
import com.example.attemptservice.dto.PatchAttemptResponse;
import com.example.attemptservice.dto.QuestionReviewDto;
import com.example.attemptservice.dto.StartAttemptRequest;
import com.example.attemptservice.dto.StartAttemptResponse;
import com.example.attemptservice.dto.SubmitAttemptResponse;
import com.example.attemptservice.dto.internal.InternalTestBlueprintDto;
import com.example.attemptservice.dto.internal.TestServiceResponse;
import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptAnswer;
import com.example.attemptservice.entity.AttemptStatus;
import com.example.attemptservice.entity.OutboxEvent;
import com.example.attemptservice.event.AttemptSubmittedEvent;
import com.example.attemptservice.exception.AttemptNotFoundException;
import com.example.attemptservice.redis.AttemptRedisHash;
import com.example.attemptservice.redis.AttemptRedisRepository;
import com.example.attemptservice.repository.AttemptAnswerRepository;
import com.example.attemptservice.repository.AttemptRepository;
import com.example.attemptservice.repository.ActiveAttemptRepository;
import com.example.attemptservice.repository.OutboxEventRepository;
import com.example.attemptservice.worker.AttemptFlushWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AttemptService {

    private static final String REDIS_KEY_PREFIX = "attempt:";

    private final OutboxEventRepository outboxEventRepository;
    private final AttemptRepository attemptRepository;
    private final ActiveAttemptRepository activeAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final AttemptRedisRepository attemptRedisRepository;
    private final AttemptFlushWorker attemptFlushWorker;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "attempt-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService emitterPushExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "attempt-sse-push");
        thread.setDaemon(true);
        return thread;
    });
    private final TestServiceFeignClient testServiceFeignClient;

    public AttemptService(AttemptRepository attemptRepository,
                           ActiveAttemptRepository activeAttemptRepository,
                           AttemptAnswerRepository attemptAnswerRepository,
                           AttemptRedisRepository attemptRedisRepository,
                           OutboxEventRepository outboxEventRepository,
                           @Lazy AttemptFlushWorker attemptFlushWorker,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           TestServiceFeignClient testServiceFeignClient) {
        this.attemptRepository = attemptRepository;
        this.activeAttemptRepository = activeAttemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.attemptRedisRepository = attemptRedisRepository;
        this.attemptFlushWorker = attemptFlushWorker;
        this.outboxEventRepository = outboxEventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeats, 20, 20, TimeUnit.SECONDS);
        this.testServiceFeignClient = testServiceFeignClient;
    }

    @Transactional
    public StartAttemptResponse startAttempt(StartAttemptRequest request) {
        Instant now = Instant.now();

        TestServiceResponse<InternalTestBlueprintDto> response =
                testServiceFeignClient.getTestBlueprint(request.getTestId());
        if (response == null || !response.success() || response.data() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Test service did not return a test blueprint");
        }
        InternalTestBlueprintDto blueprint = response.data();
        if (!Boolean.TRUE.equals(blueprint.getFree())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A verified entitlement is required for this test");
        }
        if (blueprint.getDurationMinutes() == null || blueprint.getDurationMinutes() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Test service returned an invalid test duration");
        }

        Optional<Attempt> existingAttempt = attemptRepository
                .findFirstByUserIdAndTestIdAndStatusOrderByStartedAtDesc(
                        request.getUserId(),
                        request.getTestId(),
                        AttemptStatus.IN_PROGRESS
                );

        if (existingAttempt.isPresent()) {
            Attempt attempt = existingAttempt.get();
            Instant deadline = attempt.getStartedAt()
                    .plusSeconds(attempt.getDurationMinutes() * 60L);

            if (now.isBefore(deadline)) {
                return StartAttemptResponse.builder()
                        .attemptId(attempt.getId())
                        .deadline(deadline)
                        .createdAt(attempt.getCreatedAt())
                        .updatedAt(attempt.getUpdatedAt())
                        .deletedAt(attempt.getDeletedAt())
                        .build();
            }

            finalizeAttempt(attempt, AttemptStatus.EXPIRED);
            now = Instant.now();
        }

        String attemptId = UUID.randomUUID().toString();
        if (activeAttemptRepository.claim(request.getUserId(), request.getTestId(), attemptId, now) == 0) {
            Attempt activeAttempt = activeAttemptRepository.findByUserIdAndTestId(
                            request.getUserId(), request.getTestId())
                    .flatMap(active -> attemptRepository.findById(active.getAttemptId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "An attempt is already being started; retry the request"));
            Instant activeDeadline = activeAttempt.getStartedAt()
                    .plusSeconds(activeAttempt.getDurationMinutes() * 60L);
            if (activeAttempt.getStatus() == AttemptStatus.IN_PROGRESS && now.isBefore(activeDeadline)) {
                return StartAttemptResponse.builder()
                        .attemptId(activeAttempt.getId())
                        .deadline(activeDeadline)
                        .createdAt(activeAttempt.getCreatedAt())
                        .updatedAt(activeAttempt.getUpdatedAt())
                        .deletedAt(activeAttempt.getDeletedAt())
                        .build();
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The active attempt is being finalized; retry the request");
        }

        Attempt attempt = Attempt.builder()
                .id(attemptId)
                .userId(request.getUserId())
                .testId(request.getTestId())
                .startedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .durationMinutes(blueprint.getDurationMinutes())
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        Attempt saved = attemptRepository.save(attempt);

        Instant deadline = saved.getStartedAt()
                .plusSeconds(saved.getDurationMinutes() * 60L);

        TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        attemptRedisRepository.save(AttemptRedisHash.builder()
                                .attemptId(saved.getId())
                                .userId(saved.getUserId())
                                .examId(saved.getTestId())
                                .startedAt(saved.getStartedAt().getEpochSecond())
                                .durationSec(saved.getDurationMinutes() * 60)
                                .status(saved.getStatus().name())
                                .dirtyFlag(false)
                                .currentQuestionIndex(0)
                                .answersJson("{}")
                                .version(0L)
                                .ttlSeconds((long) saved.getDurationMinutes() * 60 + 3600)
                                .build());
                    }
                });

        return StartAttemptResponse.builder()
                .attemptId(saved.getId())
                .deadline(deadline)
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .deletedAt(saved.getDeletedAt())
                .build();
    }

    @Transactional
    public SubmitAttemptResponse submitAttempt(String attemptId, String userId) {
        Attempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));

        verifyOwnership(attempt, userId);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED || attempt.getStatus() == AttemptStatus.EXPIRED) {
            return toSubmitResponse(attempt);
        }

        finalizeAttempt(attempt, AttemptStatus.SUBMITTED);
        return toSubmitResponse(attempt);
    }

    @Transactional
    public void finalizeAttempt(Attempt attempt, AttemptStatus finalStatus) {
        if (finalStatus != AttemptStatus.SUBMITTED && finalStatus != AttemptStatus.EXPIRED) {
            throw new IllegalArgumentException("Invalid final attempt status: " + finalStatus);
        }

        Attempt lockedAttempt = attempt;

        if (lockedAttempt.getStatus() == AttemptStatus.SUBMITTED
                || lockedAttempt.getStatus() == AttemptStatus.EXPIRED) {
            return;
        }

        final String attemptId = lockedAttempt.getId();
        Instant finalizedAt = Instant.now();

        Instant deadline = lockedAttempt.getStartedAt()
                .plusSeconds(lockedAttempt.getDurationMinutes() * 60L);

        AttemptStatus effectiveStatus = finalStatus;

        if (finalStatus == AttemptStatus.SUBMITTED && !finalizedAt.isBefore(deadline)) {
            effectiveStatus = AttemptStatus.EXPIRED;
        }

        AttemptRedisHash hash = attemptRedisRepository.findById(attemptId).orElse(null);

        if (hash != null) {
            try {
                attemptFlushWorker.flushAnswers(hash);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to flush final answers for attemptId: " + attemptId,
                        e
                );
            }
        }

        lockedAttempt.setStatus(effectiveStatus);
        lockedAttempt.setUpdatedAt(finalizedAt);
        attemptRepository.save(lockedAttempt);
        activeAttemptRepository.releaseByAttemptId(attemptId);

        try {
            TestServiceResponse<InternalTestBlueprintDto> response =
                    testServiceFeignClient.getTestBlueprint(lockedAttempt.getTestId());

            if (response == null || !response.success() || response.data() == null) {
                throw new IllegalStateException(
                        "Test service did not return a test blueprint for finalized attempt"
                );
            }

            List<AttemptAnswer> answers =
                    attemptAnswerRepository.findByAttemptId(attemptId);

            long elapsedSeconds = Math.max(
                    0L,
                    Duration.between(lockedAttempt.getStartedAt(), finalizedAt).getSeconds()
            );

            long maxDurationSeconds = lockedAttempt.getDurationMinutes() * 60L;
            long timeTakenSeconds = Math.min(elapsedSeconds, maxDurationSeconds);

            AttemptSubmittedEvent event = AttemptAnalyticsEventFactory.create(
                    UUID.randomUUID().toString(),
                    lockedAttempt.getId(),
                    lockedAttempt.getUserId(),
                    response.data(),
                    answers,
                    timeTakenSeconds,
                    finalizedAt
            );

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(event.eventId())
                    .eventType("ATTEMPT_FINALIZED")
                    .aggregateId(lockedAttempt.getId())
                    .payload(objectMapper.writeValueAsString(event))
                    .createdAt(finalizedAt)
                    .build();

            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create attempt finalized outbox event",
                    e
            );
        }

        if (hash != null) {
            TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                attemptRedisRepository.deleteById(attemptId);
                            } catch (Exception e) {
                                log.error(
                                        "Failed to delete Redis state after commit for attemptId: {}",
                                        attemptId,
                                        e
                                );
                            }
                        }
                    }
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<AttemptHistorySummary> getHistory(String userId, int page, int size) {
        Page<Attempt> rawAttempts = attemptRepository.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"))
        );

        List<String> testIds = rawAttempts.stream()
                .map(Attempt::getTestId)
                .distinct()
                .toList();

        Map<String, com.example.attemptservice.dto.internal.InternalTestBulkInfoDto> testInfoMap =
                new java.util.HashMap<>();

        if (!testIds.isEmpty()) {
            try {
                TestServiceResponse<List<com.example.attemptservice.dto.internal.InternalTestBulkInfoDto>> response =
                        testServiceFeignClient.getBulkTestInfo(testIds);

                if (response != null && response.data() != null) {
                    for (com.example.attemptservice.dto.internal.InternalTestBulkInfoDto info : response.data()) {
                        testInfoMap.put(info.getTestId(), info);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch bulk test info for attempt history", e);
            }
        }

        List<AttemptHistorySummary> historySummaries = rawAttempts.stream()
                .map(attempt -> {
                    com.example.attemptservice.dto.internal.InternalTestBulkInfoDto info =
                            testInfoMap.get(attempt.getTestId());

                    return AttemptHistorySummary.builder()
                            .attemptId(attempt.getId())
                            .testId(attempt.getTestId())
                            .testName(info != null ? info.getTestName() : "Unknown Test")
                            .categoryName(info != null ? info.getCategoryName() : "Unknown Category")
                            .status(attempt.getStatus().name())
                            .finalScore(attempt.getFinalScore())
                            .startedAt(attempt.getStartedAt())
                            .createdAt(attempt.getCreatedAt())
                            .updatedAt(attempt.getUpdatedAt())
                            .deletedAt(attempt.getDeletedAt())
                            .build();
                })
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                historySummaries,
                rawAttempts.getPageable(),
                rawAttempts.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AttemptReviewResponse getReview(String attemptId, String userId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));

        verifyOwnership(attempt, userId);

        List<AttemptAnswer> studentAnswers = attemptAnswerRepository.findByAttemptId(attemptId);

        Map<String, String> answerMap = studentAnswers.stream()
                .collect(Collectors.toMap(
                        AttemptAnswer::getQuestionId,
                        AttemptAnswer::getSelectedOption
                ));

        TestServiceResponse<InternalTestBlueprintDto> response =
                testServiceFeignClient.getTestBlueprint(attempt.getTestId());

        if (response == null || !response.success() || response.data() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Test service did not return a test blueprint"
            );
        }

        InternalTestBlueprintDto blueprint = response.data();
        List<QuestionReviewDto> reviewDtos = new ArrayList<>();

        if (blueprint.getSections() != null) {
            for (InternalTestBlueprintDto.InternalSectionDto section : blueprint.getSections()) {
                if (section.getQuestions() != null) {
                    for (InternalTestBlueprintDto.InternalQuestionDto q : section.getQuestions()) {
                        String selected = answerMap.get(q.getQuestionId());

                        String correctOpt = null;

                        if (q.getCorrectAnswer() != null) {
                            if ("MCQ".equalsIgnoreCase(q.getQuestionType())) {
                                Object key = q.getCorrectAnswer().get("key");
                                correctOpt = key != null ? key.toString() : null;
                            } else if ("MULTI_CORRECT".equalsIgnoreCase(q.getQuestionType())) {
                                Object keys = q.getCorrectAnswer().get("keys");
                                correctOpt = keys != null ? keys.toString() : null;
                            } else if ("NUMERICAL".equalsIgnoreCase(q.getQuestionType())) {
                                Object val = q.getCorrectAnswer().get("value");
                                correctOpt = val != null ? val.toString() : null;
                            }
                        }

                        String qText = (q.getTranslations() != null && !q.getTranslations().isEmpty())
                                ? q.getTranslations().get(0).getQuestionText()
                                : "Question text unavailable";

                        reviewDtos.add(QuestionReviewDto.builder()
                                .questionId(q.getQuestionId())
                                .questionText(qText)
                                .selectedOption(selected)
                                .correctOption(correctOpt)
                                .explanation(q.getExplanation())
                                .createdAt(q.getCreatedAt())
                                .updatedAt(q.getUpdatedAt())
                                .deletedAt(q.getDeletedAt())
                                .build());
                    }
                }
            }
        }

        return AttemptReviewResponse.builder()
                .attemptId(attempt.getId())
                .finalScore(attempt.getFinalScore())
                .questions(reviewDtos)
                .createdAt(attempt.getCreatedAt())
                .updatedAt(attempt.getUpdatedAt())
                .deletedAt(attempt.getDeletedAt())
                .build();
    }

    public AttemptStateSnapshot getAttemptState(String attemptId, String userId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));

        verifyOwnership(attempt, userId);

        AttemptRedisHash hash = attemptRedisRepository.findById(attemptId)
                .orElseGet(() -> rehydrate(attemptId));

        return new AttemptStateSnapshot(toStateResponse(hash), hash.getVersion());
    }

    @Transactional
    public Long patchAttempt(String attemptId, String userId, PatchAttemptRequest request) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));

        verifyOwnership(attempt, userId);

        AttemptRedisHash existing = attemptRedisRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));

        if (existing.getStartedAt() == null
                || existing.getDurationSec() == null
                || Instant.now().getEpochSecond() >= existing.getStartedAt() + existing.getDurationSec()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Attempt deadline has expired");
        }

        AttemptRedisHash current = attemptRedisRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));

        if (!request.getVersion().equals(current.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attempt version conflict; reload the latest state and retry"
            );
        }

        Map<String, String> answers = readAnswers(current.getAnswersJson());
        answers.put(request.getQuestionId(), request.getSelectedOption());

        String updatedAnswers = writeAnswers(answers);
        long expectedVersion = current.getVersion();
        long nextVersion = expectedVersion + 1;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
                "local version = redis.call('HGET', KEYS[1], 'version') " +
                "if not version or tonumber(version) ~= tonumber(ARGV[1]) then return 0 end " +
                "local startedAt = tonumber(redis.call('HGET', KEYS[1], 'startedAt')) " +
                "local durationSec = tonumber(redis.call('HGET', KEYS[1], 'durationSec')) " +
                "if not startedAt or not durationSec or tonumber(ARGV[5]) >= startedAt + durationSec then return -1 end " +
                "redis.call('HSET', KEYS[1], 'answersJson', ARGV[2], 'currentQuestionIndex', ARGV[3], 'dirtyFlag', 'true', 'version', ARGV[4]) " +
                "return 1",
                Long.class
        );

        Long result = redisTemplate.execute(
                script,
                List.of(REDIS_KEY_PREFIX + attemptId),
                Long.toString(expectedVersion),
                updatedAnswers,
                request.getCurrentQuestionIndex().toString(),
                Long.toString(nextVersion),
                Long.toString(Instant.now().getEpochSecond())
        );

        if (result == -1L) {
            throw new ResponseStatusException(HttpStatus.GONE, "Attempt deadline has expired");
        }

        if (result == null || result == 0L) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attempt version conflict; reload the latest state and retry"
            );
        }

        attempt.setUpdatedAt(Instant.now());
        attemptRepository.save(attempt);

        return nextVersion;
    }

    public SseEmitter getSseEmitter(String attemptId, String userId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));

        verifyOwnership(attempt, userId);

        SseEmitter emitter = new SseEmitter(0L);

        CopyOnWriteArrayList<SseEmitter> group = emitters.computeIfAbsent(
                attemptId,
                ignored -> new CopyOnWriteArrayList<>()
        );

        group.add(emitter);

        Runnable remove = () -> {
            group.remove(emitter);

            if (group.isEmpty()) {
                emitters.remove(attemptId, group);
            }
        };

        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());

        return emitter;
    }

    private AttemptRedisHash rehydrate(String attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));

        List<AttemptAnswer> savedAnswers =
                attemptAnswerRepository.findByAttemptId(attemptId);

        Map<String, String> answers = new java.util.LinkedHashMap<>();

        for (AttemptAnswer answer : savedAnswers) {
            answers.put(answer.getQuestionId(), answer.getSelectedOption());
        }

        AttemptRedisHash rebuilt = AttemptRedisHash.builder()
                .attemptId(attempt.getId())
                .userId(attempt.getUserId())
                .examId(attempt.getTestId())
                .startedAt(attempt.getStartedAt().getEpochSecond())
                .durationSec(attempt.getDurationMinutes() * 60)
                .status(attempt.getStatus().name())
                .dirtyFlag(false)
                .currentQuestionIndex(0)
                .answersJson(writeAnswers(answers))
                .version(0L)
                .ttlSeconds(Math.max(
                        60L,
                        attempt.getStartedAt()
                                .plusSeconds(attempt.getDurationMinutes() * 60L)
                                .plusSeconds(3600)
                                .getEpochSecond() - Instant.now().getEpochSecond()
                ))
                .build();

        return attemptRedisRepository.save(rebuilt);
    }

    private AttemptStateResponse toStateResponse(AttemptRedisHash hash) {
        Attempt attempt = attemptRepository.findById(hash.getAttemptId())
                .orElseThrow(() -> new AttemptNotFoundException(hash.getAttemptId()));

        Instant expiresAt = hash.getStartedAt() != null && hash.getDurationSec() != null
                ? Instant.ofEpochSecond(hash.getStartedAt() + hash.getDurationSec())
                : null;

        return AttemptStateResponse.builder()
                .attemptId(hash.getAttemptId())
                .userId(hash.getUserId())
                .testId(hash.getExamId())
                .status(hash.getStatus())
                .currentQuestionIndex(hash.getCurrentQuestionIndex())
                .answers(readAnswers(hash.getAnswersJson()))
                .expiresAt(expiresAt)
                .createdAt(attempt.getCreatedAt())
                .updatedAt(attempt.getUpdatedAt())
                .deletedAt(attempt.getDeletedAt())
                .build();
    }

    public record AttemptStateSnapshot(
            AttemptStateResponse state,
            Long attemptVersion
    ) {}

    private Map<String, String> readAnswers(String json) {
        if (json == null || json.isBlank()) {
            return new java.util.LinkedHashMap<>();
        }

        try {
            return new java.util.LinkedHashMap<>(
                    objectMapper.readValue(
                            json,
                            new TypeReference<Map<String, String>>() {}
                    )
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Stored attempt answers are invalid",
                    e
            );
        }
    }

    private String writeAnswers(Map<String, String> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not serialize answers",
                    e
            );
        }
    }

    private void verifyOwnership(Attempt attempt, String userId) {
        if (!attempt.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this attempt"
            );
        }
    }

    private void sendHeartbeats() {
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            long remaining = attemptRedisRepository.findById(entry.getKey())
                    .filter(hash -> hash.getStartedAt() != null && hash.getDurationSec() != null)
                    .map(hash -> hash.getStartedAt()
                            + hash.getDurationSec()
                            - Instant.now().getEpochSecond())
                    .orElse(Long.MIN_VALUE);

            for (SseEmitter emitter : entry.getValue()) {
                emitterPushExecutor.submit(() -> {
                    try {
                        if (remaining >= 0
                                && remaining <= 300
                                && remaining != Long.MIN_VALUE) {
                            emitter.send(SseEmitter.event()
                                    .name("time_warning")
                                    .data(Map.of(
                                            "remainingSeconds",
                                            remaining
                                    )));
                        } else {
                            emitter.send(SseEmitter.event().comment("ping"));
                        }
                    } catch (Exception e) {
                        log.debug(
                                "SSE client disconnected for attemptId: {}",
                                entry.getKey()
                        );

                        entry.getValue().remove(emitter);
                        emitter.complete();

                        if (entry.getValue().isEmpty()) {
                            emitters.remove(entry.getKey(), entry.getValue());
                        }
                    }
                });
            }
        }
    }

    private SubmitAttemptResponse toSubmitResponse(Attempt attempt) {
        return SubmitAttemptResponse.builder()
                .attemptId(attempt.getId())
                .status(attempt.getStatus().name())
                .finalScore(attempt.getFinalScore())
                .createdAt(attempt.getCreatedAt())
                .updatedAt(attempt.getUpdatedAt())
                .deletedAt(attempt.getDeletedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean hasUserAttemptedTest(String userId, String testId) {
        return attemptRepository.existsByUserIdAndTestIdAndStatus(
                userId,
                testId,
                AttemptStatus.SUBMITTED
        );
    }
}
