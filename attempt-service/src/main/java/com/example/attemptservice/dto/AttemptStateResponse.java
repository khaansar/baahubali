package com.example.attemptservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.time.Instant;

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
    private Map<String, String> answers;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
