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
public class PatchAttemptResponse {
    private boolean success;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
