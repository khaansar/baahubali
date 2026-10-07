package com.example.attemptservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptStateResponse {
    private String attemptId;
    private String userId;
    private String testId;
    private String status;
    private Integer currentQuestionIndex;
    private String currentSectionId;
    private Map<String, Integer> sectionTimeSpentSec;
    private Instant currentSectionStartedAt;
    private Map<String, String> answers;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}