package com.example.attemptservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InProgressAttemptDto {
    private String id;
    private String testId;
    private String testTitle;
    private Instant startedAt;
    private Integer progressPercentage;
}
