package com.example.attemptservice.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAttemptResponse {
    private String attemptId;
    private String status;
    private Double finalScore;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
