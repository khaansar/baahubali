package com.example.attemptservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptReviewResponse {
    private String attemptId;
    private Double finalScore;
    private List<QuestionReviewDto> questions;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
