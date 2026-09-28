package com.example.attemptservice.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalTestBlueprintDto {
    private String testId;
    private String title;
    private Integer durationMinutes;
    private String instructions;
    private BigDecimal totalMarks;
    private Boolean free;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private List<InternalSectionDto> sections;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InternalSectionDto {
        private String sectionId;
        private String title;
        private Integer sequenceOrder;
        private Integer durationMinutes;
        private Boolean shuffleQuestions;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant deletedAt;
        private List<InternalQuestionDto> questions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InternalQuestionDto {
        private String questionId;
        private Integer sequenceOrder;
        private String questionType;
        private List<InternalTranslationDto> translations;
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private Map<String, Object> correctAnswer;
        private BigDecimal positiveMarks;
        private BigDecimal negativeMarks;
        private String explanation;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant deletedAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InternalTranslationDto {
        private String language;
        private String questionText;
        private String optionsJson;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant deletedAt;
    }
}
