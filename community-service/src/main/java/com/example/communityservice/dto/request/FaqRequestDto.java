package com.example.communityservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FaqRequestDto(

        String targetId,

        @NotBlank(message = "Question cannot be blank")
        @Size(max = 2000, message = "Question cannot exceed 2000 characters")
        String question,

        @NotBlank(message = "Answer cannot be blank")
        @Size(max = 10000, message = "Answer cannot exceed 10000 characters")
        String answer,

        @NotNull(message = "Display order is required")
        Integer displayOrder
) {}
