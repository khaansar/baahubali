package com.example.testservice.dto.admin;

import com.example.testservice.dto.QuestionTranslationDto;
import com.example.testservice.entity.Difficulty;
import com.example.testservice.entity.QuestionType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionDetailDto(
        UUID id,
        QuestionType questionType,
        List<QuestionTranslationDto> translations,
        Map<String, Object> correctAnswerJson,
        BigDecimal positiveMarks,
        BigDecimal negativeMarks,
        String explanation,
        String topic,
        Difficulty difficulty,
        boolean isLocked,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        String createdBy,
        String warning
) {}
