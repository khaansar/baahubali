package com.example.analyticsservice.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.ReportData;
import com.example.analyticsservice.contract.SectionAnalysisResult;
import com.example.analyticsservice.contract.SectionPerformanceResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportAssemblerTest {

    @Test
    void combinesEventAndEngineResultsWithoutRecalculating() {
        AttemptSubmittedEvent event = new AttemptSubmittedEvent(
                "att-1", "user-1", "cat-1", "series-1", "test-1", 5420L, List.of());
        SectionAnalysisResult sectionResult = new SectionAnalysisResult(
                List.of(new SectionPerformanceResult("s1", "Physics", 65.0, 100.0, 30, 20, 5, 5, 80.0, 1800L)),
                185.5, 300.0, 82.5);
        PeerComparisonResult peer = new PeerComparisonResult(285.0, 112.4, 4800L, 42, 1500, 97.2);

        ReportData report = new ReportAssembler().assemble(event, sectionResult, peer);

        assertThat(report.metadata().categoryId()).isEqualTo("cat-1");
        assertThat(report.metadata().testSeriesId()).isEqualTo("series-1");
        assertThat(report.metadata().testId()).isEqualTo("test-1");
        assertThat(report.metadata().attemptId()).isEqualTo("att-1");
        assertThat(report.summary().totalScore()).isEqualTo(185.5);
        assertThat(report.summary().maxScore()).isEqualTo(300.0);
        assertThat(report.summary().accuracy()).isEqualTo(82.5);
        assertThat(report.summary().rank()).isEqualTo(42);
        assertThat(report.summary().totalParticipants()).isEqualTo(1500);
        assertThat(report.summary().percentile()).isEqualTo(97.2);
        assertThat(report.summary().timeTakenSeconds()).isEqualTo(5420L);
        assertThat(report.sectionBreakdown()).hasSize(1);
        assertThat(report.sectionBreakdown().get(0).sectionName()).isEqualTo("Physics");
        assertThat(report.sectionBreakdown().get(0).correct()).isEqualTo(20);
        assertThat(report.peerComparison()).isSameAs(peer);
    }
}