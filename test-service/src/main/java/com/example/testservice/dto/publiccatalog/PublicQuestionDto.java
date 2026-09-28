package com.example.testservice.dto.publiccatalog;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

public record PublicQuestionDto(
        UUID questionId,
        Integer sequenceOrder,
        String questionType,
        String questionText,
        Map<String, Object> optionsJson,
        BigDecimal positiveMarks,
        BigDecimal negativeMarks,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}
