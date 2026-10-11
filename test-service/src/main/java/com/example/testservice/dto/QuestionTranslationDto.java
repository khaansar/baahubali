package com.example.testservice.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;
import com.example.testservice.dto.QuestionImageDto;

import java.time.Instant;

@Data
public class QuestionTranslationDto {
    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Question text is required")
    private String questionText;

    private String optionsJson;

    private List<QuestionImageDto> questionImages;

    private Map<String, List<QuestionImageDto>> optionImages;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant updatedAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant deletedAt;
}
