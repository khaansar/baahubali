package com.example.testservice.dto.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

public record QuestionBlueprintDto(
        UUID questionId,
        Integer sequenceOrder,
        String questionType,
        List<QuestionTranslationBlueprintDto> translations,
        Map<String, Object> correctAnswer,
        BigDecimal positiveMarks,
        BigDecimal negativeMarks,
        String explanation,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}
