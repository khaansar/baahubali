package com.example.analyticsservice.contract;

public record TopicAnswerPayload(
        String topic,
        Integer totalQuestions,
        Integer correct,
        Integer incorrect,
        Integer unattempted,
        Double accuracy,
        Long timeSpentSeconds
) {
}
