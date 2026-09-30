package com.example.analyticsservice.contract;

import java.util.List;

public record ReportData(
        ReportMetadata metadata,
        ReportSummary summary,
        List<SectionReport> sectionBreakdown,
        PeerComparisonResult peerComparison
) {
}
