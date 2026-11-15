package com.example.attemptservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PatchAttemptRequest {
    @NotBlank(message = "questionId is required")
    private String questionId;
    
    private String currentSectionId;
    
    private String selectedOption;

    /** Review flag of the question; null leaves the stored flag unchanged. */
    private Boolean markedForReview;
    
    @NotNull(message = "currentQuestionIndex is required")
    private Integer currentQuestionIndex;
    
    @NotNull(message = "version is required")
    private Long version;
}