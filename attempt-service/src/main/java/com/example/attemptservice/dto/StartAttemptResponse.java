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
public class StartAttemptResponse {
    private String attemptId;
    private Instant deadline;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}