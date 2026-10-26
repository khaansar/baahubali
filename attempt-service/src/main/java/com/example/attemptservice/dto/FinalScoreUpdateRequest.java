package com.example.attemptservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Internal payload sent by analytics once an attempt has been evaluated. */
@Getter
@Setter
@NoArgsConstructor
public class FinalScoreUpdateRequest {

    @NotNull(message = "finalScore is required")
    private Double finalScore;
}
