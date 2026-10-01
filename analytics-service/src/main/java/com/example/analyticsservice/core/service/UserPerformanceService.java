package com.example.analyticsservice.core.service;

import com.example.analyticsservice.contract.UserPerformanceResponse;
import com.example.analyticsservice.core.entity.ReportStatus;
import com.example.analyticsservice.core.entity.SectionPerformanceEntity;
import com.example.analyticsservice.core.entity.TestReportEntity;
import com.example.analyticsservice.core.repository.SectionPerformanceRepository;
import com.example.analyticsservice.core.repository.TestReportRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

    private final TestReportRepository testReportRepository;
    private final SectionPerformanceRepository sectionPerformanceRepository;

    @Transactional(readOnly = true)
    public UserPerformanceResponse getPerformance(String userId) {
        List<TestReportEntity> reports =
                testReportRepository.findByUserIdAndStatusOrderByCreatedAtAsc(
                        userId,
                        ReportStatus.COMPLETED.name()
                );

        if (reports.isEmpty()) {
            return new UserPerformanceResponse(
                    new UserPerformanceResponse.PerformanceSummary(
                            0,
                            zero(),
                            zero(),
                            zero(),
                            zero(),
                            zero(),
                            zero(),
                            zero()
                    ),
                    List.of(),
                    List.of()
            );
        }

        List<UserPerformanceResponse.AttemptPerformance> attempts =
                reports.stream()
                        .map(this::toAttemptPerformance)
                        .toList();

        UserPerformanceResponse.PerformanceSummary summary =
                buildSummary(reports);

        Set<String> completedAttemptIds =
                reports.stream()
                        .map(TestReportEntity::getAttemptId)
                        .collect(Collectors.toSet());

        List<SectionPerformanceEntity> sectionRows =
                sectionPerformanceRepository.findByUserId(userId)
                        .stream()
                        .filter(section -> completedAttemptIds.contains(section.getAttemptId()))
                        .toList();

        List<UserPerformanceResponse.SectionPerformanceSummary> sectionPerformance =
                buildSectionPerformance(sectionRows);

        return new UserPerformanceResponse(
                summary,
                attempts,
                sectionPerformance
        );
    }

    private UserPerformanceResponse.AttemptPerformance toAttemptPerformance(
            TestReportEntity report) {

        return new UserPerformanceResponse.AttemptPerformance(
                report.getAttemptId(),
                report.getTestId(),
                report.getCategoryId(),
                report.getTestSeriesId(),
                report.getTotalScore(),
                report.getMaxScore(),
                calculatePercentage(
                        report.getTotalScore(),
                        report.getMaxScore()
                ),
                report.getAccuracyPercentage(),
                report.getTimeTakenSeconds(),
                report.getRankPosition(),
                report.getPercentile(),
                report.getCreatedAt()
        );
    }

    private UserPerformanceResponse.PerformanceSummary buildSummary(
            List<TestReportEntity> reports) {

        BigDecimal averageScore =
                average(reports.stream()
                        .map(TestReportEntity::getTotalScore)
                        .toList());

        BigDecimal bestScore =
                reports.stream()
                        .map(TestReportEntity::getTotalScore)
                        .filter(value -> value != null)
                        .max(BigDecimal::compareTo)
                        .orElse(zero());

        BigDecimal averageScorePercentage =
                average(reports.stream()
                        .map(report -> calculatePercentage(
                                report.getTotalScore(),
                                report.getMaxScore()
                        ))
                        .toList());

        BigDecimal bestScorePercentage =
                reports.stream()
                        .map(report -> calculatePercentage(
                                report.getTotalScore(),
                                report.getMaxScore()
                        ))
                        .max(BigDecimal::compareTo)
                        .orElse(zero());

        BigDecimal averageAccuracy =
                average(reports.stream()
                        .map(TestReportEntity::getAccuracyPercentage)
                        .toList());

        BigDecimal averageTime =
                average(reports.stream()
                        .map(report -> report.getTimeTakenSeconds() == null
                                ? null
                                : BigDecimal.valueOf(report.getTimeTakenSeconds()))
                        .toList());

        BigDecimal averagePercentile =
                average(reports.stream()
                        .map(TestReportEntity::getPercentile)
                        .toList());

        return new UserPerformanceResponse.PerformanceSummary(
                reports.size(),
                averageScore,
                bestScore,
                averageScorePercentage,
                bestScorePercentage,
                averageAccuracy,
                averageTime,
                averagePercentile
        );
    }

    private List<UserPerformanceResponse.SectionPerformanceSummary> buildSectionPerformance(
            List<SectionPerformanceEntity> rows) {

        Map<String, List<SectionPerformanceEntity>> grouped =
                rows.stream()
                        .collect(Collectors.groupingBy(
                                SectionPerformanceEntity::getSectionId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        return grouped.entrySet()
                .stream()
                .map(entry -> {
                    List<SectionPerformanceEntity> sectionRows = entry.getValue();

                    SectionPerformanceEntity first = sectionRows.get(0);

                    long totalQuestions =
                            sectionRows.stream()
                                    .mapToLong(row -> safeInt(row.getTotalQuestions()))
                                    .sum();

                    long correct =
                            sectionRows.stream()
                                    .mapToLong(row -> safeInt(row.getCorrectCount()))
                                    .sum();

                    long incorrect =
                            sectionRows.stream()
                                    .mapToLong(row -> safeInt(row.getIncorrectCount()))
                                    .sum();

                    long unattempted =
                            sectionRows.stream()
                                    .mapToLong(row -> safeInt(row.getUnattemptedCount()))
                                    .sum();

                    long totalTimeSpent =
                            sectionRows.stream()
                                    .mapToLong(row -> safeInt(row.getTimeSpentSeconds()))
                                    .sum();

                    BigDecimal averageAccuracy =
                            average(sectionRows.stream()
                                    .map(SectionPerformanceEntity::getAccuracyPercentage)
                                    .toList());

                    BigDecimal averageTimeSpent =
                            sectionRows.isEmpty()
                                    ? zero()
                                    : BigDecimal.valueOf(totalTimeSpent)
                                            .divide(
                                                    BigDecimal.valueOf(sectionRows.size()),
                                                    2,
                                                    RoundingMode.HALF_UP
                                            );

                    return new UserPerformanceResponse.SectionPerformanceSummary(
                            first.getSectionId(),
                            first.getSectionName(),
                            sectionRows.size(),
                            totalQuestions,
                            correct,
                            incorrect,
                            unattempted,
                            averageAccuracy,
                            totalTimeSpent,
                            averageTimeSpent
                    );
                })
                .sorted(
                        Comparator.comparing(
                                UserPerformanceResponse.SectionPerformanceSummary
                                        ::averageAccuracyPercentage
                        ).reversed()
                )
                .toList();
    }

    private BigDecimal calculatePercentage(
            BigDecimal score,
            BigDecimal maxScore) {

        if (score == null
                || maxScore == null
                || maxScore.compareTo(BigDecimal.ZERO) == 0) {
            return zero();
        }

        return score
                .multiply(BigDecimal.valueOf(100))
                .divide(maxScore, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> nonNullValues =
                values.stream()
                        .filter(value -> value != null)
                        .toList();

        if (nonNullValues.isEmpty()) {
            return zero();
        }

        BigDecimal total =
                nonNullValues.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(
                BigDecimal.valueOf(nonNullValues.size()),
                2,
                RoundingMode.HALF_UP
        );
    }

    private long safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}