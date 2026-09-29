package com.example.analyticsservice.contract;

public record ReportMetadata(
        String categoryId,
        String testSeriesId,
        String testId,
        String attemptId
) {
}
