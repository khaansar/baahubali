package com.example.analyticsservice.core.service;
import com.example.analyticsservice.core.exception.*;


import com.example.analyticsservice.core.entity.*;
import com.example.analyticsservice.core.repository.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.SectionAnalysisResult;
import com.example.analyticsservice.contract.SectionPerformanceResult;
import com.example.analyticsservice.core.entity.ReportStatus;
import com.example.analyticsservice.core.entity.SectionPerformanceEntity;
import com.example.analyticsservice.core.repository.SectionPerformanceRepository;
import com.example.analyticsservice.core.entity.TestLeaderboardSnapshotEntity;
import com.example.analyticsservice.core.repository.TestLeaderboardSnapshotRepository;
import com.example.analyticsservice.core.entity.TestReportEntity;
import com.example.analyticsservice.core.repository.TestReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Repository-mocked tests; no database. Mapping to real MySQL is covered by integration tests. */
class ReportPersistenceServiceTest {

    private TestReportRepository reports;
    private SectionPerformanceRepository sections;
    private TestLeaderboardSnapshotRepository snapshots;
    private ReportPersistenceService service;

    private final AttemptSubmittedEvent event = new AttemptSubmittedEvent(
            "event-1", "att-1", "user-1", "cat-1", "series-1", "test-1", 5420L, List.of(),
                Instant.parse("2026-01-01T00:00:00Z"));

    private final SectionAnalysisResult sectionResult = new SectionAnalysisResult(
            List.of(
                    new SectionPerformanceResult("s1", "Physics", 65.0, 100.0, 30, 20, 5, 5, 80.0, 1800L),
                    new SectionPerformanceResult("s2", "Chemistry", 70.5, 100.0, 30, 22, 3, 5, 88.0, 1620L)),
            185.5, 300.0, 82.5);

    private final PeerComparisonResult peer = new PeerComparisonResult(285.0, 112.4, 4800L, 42, 1500, 97.2);

    @BeforeEach
    void setUp() {
        reports = mock(TestReportRepository.class);
        sections = mock(SectionPerformanceRepository.class);
        snapshots = mock(TestLeaderboardSnapshotRepository.class);
        service = new ReportPersistenceService(reports, sections, snapshots);
        when(snapshots.findByAttemptId("att-1")).thenReturn(Optional.empty());
    }

    private TestReportEntity report(String status) {
        TestReportEntity r = new TestReportEntity();
        r.setId("rep-1");
        r.setAttemptId("att-1");
        r.setStatus(status);
        return r;
    }

    @Test
    void claimCreatesProcessingReport() {
        when(reports.findByAttemptIdForUpdate("att-1")).thenReturn(Optional.empty());

        assertThat(service.claim(event)).isTrue();

        ArgumentCaptor<TestReportEntity> captor = ArgumentCaptor.forClass(TestReportEntity.class);
        verify(reports).saveAndFlush(captor.capture());
        TestReportEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PROCESSING");
        assertThat(saved.getAttemptId()).isEqualTo("att-1");
        assertThat(saved.getCategoryId()).isEqualTo("cat-1");
        assertThat(saved.getTestSeriesId()).isEqualTo("series-1");
        assertThat(saved.getTimeTakenSeconds()).isEqualTo(5420);
        assertThat(saved.getId()).isNotBlank();
    }

    @Test
    void claimOfCompletedReportIsDuplicate() {
        when(reports.findByAttemptIdForUpdate("att-1")).thenReturn(Optional.of(report("COMPLETED")));

        assertThat(service.claim(event)).isFalse();

        verify(reports, never()).saveAndFlush(any());
        verify(reports, never()).save(any());
    }

    @Test
    void claimOfFailedReportRetriesAsProcessing() {
        TestReportEntity failed = report("FAILED");
        when(reports.findByAttemptIdForUpdate("att-1")).thenReturn(Optional.of(failed));

        assertThat(service.claim(event)).isTrue();

        assertThat(failed.getStatus()).isEqualTo("PROCESSING");
        verify(reports, never()).saveAndFlush(any());
    }

    @Test
    void completePersistsReportSectionsAndSnapshot() {
        TestReportEntity processing = report("PROCESSING");
        when(reports.findByAttemptIdForUpdate("att-1")).thenReturn(Optional.of(processing));

        service.complete(event, sectionResult, peer, "{\"metadata\":{}}");

        assertThat(processing.getStatus()).isEqualTo(ReportStatus.COMPLETED.name());
        assertThat(processing.getTotalScore()).isEqualByComparingTo("185.50");
        assertThat(processing.getMaxScore()).isEqualByComparingTo("300.00");
        assertThat(processing.getAccuracyPercentage()).isEqualByComparingTo("82.50");
        assertThat(processing.getRankPosition()).isEqualTo(42);
        assertThat(processing.getPercentile()).isEqualByComparingTo("97.20");
        assertThat(processing.getReportData()).isEqualTo("{\"metadata\":{}}");

        verify(sections).deleteByAttemptId("att-1");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SectionPerformanceEntity>> rows = ArgumentCaptor.forClass(List.class);
        verify(sections).saveAll(rows.capture());
        assertThat(rows.getValue()).hasSize(2);
        SectionPerformanceEntity first = rows.getValue().get(0);
        assertThat(first.getAttemptId()).isEqualTo("att-1");
        assertThat(first.getUserId()).isEqualTo("user-1");
        assertThat(first.getTestId()).isEqualTo("test-1");
        assertThat(first.getSectionId()).isEqualTo("s1");
        assertThat(first.getCorrectCount()).isEqualTo(20);
        assertThat(first.getIncorrectCount()).isEqualTo(5);
        assertThat(first.getUnattemptedCount()).isEqualTo(5);
        assertThat(first.getAccuracyPercentage()).isEqualByComparingTo("80.00");
        assertThat(first.getTimeSpentSeconds()).isEqualTo(1800);
        assertThat(first.getTopicName()).isNull();

        ArgumentCaptor<TestLeaderboardSnapshotEntity> snap = ArgumentCaptor.forClass(TestLeaderboardSnapshotEntity.class);
        verify(snapshots).save(snap.capture());
        assertThat(snap.getValue().getTestId()).isEqualTo("test-1");
        assertThat(snap.getValue().getAttemptId()).isEqualTo("att-1");
        assertThat(snap.getValue().getUserId()).isEqualTo("user-1");
        assertThat(snap.getValue().getScore()).isEqualByComparingTo("185.50");
        assertThat(snap.getValue().getTimeTakenSeconds()).isEqualTo(5420);
        assertThat(snap.getValue().getRankPosition()).isEqualTo(42);
    }

    @Test
    void completeUpdatesExistingSnapshotInsteadOfDuplicating() {
        TestReportEntity processing = report("PROCESSING");
        TestLeaderboardSnapshotEntity existing = new TestLeaderboardSnapshotEntity();
        existing.setId(7L);
        when(reports.findByAttemptIdForUpdate("att-1")).thenReturn(Optional.of(processing));
        when(snapshots.findByAttemptId("att-1")).thenReturn(Optional.of(existing));

        service.complete(event, sectionResult, peer, "{}");

        ArgumentCaptor<TestLeaderboardSnapshotEntity> snap = ArgumentCaptor.forClass(TestLeaderboardSnapshotEntity.class);
        verify(snapshots).save(snap.capture());
        assertThat(snap.getValue()).isSameAs(existing);
        assertThat(existing.getRankPosition()).isEqualTo(42);
    }

    @Test
    void completeOnAlreadyCompletedReportWritesNothing() {
        when(reports.findByAttemptIdForUpdate("att-1")).thenReturn(Optional.of(report("COMPLETED")));

        service.complete(event, sectionResult, peer, "{}");

        verify(reports, never()).save(any());
        verify(sections, never()).saveAll(any());
        verify(snapshots, never()).save(any());
    }

    @Test
    void markFailedSetsFailedButNeverDowngradesCompleted() {
        TestReportEntity processing = report("PROCESSING");
        when(reports.findByAttemptIdForUpdate("att-1")).thenReturn(Optional.of(processing));
        service.markFailed("att-1");
        assertThat(processing.getStatus()).isEqualTo("FAILED");

        TestReportEntity completed = report("COMPLETED");
        when(reports.findByAttemptIdForUpdate("att-1")).thenReturn(Optional.of(completed));
        service.markFailed("att-1");
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
    }
}