package com.example.analyticsservice.core.service;

import com.example.analyticsservice.core.exception.*;

import com.example.analyticsservice.core.entity.*;
import com.example.analyticsservice.core.repository.*;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.SectionAnalysisResult;
import com.example.analyticsservice.core.entity.ReportStatus;
import com.example.analyticsservice.core.entity.SectionPerformanceEntity;
import com.example.analyticsservice.core.repository.SectionPerformanceRepository;
import com.example.analyticsservice.core.entity.TestLeaderboardSnapshotEntity;
import com.example.analyticsservice.core.repository.TestLeaderboardSnapshotRepository;
import com.example.analyticsservice.core.entity.TestReportEntity;
import com.example.analyticsservice.core.repository.TestReportRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns all database writes for the orchestration pipeline. Each public method is one short
 * transaction; no engine/network calls happen inside them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportPersistenceService {

    private final TestReportRepository reports;
    private final SectionPerformanceRepository sections;
    private final TestLeaderboardSnapshotRepository snapshots;

    /**
     * Claims the attempt for processing.
     *
     * @return true if the attempt should be processed (new, or a previous PROCESSING/FAILED
     *         report being retried); false if a COMPLETED report already exists (duplicate).
     * @throws org.springframework.dao.DataIntegrityViolationException if a concurrent insert
     *         won the race on attempt_id; the caller re-evaluates by calling again.
     */
    @Transactional
    public boolean claim(AttemptSubmittedEvent event) {
        Optional<TestReportEntity> existing = reports.findByAttemptId(event.attemptId());
        if (existing.isPresent()) {
            TestReportEntity report = existing.get();
            if (ReportStatus.COMPLETED.name().equals(report.getStatus())) {
                log.info("Duplicate event for completed report, attemptId={}", event.attemptId());
                return false;
            }
            if (ReportStatus.FAILED.name().equals(report.getStatus())) {
                report.setStatus(ReportStatus.PROCESSING.name());
                reports.save(report);
            }
            log.info("Reprocessing existing {} report, attemptId={}", report.getStatus(), event.attemptId());
            return true;
        }

        TestReportEntity report = new TestReportEntity();
        report.setId(UUID.randomUUID().toString());
        report.setAttemptId(event.attemptId());
        report.setUserId(event.userId());
        report.setCategoryId(event.categoryId());
        report.setTestSeriesId(event.testSeriesId());
        report.setTestId(event.testId());
        report.setTotalScore(scale2(0.0));
        report.setMaxScore(scale2(0.0));
        report.setAccuracyPercentage(scale2(0.0));
        report.setTimeTakenSeconds(toInt(event.timeTakenSeconds()));
        report.setStatus(ReportStatus.PROCESSING.name());
        reports.saveAndFlush(report);
        return true;
    }

    /** Writes report, section rows and leaderboard snapshot atomically and marks COMPLETED. */
    @Transactional
    public void complete(AttemptSubmittedEvent event,
                         SectionAnalysisResult sectionResult,
                         PeerComparisonResult peer,
                         String reportJson) {
        String attemptId = event.attemptId();
        TestReportEntity report = reports.findByAttemptIdForUpdate(attemptId)
                .orElseThrow(() -> new IllegalStateException("No claimed report for attemptId=" + attemptId));

        if (ReportStatus.COMPLETED.name().equals(report.getStatus())) {
            log.info("Report already completed by another delivery, attemptId={}", attemptId);
            return;
        }

        report.setTotalScore(scale2(sectionResult.totalScore()));
        report.setMaxScore(scale2(sectionResult.maxScore()));
        report.setAccuracyPercentage(scale2(sectionResult.accuracyPercentage()));
        report.setTimeTakenSeconds(toInt(event.timeTakenSeconds()));
        report.setRankPosition(peer.rank());
        report.setPercentile(scale2(peer.percentile()));
        report.setReportData(reportJson);
        report.setStatus(ReportStatus.COMPLETED.name());
        reports.save(report);

        sections.deleteByAttemptId(attemptId);
        List<SectionPerformanceEntity> rows = sectionResult.sections().stream()
                .map(s -> {
                    SectionPerformanceEntity row = new SectionPerformanceEntity();
                    row.setAttemptId(attemptId);
                    row.setUserId(event.userId());
                    row.setTestId(event.testId());
                    row.setSectionId(s.sectionId());
                    row.setSectionName(s.sectionName());
                    row.setTotalQuestions(s.totalQuestions());
                    row.setCorrectCount(s.correct());
                    row.setIncorrectCount(s.incorrect());
                    row.setUnattemptedCount(s.unattempted());
                    row.setAccuracyPercentage(scale2(s.accuracy()));
                    row.setTimeSpentSeconds(toInt(s.timeSpentSeconds()));
                    return row;
                })
                .toList();
        sections.saveAll(rows);

        TestLeaderboardSnapshotEntity snapshot = snapshots.findByAttemptId(attemptId)
                .orElseGet(TestLeaderboardSnapshotEntity::new);
        snapshot.setTestId(event.testId());
        snapshot.setAttemptId(attemptId);
        snapshot.setUserId(event.userId());
        snapshot.setScore(scale2(sectionResult.totalScore()));
        snapshot.setTimeTakenSeconds(toInt(event.timeTakenSeconds()));
        snapshot.setRankPosition(peer.rank());
        snapshots.save(snapshot);
    }

    /** Marks a non-completed report FAILED. Never downgrades a COMPLETED report. */
    @Transactional
    public void markFailed(String attemptId) {
        reports.findByAttemptId(attemptId).ifPresent(report -> {
            if (!ReportStatus.COMPLETED.name().equals(report.getStatus())) {
                report.setStatus(ReportStatus.FAILED.name());
                reports.save(report);
            }
        });
    }

    private static BigDecimal scale2(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer toInt(Long value) {
        return value == null ? null : Math.toIntExact(value);
    }
}