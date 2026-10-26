package com.example.attemptservice.service;

import com.example.attemptservice.client.TestServiceClient;
import com.example.attemptservice.dto.AttemptHistorySummary;
import com.example.attemptservice.dto.AttemptReviewResponse;
import com.example.attemptservice.dto.AttemptStateResponse;
import com.example.attemptservice.dto.PatchAttemptRequest;
import com.example.attemptservice.dto.PatchAttemptResponse;
import com.example.attemptservice.dto.QuestionReviewDto;
import com.example.attemptservice.dto.SectionReviewDto;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
    private final TestServiceClient testServiceClient;

    public AttemptService(AttemptRepository attemptRepository,
                           ActiveAttemptRepository activeAttemptRepository,
                           AttemptAnswerRepository attemptAnswerRepository,
                           AttemptRedisRepository attemptRedisRepository,
                           OutboxEventRepository outboxEventRepository,
                           @Lazy AttemptFlushWorker attemptFlushWorker,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           TestServiceClient testServiceClient) {
        this.attemptRepository = attemptRepository;
        this.activeAttemptRepository = activeAttemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.attemptRedisRepository = attemptRedisRepository;
        this.attemptFlushWorker = attemptFlushWorker;
        this.outboxEventRepository = outboxEventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeats, 20, 20, TimeUnit.SECONDS);
        this.testServiceClient = testServiceClient;
    }

    @Transactional
    public StartAttemptResponse startAttempt(StartAttemptRequest request) {
        Instant now = Instant.now();

        TestServiceResponse<InternalTestBlueprintDto> response =
                testServiceClient.getTestBlueprint(request.getTestId());
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

        Map<String, Integer> sectionDurations = new java.util.HashMap<>();
        Map<String, List<String>> questionOrderMap = new java.util.LinkedHashMap<>();
        String firstSectionId = null;
        if (blueprint.getSections() != null && !blueprint.getSections().isEmpty()) {
            for (InternalTestBlueprintDto.InternalSectionDto sec : blueprint.getSections()) {
                if (firstSectionId == null) firstSectionId = sec.getSectionId();
                if (sec.getDurationMinutes() != null) {
                    sectionDurations.put(sec.getSectionId(), sec.getDurationMinutes() * 60);
                }
                if (sec.getQuestions() != null && !sec.getQuestions().isEmpty()) {
                    List<String> qIds = new java.util.ArrayList<>();
                    for (InternalTestBlueprintDto.InternalQuestionDto q : sec.getQuestions()) {
                        if (q.getQuestionId() != null) {
                            qIds.add(q.getQuestionId());
                        }
                    }
                    if (Boolean.TRUE.equals(sec.getShuffleQuestions()) && qIds.size() > 1) {
                        java.util.Collections.shuffle(qIds);
                    }
                    questionOrderMap.put(sec.getSectionId(), qIds);
                }
            }
        }
        String sectionDurationsStr = "{}";
        try {
            sectionDurationsStr = objectMapper.writeValueAsString(sectionDurations);
        } catch (Exception e) {}

        String questionOrderJson = null;
        if (!questionOrderMap.isEmpty()) {
            try {
                questionOrderJson = objectMapper.writeValueAsString(questionOrderMap);
            } catch (Exception e) {
                log.warn("Failed to serialize questionOrderMap for attempt {}", attemptId, e);
            }
        }

        String finalFirstSectionId = firstSectionId;
        String finalSectionDurationsStr = sectionDurationsStr;
        String finalQuestionOrderJson = questionOrderJson;

        Attempt attempt = Attempt.builder()
                .id(attemptId)
                .userId(request.getUserId())
                .testId(request.getTestId())
                .startedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .durationMinutes(blueprint.getDurationMinutes())
                .status(AttemptStatus.IN_PROGRESS)
                .sectionTimeSpentJson("{}")
                .questionOrderJson(questionOrderJson)
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
                                .currentSectionId(finalFirstSectionId)
                                .currentSectionStartedAt(saved.getStartedAt().getEpochSecond())
                                .sectionTimeSpentJson("{}")
                                .sectionDurationsJson(finalSectionDurationsStr)
                                .questionOrderJson(finalQuestionOrderJson)
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

    /**
     * Stores the score calculated by analytics. This is intentionally idempotent:
     * Kafka redelivery may cause analytics to submit the same score more than once.
     */
    @Transactional
    public void updateFinalScore(String attemptId, Double finalScore) {
        if (finalScore == null || !Double.isFinite(finalScore)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "finalScore must be a finite number");
        }

        Attempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new AttemptNotFoundException(attemptId));
        attempt.setFinalScore(finalScore);
        attemptRepository.save(attempt);
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
                    testServiceClient.getTestBlueprint(lockedAttempt.getTestId());

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

            /*
             * Keep the analytics event fields unchanged, but add the final attempt
             * status to the outbox payload. Notification consumers can then
             * distinguish a genuinely submitted attempt from an expired attempt.
             */
            ObjectNode eventPayload = objectMapper.valueToTree(event);
            eventPayload.put("status", effectiveStatus.name());

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(event.eventId())
                    .eventType("ATTEMPT_FINALIZED")
                    .aggregateId(lockedAttempt.getId())
                    .payload(objectMapper.writeValueAsString(eventPayload))
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
                        testServiceClient.getBulkTestInfo(testIds);

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

        Map<String, String> answerMap = new HashMap<>();
        for (AttemptAnswer answer : studentAnswers) {
            // A null selection represents an unanswered question.  Do not use
            // Collectors.toMap here: duplicate or null legacy rows must not make
            // a student's review unavailable.
            answerMap.put(answer.getQuestionId(), answer.getSelectedOption());
        }

        TestServiceResponse<InternalTestBlueprintDto> response =
                testServiceClient.getTestBlueprint(attempt.getTestId());

        if (response == null || !response.success() || response.data() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Test service did not return a test blueprint"
            );
        }

        InternalTestBlueprintDto blueprint = response.data();
        Double finalScore = attempt.getFinalScore();
        List<QuestionReviewDto> reviewDtos = new ArrayList<>();
        List<SectionReviewDto> sectionReviewDtos = new ArrayList<>();
        int correctCount = 0;
        int wrongCount = 0;
        int unansweredCount = 0;

        Map<String, List<String>> questionOrderMap = null;
        if (attempt.getQuestionOrderJson() != null && !attempt.getQuestionOrderJson().isBlank()) {
            try {
                questionOrderMap = objectMapper.readValue(attempt.getQuestionOrderJson(), new TypeReference<>() {});
            } catch (Exception e) {}
        }

        if (blueprint.getSections() != null) {
            for (InternalTestBlueprintDto.InternalSectionDto section : blueprint.getSections()) {
                int sectionCorrectCount = 0;
                int sectionWrongCount = 0;
                int sectionUnansweredCount = 0;
                double sectionMarksAwarded = 0;
                if (section.getQuestions() != null) {
                    List<InternalTestBlueprintDto.InternalQuestionDto> sectionQuestions = section.getQuestions();
                    if (questionOrderMap != null && questionOrderMap.containsKey(section.getSectionId())) {
                        List<String> order = questionOrderMap.get(section.getSectionId());
                        Map<String, InternalTestBlueprintDto.InternalQuestionDto> qMap = new java.util.HashMap<>();
                        for (InternalTestBlueprintDto.InternalQuestionDto q : sectionQuestions) {
                            if (q.getQuestionId() != null) {
                                qMap.put(q.getQuestionId(), q);
                            }
                        }
                        List<InternalTestBlueprintDto.InternalQuestionDto> orderedList = new ArrayList<>();
                        for (String qId : order) {
                            InternalTestBlueprintDto.InternalQuestionDto q = qMap.get(qId);
                            if (q != null) orderedList.add(q);
                        }
                        for (InternalTestBlueprintDto.InternalQuestionDto q : sectionQuestions) {
                            if (!order.contains(q.getQuestionId())) {
                                orderedList.add(q);
                            }
                        }
                        sectionQuestions = orderedList;
                    }
                    for (InternalTestBlueprintDto.InternalQuestionDto q : sectionQuestions) {
                        String selected = answerMap.get(q.getQuestionId());
                        String correctOpt = getCorrectOption(q);
                        QuestionResult result = evaluateQuestion(q, selected,
                                Boolean.TRUE.equals(blueprint.getNegativeMarkingEnabled()));

                        switch (result.status()) {
                            case "CORRECT" -> {
                                correctCount++;
                                sectionCorrectCount++;
                            }
                            case "WRONG" -> {
                                wrongCount++;
                                sectionWrongCount++;
                            }
                            default -> {
                                unansweredCount++;
                                sectionUnansweredCount++;
                            }
                        }
                        sectionMarksAwarded += result.marksAwarded();

                        InternalTestBlueprintDto.InternalTranslationDto translation =
                                (q.getTranslations() != null && !q.getTranslations().isEmpty())
                                        ? q.getTranslations().get(0)
                                        : null;

                        String qText = translation != null
                                ? translation.getQuestionText()
                                : "Question text unavailable";

                        reviewDtos.add(QuestionReviewDto.builder()
                                .questionId(q.getQuestionId())
                                .sectionId(section.getSectionId())
                                .sectionTitle(section.getTitle())
                                .questionText(qText)
                                .questionImages(translation != null && translation.getQuestionImages() != null
                                        ? translation.getQuestionImages() : List.of())
                                .optionImages(translation != null && translation.getOptionImages() != null
                                        ? translation.getOptionImages() : Map.of())
                                .options(readOptions(translation != null ? translation.getOptionsJson() : null))
                                .questionType(q.getQuestionType())
                                .selectedOption(selected)
                                .correctOption(correctOpt)
                                .status(result.status())
                                .positiveMarks(decimalValue(q.getPositiveMarks()))
                                .negativeMarks(decimalValue(q.getNegativeMarks()))
                                .marksAwarded(result.marksAwarded())
                                .explanation(q.getExplanation())
                                .explanationImages(q.getExplanationImages() != null ? q.getExplanationImages() : List.of())
                                .createdAt(q.getCreatedAt())
                                .updatedAt(q.getUpdatedAt())
                                .deletedAt(q.getDeletedAt())
                                .build());

                    }
                }
                sectionReviewDtos.add(SectionReviewDto.builder()
                        .sectionId(section.getSectionId())
                        .sectionTitle(section.getTitle())
                        .totalQuestions(section.getQuestions() == null ? 0 : section.getQuestions().size())
                        .correctCount(sectionCorrectCount)
                        .wrongCount(sectionWrongCount)
                        .unansweredCount(sectionUnansweredCount)
                        .marksAwarded(round(sectionMarksAwarded))
                        .build());
            }
        }

        return AttemptReviewResponse.builder()
                .attemptId(attempt.getId())
                .finalScore(finalScore)
                .totalQuestions(reviewDtos.size())
                .correctCount(correctCount)
                .wrongCount(wrongCount)
                .unansweredCount(unansweredCount)
                .sections(sectionReviewDtos)
                .questions(reviewDtos)
                .createdAt(attempt.getCreatedAt())
                .updatedAt(attempt.getUpdatedAt())
                .deletedAt(attempt.getDeletedAt())
                .build();
    }

    private String getCorrectOption(InternalTestBlueprintDto.InternalQuestionDto question) {
        if (question.getCorrectAnswer() == null) {
            return null;
        }

        Object answer = switch (question.getQuestionType() == null ? "" : question.getQuestionType().toUpperCase()) {
            case "MCQ" -> question.getCorrectAnswer().get("key");
            case "MULTI_CORRECT" -> question.getCorrectAnswer().get("keys");
            case "NUMERICAL" -> question.getCorrectAnswer().get("value");
            default -> null;
        };
        return answer == null ? null : answer.toString();
    }

    private QuestionResult evaluateQuestion(InternalTestBlueprintDto.InternalQuestionDto question,
                                            String selectedOption,
                                            boolean negativeMarkingEnabled) {
        if (selectedOption == null || selectedOption.isBlank()) {
            return new QuestionResult("UNANSWERED", 0D);
        }

        if (isCorrect(question, selectedOption)) {
            return new QuestionResult("CORRECT", decimalValue(question.getPositiveMarks()));
        }

        return new QuestionResult("WRONG", negativeMarkingEnabled
                ? -decimalValue(question.getNegativeMarks())
                : 0D);
    }

    private boolean isCorrect(InternalTestBlueprintDto.InternalQuestionDto question, String selectedOption) {
        if (question.getCorrectAnswer() == null || question.getQuestionType() == null) {
            return false;
        }

        return switch (question.getQuestionType().toUpperCase()) {
            case "MCQ" -> matchesText(question.getCorrectAnswer().get("key"), selectedOption);
            case "MULTI_CORRECT" -> normalizeSet(question.getCorrectAnswer().get("keys"))
                    .equals(normalizeSet(selectedOption));
            case "NUMERICAL" -> matchesNumber(question.getCorrectAnswer().get("value"), selectedOption);
            default -> false;
        };
    }

    private boolean matchesText(Object correctAnswer, String selectedOption) {
        return correctAnswer != null && correctAnswer.toString().trim()
                .equalsIgnoreCase(selectedOption.trim());
    }

    private boolean matchesNumber(Object correctAnswer, String selectedOption) {
        if (correctAnswer == null) {
            return false;
        }
        try {
            return new BigDecimal(correctAnswer.toString().trim())
                    .compareTo(new BigDecimal(selectedOption.trim())) == 0;
        } catch (NumberFormatException ex) {
            return matchesText(correctAnswer, selectedOption);
        }
    }

    private Set<String> normalizeSet(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (value instanceof Collection<?> values) {
            return values.stream().filter(Objects::nonNull).map(Object::toString)
                    .map(String::trim).filter(valueText -> !valueText.isBlank())
                    .map(String::toUpperCase).collect(Collectors.toCollection(TreeSet::new));
        }

        String text = value.toString().trim().replace("[", "").replace("]", "")
                .replace("\"", "").replace("'", "");
        if (text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.split(",")).map(String::trim)
                .filter(valueText -> !valueText.isBlank()).map(String::toUpperCase)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Map<String, Object> readOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            log.warn("Unable to parse options JSON for attempt review");
            return Map.of();
        }
    }

    private double decimalValue(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private record QuestionResult(String status, double marksAwarded) {
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

        long now = Instant.now().getEpochSecond();
        String currentSec = current.getCurrentSectionId();
        Map<String, Integer> timeSpentMap = readSectionTimes(current.getSectionTimeSpentJson());
        Map<String, Integer> limitsMap = readSectionTimes(current.getSectionDurationsJson());

        String incomingSec = request.getCurrentSectionId();
        if (incomingSec != null && !incomingSec.equals(currentSec)) {
            // Section switch
            if (currentSec != null && current.getCurrentSectionStartedAt() != null) {
                int spent = timeSpentMap.getOrDefault(currentSec, 0) + (int) (now - current.getCurrentSectionStartedAt());
                timeSpentMap.put(currentSec, spent);
            }
            current.setCurrentSectionId(incomingSec);
            current.setCurrentSectionStartedAt(now);
            try {
                current.setSectionTimeSpentJson(objectMapper.writeValueAsString(timeSpentMap));
            } catch (Exception e) {}
            currentSec = incomingSec;
        } else {
            // Check limit
            if (currentSec != null) {
                int spent = timeSpentMap.getOrDefault(currentSec, 0);
                if (current.getCurrentSectionStartedAt() != null) {
                    spent += (int) (now - current.getCurrentSectionStartedAt());
                }
                Integer limit = limitsMap.get(currentSec);
                if (limit != null && spent > limit) {
                    throw new ResponseStatusException(HttpStatus.GONE, "Section deadline has expired");
                }
            }
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
                "redis.call('HSET', KEYS[1], 'answersJson', ARGV[2], 'currentQuestionIndex', ARGV[3], 'dirtyFlag', 'true', 'version', ARGV[4], 'currentSectionId', ARGV[6], 'currentSectionStartedAt', ARGV[7], 'sectionTimeSpentJson', ARGV[8]) " +
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
                Long.toString(now),
                current.getCurrentSectionId() != null ? current.getCurrentSectionId() : "",
                current.getCurrentSectionStartedAt() != null ? current.getCurrentSectionStartedAt().toString() : Long.toString(now),
                current.getSectionTimeSpentJson() != null ? current.getSectionTimeSpentJson() : "{}"
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

    @Transactional
    public AttemptStateResponse switchSection(String attemptId, String userId, String newSectionId, boolean submitCurrent) {
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

        long now = Instant.now().getEpochSecond();
        String currentSec = existing.getCurrentSectionId();
        Map<String, Integer> timeSpentMap = readSectionTimes(existing.getSectionTimeSpentJson());
        Map<String, Integer> limitsMap = readSectionTimes(existing.getSectionDurationsJson());

        if (newSectionId != null && !newSectionId.equals(currentSec)) {
            // Validate if new section is allowed (e.g. not expired)
            Integer limit = limitsMap.get(newSectionId);
            int spent = timeSpentMap.getOrDefault(newSectionId, 0);
            if (limit != null && spent >= limit) {
                throw new ResponseStatusException(HttpStatus.GONE, "Target section deadline has expired");
            }

            if (currentSec != null && existing.getCurrentSectionStartedAt() != null) {
                int oldSpent = timeSpentMap.getOrDefault(currentSec, 0) + (int) (now - existing.getCurrentSectionStartedAt());
                if (submitCurrent) {
                    Integer oldLimit = limitsMap.get(currentSec);
                    oldSpent = oldLimit != null ? oldLimit : 999999;
                }
                timeSpentMap.put(currentSec, oldSpent);
            }
            existing.setCurrentSectionId(newSectionId);
            existing.setCurrentSectionStartedAt(now);
            try {
                existing.setSectionTimeSpentJson(objectMapper.writeValueAsString(timeSpentMap));
            } catch (Exception e) {}

            attemptRedisRepository.save(existing);
        }

        return toStateResponse(existing);
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
                .currentSectionId(null)
                .sectionTimeSpentJson(attempt.getSectionTimeSpentJson() != null ? attempt.getSectionTimeSpentJson() : "{}")
                .currentSectionStartedAt(Instant.now().getEpochSecond())
                .sectionDurationsJson("{}")
                .questionOrderJson(attempt.getQuestionOrderJson())
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

        Map<String, Integer> timeSpentMap = new java.util.HashMap<>();
        try {
            if (hash.getSectionTimeSpentJson() != null) {
                timeSpentMap = objectMapper.readValue(hash.getSectionTimeSpentJson(), new TypeReference<>() {});
            }
        } catch (Exception e) {}

        Map<String, List<String>> questionOrderMap = null;
        try {
            String qOrderJson = hash.getQuestionOrderJson();
            if (qOrderJson == null || qOrderJson.isBlank()) {
                qOrderJson = attempt.getQuestionOrderJson();
            }
            if (qOrderJson != null && !qOrderJson.isBlank()) {
                questionOrderMap = objectMapper.readValue(qOrderJson, new TypeReference<>() {});
            }
        } catch (Exception e) {}

        return AttemptStateResponse.builder()
                .attemptId(hash.getAttemptId())
                .userId(hash.getUserId())
                .testId(hash.getExamId())
                .status(hash.getStatus())
                .currentQuestionIndex(hash.getCurrentQuestionIndex())
                .currentSectionId(hash.getCurrentSectionId())
                .sectionTimeSpentSec(timeSpentMap)
                .currentSectionStartedAt(hash.getCurrentSectionStartedAt() != null ? Instant.ofEpochSecond(hash.getCurrentSectionStartedAt()) : null)
                .answers(readAnswers(hash.getAnswersJson()))
                .questionOrder(questionOrderMap)
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

    private Map<String, Integer> readSectionTimes(String json) {
        if (json == null || json.isBlank()) return new java.util.HashMap<>();
        try {
            return new java.util.HashMap<>(objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {}));
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

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
