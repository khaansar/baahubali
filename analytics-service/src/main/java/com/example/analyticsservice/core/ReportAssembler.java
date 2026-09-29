package com.example.analyticsservice.core;

import com.example.analyticsservice.contract.AttemptSubmittedEvent;
import com.example.analyticsservice.contract.PeerComparisonResult;
import com.example.analyticsservice.contract.ReportData;
import com.example.analyticsservice.contract.ReportMetadata;
import com.example.analyticsservice.contract.ReportSummary;
import com.example.analyticsservice.contract.SectionAnalysisResult;
import com.example.analyticsservice.contract.SectionReport;
import java.util.List;
import org.springframework.stereotype.Component;

/** Pure mapping of event + engine results into ReportData. Performs no calculations. */
@Component
public class ReportAssembler {

    public ReportData assemble(AttemptSubmittedEvent event,
                               SectionAnalysisResult sectionResult,
                               PeerComparisonResult peer) {
        ReportMetadata metadata = new ReportMetadata(
                event.categoryId(), event.testSeriesId(), event.testId(), event.attemptId());

        ReportSummary summary = new ReportSummary(
                sectionResult.totalScore(),
                sectionResult.maxScore(),
                peer.rank(),
                peer.totalParticipants(),
                peer.percentile(),
                sectionResult.accuracyPercentage(),
                event.timeTakenSeconds());

        List<SectionReport> breakdown = sectionResult.sections().stream()
                .map(s -> new SectionReport(
                        s.sectionId(), s.sectionName(), s.score(), s.maxScore(),
                        s.totalQuestions(), s.correct(), s.incorrect(), s.unattempted(),
                        s.accuracy(), s.timeSpentSeconds()))
                .toList();

        return new ReportData(metadata, summary, breakdown, peer);
    }
}