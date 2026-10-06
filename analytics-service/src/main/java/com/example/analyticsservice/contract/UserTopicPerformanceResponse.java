package com.example.analyticsservice.contract;

import java.math.BigDecimal;
import java.util.List;

public record UserTopicPerformanceResponse(
        List<TopicPerformanceDto> topics,
        List<String> strengths,
        List<String> weaknesses,
        String aiInsight
) {
    public record TopicPerformanceDto(
            String topic,
            Integer totalQuestions,
            Integer correct,
            Integer incorrect,
            Integer unattempted,
            BigDecimal accuracyPercentage,
            Integer attemptsCount,
            Long avgTimeSpentSeconds
    ) {
    }
}
