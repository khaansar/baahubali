package com.example.analyticsservice.core.service;

import com.example.analyticsservice.contract.UserPerformanceResponse;
import com.example.analyticsservice.contract.UserTopicPerformanceResponse;
import com.example.analyticsservice.core.entity.ReportStatus;
import com.example.analyticsservice.core.entity.SectionPerformanceEntity;
import com.example.analyticsservice.core.entity.TestReportEntity;
import com.example.analyticsservice.core.entity.UserTopicPerformanceEntity;
import com.example.analyticsservice.core.repository.SectionPerformanceRepository;
import com.example.analyticsservice.core.repository.TestReportRepository;
import com.example.analyticsservice.core.repository.UserTopicPerformanceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPerformanceService {

    private static final int DECIMAL_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal STRENGTH_THRESHOLD = BigDecimal.valueOf(70);
    private static final BigDecimal WEAKNESS_THRESHOLD = BigDecimal.valueOf(60);

    private final TestReportRepository testReportRepository;
    private final SectionPerformanceRepository sectionPerformanceRepository;
    private final UserTopicPerformanceRepository topicPerformanceRepository;

    /**
     * Returns the user's completed-attempt performance, summary metrics,
     * and section-level performance.
     *
     * All BigDecimal values in the response are normalized to two decimal
     * places. Counts, ranks, IDs, timestamps, and integer time values
     * retain their original types and values.
     */
    @Transactional(readOnly = true)
    public UserPerformanceResponse getPerformance(String userId) {
        List<TestReportEntity> reports = testReportRepository.findByUserIdAndStatusOrderByCreatedAtAsc(userId, ReportStatus.COMPLETED.name());

        if (reports.isEmpty()) {
            return new UserPerformanceResponse(
                    new UserPerformanceResponse.PerformanceSummary(
                            0, zero(), zero(), zero(), zero(), zero(), zero(), zero()
                    ),
                    List.of(),
                    List.of()
            );
        }

        /*
         * Build each attempt response with normalized score, percentage,
         * accuracy, and percentile values.
         */
        List<UserPerformanceResponse.AttemptPerformance> attempts = reports.stream()
                .map(this::toAttemptPerformance)
                .toList();

        /*
         * The summary builder uses the original database rows for
         * aggregation, then rounds each returned decimal metric.
         */
        UserPerformanceResponse.PerformanceSummary summary = buildSummary(reports);

        /*
         * Section rows are restricted to attempts that have completed.
         * This prevents section records for incomplete or unrelated
         * attempts from leaking into the aggregate.
         */
        Set<String> completedAttemptIds = reports.stream()
                .map(TestReportEntity::getAttemptId)
                .collect(Collectors.toSet());

        List<SectionPerformanceEntity> sectionRows = sectionPerformanceRepository.findByUserId(userId).stream()
                .filter(section -> completedAttemptIds.contains(section.getAttemptId()))
                .toList();

        List<UserPerformanceResponse.SectionPerformanceSummary> sectionPerformance = buildSectionPerformance(sectionRows);

        return new UserPerformanceResponse(
                summary,
                attempts,
                sectionPerformance
        );
    }

    /**
     * Returns topic-level analytics for the user.
     *
     * Topic accuracy percentages are rounded to two decimal places before
     * the response DTO is constructed. Strength and weakness classification
     * still uses the original accuracy values so rounding cannot change
     * whether a topic meets a threshold.
     */
    @Transactional(readOnly = true)
    public UserTopicPerformanceResponse getTopicPerformance(String userId) {
        List<UserTopicPerformanceEntity> topicEntities = topicPerformanceRepository.findByUserIdOrderByAccuracyPercentageDesc(userId);

        if (topicEntities.isEmpty()) {
            return new UserTopicPerformanceResponse(
                    List.of(),
                    List.of(),
                    List.of(),
                    "Attempt more tests to unlock AI-powered topic insights and weakness analysis."
            );
        }

        /*
         * Map topic entities to the API contract, normalizing accuracy.
         * Counts and attempts remain integers.
         */
        List<UserTopicPerformanceResponse.TopicPerformanceDto> topics = topicEntities.stream()
                .map(topic -> new UserTopicPerformanceResponse.TopicPerformanceDto(
                        topic.getTopic(),
                        topic.getTotalQuestions(),
                        topic.getCorrectCount(),
                        topic.getIncorrectCount(),
                        topic.getUnattemptedCount(),
                        round(topic.getAccuracyPercentage()),
                        topic.getAttemptsCount(),
                        null
                ))
                .toList();

        /*
         * Strengths are topics with accuracy >= 70%.
         * Classification is performed against the original accuracy value.
         */
        List<String> strengths = topicEntities.stream()
                .filter(topic -> topic.getAccuracyPercentage() != null && topic.getAccuracyPercentage().compareTo(STRENGTH_THRESHOLD) >= 0)
                .map(topic -> topic.getTopic() + " (" + formatPercentage(topic.getAccuracyPercentage()) + "%)")
                .limit(5)
                .toList();

        /*
         * Weaknesses are topics with accuracy < 60%.
         * Classification is performed against the original accuracy value.
         */
        List<String> weaknesses = topicEntities.stream()
                .filter(topic -> topic.getAccuracyPercentage() != null && topic.getAccuracyPercentage().compareTo(WEAKNESS_THRESHOLD) < 0)
                .sorted(Comparator.comparing(UserTopicPerformanceEntity::getAccuracyPercentage, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(topic -> topic.getTopic() + " (" + formatPercentage(topic.getAccuracyPercentage()) + "%)")
                .limit(5)
                .toList();

        /*
         * Build the existing dynamic AI insight without changing the
         * response contract or the topic selection logic.
         */
        String aiInsight;

        if (!strengths.isEmpty() && !weaknesses.isEmpty()) {
            String topStrong = topicEntities.stream()
                    .filter(topic -> topic.getAccuracyPercentage() != null && topic.getAccuracyPercentage().compareTo(STRENGTH_THRESHOLD) >= 0)
                    .map(UserTopicPerformanceEntity::getTopic)
                    .limit(2)
                    .collect(Collectors.joining(" and "));

            String topWeak = topicEntities.stream()
                    .filter(topic -> topic.getAccuracyPercentage() != null && topic.getAccuracyPercentage().compareTo(WEAKNESS_THRESHOLD) < 0)
                    .sorted(Comparator.comparing(UserTopicPerformanceEntity::getAccuracyPercentage, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(UserTopicPerformanceEntity::getTopic)
                    .limit(2)
                    .collect(Collectors.joining(" and "));

            aiInsight = "You perform well in " + topStrong + ". Focus more on " + topWeak + " to improve your overall score.";

        } else if (!weaknesses.isEmpty()) {
            String topWeak = topicEntities.stream()
                    .filter(topic -> topic.getAccuracyPercentage() != null)
                    .sorted(Comparator.comparing(UserTopicPerformanceEntity::getAccuracyPercentage))
                    .map(UserTopicPerformanceEntity::getTopic)
                    .limit(2)
                    .collect(Collectors.joining(" and "));

            aiInsight = "Focus your revision on " + topWeak + " to boost your accuracy.";

        } else if (!strengths.isEmpty()) {
            String topStrong = topicEntities.stream()
                    .filter(topic -> topic.getAccuracyPercentage() != null)
                    .map(UserTopicPerformanceEntity::getTopic)
                    .limit(2)
                    .collect(Collectors.joining(" and "));

            aiInsight = "Great job! You have strong grasp of " + topStrong + ". Keep practicing to maintain your edge.";

        } else {
            aiInsight = "Keep practicing across multiple topics to build accuracy and confidence.";
        }

        return new UserTopicPerformanceResponse(
                topics,
                strengths,
                weaknesses,
                aiInsight
        );
    }

    /**
     * Converts a persisted report into the attempt-level API response.
     *
     * Raw score, maximum score, accuracy, and percentile are rounded
     * before being exposed through the API.
     */
    private UserPerformanceResponse.AttemptPerformance toAttemptPerformance(TestReportEntity report) {
        return new UserPerformanceResponse.AttemptPerformance(
                report.getAttemptId(),
                report.getTestId(),
                report.getCategoryId(),
                report.getTestSeriesId(),
                round(report.getTotalScore()),
                round(report.getMaxScore()),
                calculatePercentage(report.getTotalScore(), report.getMaxScore()),
                round(report.getAccuracyPercentage()),
                report.getTimeTakenSeconds(),
                report.getRankPosition(),
                round(report.getPercentile()),
                report.getCreatedAt()
        );
    }

    /**
     * Builds the user's aggregate performance summary.
     *
     * Each returned decimal field has scale 2.
     */
    private UserPerformanceResponse.PerformanceSummary buildSummary(List<TestReportEntity> reports) {
        BigDecimal averageScore = average(reports.stream().map(TestReportEntity::getTotalScore).toList());

        BigDecimal bestScore = reports.stream()
                .map(TestReportEntity::getTotalScore)
                .filter(value -> value != null)
                .max(BigDecimal::compareTo)
                .map(this::round)
                .orElse(zero());

        BigDecimal averageScorePercentage = average(reports.stream().map(report -> calculatePercentage(report.getTotalScore(), report.getMaxScore())).toList());

        BigDecimal bestScorePercentage = reports.stream()
                .map(report -> calculatePercentage(report.getTotalScore(), report.getMaxScore()))
                .filter(value -> value != null)
                .max(BigDecimal::compareTo)
                .map(this::round)
                .orElse(zero());

        BigDecimal averageAccuracy = average(reports.stream().map(TestReportEntity::getAccuracyPercentage).toList());

        BigDecimal averageTime = average(reports.stream()
                .map(report -> report.getTimeTakenSeconds() == null ? null : BigDecimal.valueOf(report.getTimeTakenSeconds()))
                .toList());

        BigDecimal averagePercentile = average(reports.stream().map(TestReportEntity::getPercentile).toList());

        return new UserPerformanceResponse.PerformanceSummary(
                reports.size(),
                round(averageScore),
                round(bestScore),
                round(averageScorePercentage),
                round(bestScorePercentage),
                round(averageAccuracy),
                round(averageTime),
                round(averagePercentile)
        );
    }

    /**
     * Aggregates section performance across the user's completed attempts.
     *
     * Question counts and total time are integers and are not rounded.
     * Average accuracy and average time are decimal values and are rounded.
     */
    private List<UserPerformanceResponse.SectionPerformanceSummary> buildSectionPerformance(List<SectionPerformanceEntity> rows) {
        Map<String, List<SectionPerformanceEntity>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        SectionPerformanceEntity::getSectionId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<SectionPerformanceEntity> sectionRows = entry.getValue();
                    SectionPerformanceEntity first = sectionRows.get(0);

                    long totalQuestions = sectionRows.stream().mapToLong(row -> safeInt(row.getTotalQuestions())).sum();
                    long correct = sectionRows.stream().mapToLong(row -> safeInt(row.getCorrectCount())).sum();
                    long incorrect = sectionRows.stream().mapToLong(row -> safeInt(row.getIncorrectCount())).sum();
                    long unattempted = sectionRows.stream().mapToLong(row -> safeInt(row.getUnattemptedCount())).sum();
                    long totalTimeSpent = sectionRows.stream().mapToLong(row -> safeInt(row.getTimeSpentSeconds())).sum();

                    BigDecimal averageAccuracy = average(sectionRows.stream().map(SectionPerformanceEntity::getAccuracyPercentage).toList());

                    BigDecimal averageTimeSpent;
                    if (sectionRows.isEmpty()) {
                        averageTimeSpent = zero();
                    } else {
                        averageTimeSpent = BigDecimal.valueOf(totalTimeSpent)
                                .divide(BigDecimal.valueOf(sectionRows.size()), DECIMAL_SCALE, ROUNDING_MODE);
                    }

                    return new UserPerformanceResponse.SectionPerformanceSummary(
                            first.getSectionId(),
                            first.getSectionName(),
                            sectionRows.size(),
                            totalQuestions,
                            correct,
                            incorrect,
                            unattempted,
                            round(averageAccuracy),
                            totalTimeSpent,
                            round(averageTimeSpent)
                    );
                })
                .sorted(Comparator.comparing(
                        UserPerformanceResponse.SectionPerformanceSummary::averageAccuracyPercentage,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    /**
     * Calculates score percentage:
     *
     * (score / maxScore) * 100
     *
     * The returned percentage is rounded to two decimal places.
     */
    private BigDecimal calculatePercentage(BigDecimal score, BigDecimal maxScore) {
        if (score == null || maxScore == null || maxScore.compareTo(BigDecimal.ZERO) == 0) {
            return zero();
        }

        return score.multiply(ONE_HUNDRED)
                .divide(maxScore, DECIMAL_SCALE, ROUNDING_MODE)
                .setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculates the arithmetic mean of non-null values.
     *
     * Null values are ignored. When no values are available, 0.00 is
     * returned so the API response maintains a consistent numeric shape.
     */
    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> nonNullValues = values.stream()
                .filter(value -> value != null)
                .toList();

        if (nonNullValues.isEmpty()) {
            return zero();
        }

        BigDecimal total = nonNullValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(nonNullValues.size()), DECIMAL_SCALE, ROUNDING_MODE);
    }

    /**
     * Rounds a nullable BigDecimal to exactly two decimal places.
     *
     * BigDecimal preserves the decimal scale, so a result can retain
     * trailing zeros, for example 85.50.
     */
    private BigDecimal round(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }

    /**
     * Formats topic percentages used inside the strengths and weaknesses
     * strings. This deliberately retains two decimal places in the string.
     */
    private String formatPercentage(BigDecimal value) {
        if (value == null) {
            return zero().toPlainString();
        }

        return round(value).toPlainString();
    }

    /**
     * Converts null integer values to zero for aggregation.
     *
     * The returned value is a count/time integer, not a decimal metric.
     */
    private long safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Provides a consistently scaled zero value.
     */
    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }
}