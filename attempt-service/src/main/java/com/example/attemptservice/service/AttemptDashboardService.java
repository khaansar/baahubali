package com.example.attemptservice.service;

import com.example.attemptservice.client.TestServiceFeignClient;
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
    private final TestServiceFeignClient testServiceFeignClient;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InProgressAttemptDto getInProgressAttempt(String userId) {
        List<Attempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(userId);
        Optional<Attempt> inProgress = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .findFirst();

        if (inProgress.isEmpty()) {
            return null; // Return empty or handle gracefully
        }

        Attempt attempt = inProgress.get();
        InternalTestBlueprintDto blueprint = null;
        try {
            blueprint = testServiceFeignClient.getTestBlueprint(attempt.getTestId()).data();
        } catch (Exception e) {
            log.error("Failed to fetch blueprint for test: " + attempt.getTestId(), e);
        }

        String testTitle = blueprint != null ? blueprint.getTitle() : "Unknown Test";
        int totalQuestions = 0;
        if (blueprint != null && blueprint.getSections() != null) {
            totalQuestions = blueprint.getSections().stream()
                    .mapToInt(s -> s.getQuestions() != null ? s.getQuestions().size() : 0)
                    .sum();
        }

        int answeredCount = 0;
        Optional<AttemptRedisHash> hashOpt = attemptRedisRepository.findById(attempt.getId());
        if (hashOpt.isPresent() && hashOpt.get().getAnswersJson() != null) {
            try {
                Map<String, String> answers = objectMapper.readValue(hashOpt.get().getAnswersJson(), new TypeReference<Map<String, String>>() {});
                answeredCount = answers.size();
            } catch (Exception e) {
                log.error("Failed to parse redis answers for attempt: " + attempt.getId(), e);
                answeredCount = attemptAnswerRepository.findByAttemptId(attempt.getId()).size();
            }
        } else {
            answeredCount = attemptAnswerRepository.findByAttemptId(attempt.getId()).size();
        }

        int progressPercentage = totalQuestions > 0 ? (int) ((answeredCount * 100.0) / totalQuestions) : 0;

        return new InProgressAttemptDto(
                attempt.getId(),
                attempt.getTestId(),
                testTitle,
                attempt.getStartedAt(),
                progressPercentage
        );
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

            if (currentStreak > maxStreak) {
                maxStreak = currentStreak;
            }

            lastActiveDate = date;
        }
        
        // Reset current streak if last active date is more than 1 day ago
        if (lastActiveDate != null && lastActiveDate.isBefore(today.minusDays(1))) {
            currentStreak = 0;
        }
        
        return new StreakResponseDto(currentStreak, maxStreak, totalActiveDays, activityList);
    }
}
