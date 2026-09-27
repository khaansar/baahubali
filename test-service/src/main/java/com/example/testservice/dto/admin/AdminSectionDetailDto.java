package com.example.testservice.dto.admin;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminSectionDetailDto(
        UUID id,
        UUID testId,
        String title,
        Integer sequenceOrder,
        Integer durationMinutes,
        boolean shuffleQuestions,
        BigDecimal defaultNegativeMarks,
        int questionCount,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionMappingDto> questions
) {
    public record QuestionMappingDto(
            UUID questionId,
            Integer sequenceOrder,
            BigDecimal positiveMarksOverride,
            BigDecimal negativeMarksOverride
    ) {}
}
