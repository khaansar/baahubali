package com.example.testservice.dto.internal;

import com.example.testservice.dto.QuestionImageDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QuestionBlueprintDto(
        UUID questionId,
        Integer sequenceOrder,
        String questionType,
        String topic,
        List<QuestionTranslationBlueprintDto> translations,
        Map<String, Object> correctAnswer,
        BigDecimal positiveMarks,
        BigDecimal negativeMarks,
        String explanation,
        List<QuestionImageDto> explanationImages,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}