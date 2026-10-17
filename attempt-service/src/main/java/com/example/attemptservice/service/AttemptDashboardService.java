package com.example.attemptservice.service;

import com.example.attemptservice.client.TestServiceClient;
import com.example.attemptservice.dto.InProgressAttemptDto;
import com.example.attemptservice.dto.StreakResponseDto;
import com.example.attemptservice.dto.internal.InternalTestBlueprintDto;
import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptStatus;
import com.example.attemptservice.redis.AttemptRedisHash;
import com.example.attemptservice.redis.AttemptRedisRepository;
import com.example.attemptservice.repository.AttemptRepository;
import com.example.attemptservice.repository.AttemptAnswerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttemptDashboardService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final AttemptRedisRepository attemptRedisRepository;
    private final TestServiceClient testServiceClient;
    private final EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public InProgressAttemptDto getInProgressAttempt(String userId) {
        return attemptRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, AttemptStatus.IN_PROGRESS)
                .map(this::toInProgressAttemptDto)
                .orElse(null);
    }

    public List<InProgressAttemptDto> getInProgressAttempts(String userId) {
        return attemptRepository.findByUserIdAndStatusOrderByStartedAtDesc(userId, AttemptStatus.IN_PROGRESS)
                .stream()
                .map(this::toInProgressAttemptDto)
                .toList();
    }

    private InProgressAttemptDto toInProgressAttemptDto(Attempt attempt) {
        InternalTestBlueprintDto blueprint = null;

        try {
            var response = testServiceClient.getTestBlueprint(attempt.getTestId());

            if (response != null && response.success()) {
                blueprint = response.data();
            }
        } catch (Exception e) {
            log.error("Failed to fetch blueprint for test: {}", attempt.getTestId(), e);
        }

        String testTitle = blueprint != null && blueprint.getTitle() != null
                ? blueprint.getTitle()
                : "Unknown Test";

        int totalQuestions = 0;

        if (blueprint != null && blueprint.getSections() != null) {
            totalQuestions = blueprint.getSections()
                    .stream()
                    .mapToInt(section -> section.getQuestions() != null ? section.getQuestions().size() : 0)
                    .sum();
        }

        int answeredCount = getAnsweredCount(attempt.getId());

        int progressPercentage = totalQuestions > 0
                ? Math.min(100, (int) ((answeredCount * 100.0) / totalQuestions))
                : 0;

        long remainingSeconds = calculateRemainingSeconds(attempt);

        return new InProgressAttemptDto(
                attempt.getId(),
                attempt.getTestId(),
                testTitle,
                attempt.getStartedAt(),
                progressPercentage,
                remainingSeconds
        );
    }

    private int getAnsweredCount(String attemptId) {
        Optional<AttemptRedisHash> hashOpt = attemptRedisRepository.findById(attemptId);

        if (hashOpt.isPresent() && hashOpt.get().getAnswersJson() != null) {
            try {
                Map<String, String> answers = objectMapper.readValue(
                        hashOpt.get().getAnswersJson(),
                        new TypeReference<Map<String, String>>() {}
                );

                return answers.size();
            } catch (Exception e) {
                log.error("Failed to parse Redis answers for attempt: {}", attemptId, e);
            }
        }

        return attemptAnswerRepository.findByAttemptId(attemptId).size();
    }

    private long calculateRemainingSeconds(Attempt attempt) {
        if (attempt.getStartedAt() == null || attempt.getDurationMinutes() == null || attempt.getDurationMinutes() <= 0) {
            log.warn("Cannot calculate remaining time for attempt {}: startedAt or durationMinutes is invalid", attempt.getId());
            return 0L;
        }

        Instant deadline = attempt.getStartedAt().plusSeconds(attempt.getDurationMinutes() * 60L);

        return Math.max(0L, ChronoUnit.SECONDS.between(Instant.now(), deadline));
    }

    public StreakResponseDto getYearlyStreak(String userId) {
        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);

        String sql = "SELECT DATE(started_at) as d, COUNT(id) as c " +
                "FROM attempts " +
                "WHERE user_id = :userId AND started_at >= :oneYearAgo " +
                "GROUP BY DATE(started_at) " +
                "ORDER BY DATE(started_at)";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("oneYearAgo", oneYearAgo);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<StreakResponseDto.StreakActivity> activityList = new ArrayList<>();

        int currentStreak = 0;
        int maxStreak = 0;
        int totalActiveDays = results.size();

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate lastActiveDate = null;

        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            int count = ((Number) row[1]).intValue();

            activityList.add(
                    new StreakResponseDto.StreakActivity(
                            date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            count
                    )
            );

            if (lastActiveDate == null) {
                currentStreak = 1;
            } else if (lastActiveDate.plusDays(1).equals(date)) {
                currentStreak++;
            } else {
                currentStreak = 1;
            }

            maxStreak = Math.max(maxStreak, currentStreak);
            lastActiveDate = date;
        }

        if (lastActiveDate != null && lastActiveDate.isBefore(today.minusDays(1))) {
            currentStreak = 0;
        }

        return new StreakResponseDto(
                currentStreak,
                maxStreak,
                totalActiveDays,
                activityList
        );
    }
}
