package com.example.attemptservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptHistorySummary {
    private String attemptId;
    private String testId;
    private String status;
    private Double finalScore;
    private Instant startedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
