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
public class QuestionReviewDto {
    private String questionId;
    private String questionText;
    private String selectedOption;
    private String correctOption;
    private String explanation;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
