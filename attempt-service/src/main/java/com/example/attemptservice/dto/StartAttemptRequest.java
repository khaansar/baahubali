package com.example.attemptservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StartAttemptRequest {
    private String userId;
    
    @NotBlank(message = "testId is required")
    private String testId;
    
}
