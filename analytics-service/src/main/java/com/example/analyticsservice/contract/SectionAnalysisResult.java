package com.example.analyticsservice.contract;

import java.util.List;

public record SectionAnalysisResult(
        List<SectionPerformanceResult> sections,
        Double totalScore,
        Double maxScore,
        Double accuracyPercentage
) {
}
