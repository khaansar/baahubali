package com.example.testservice.dto.admin;

import com.example.testservice.entity.Status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminMockTestDetailDto(
        UUID id,
        UUID seriesId,
        String title,
        Integer durationMinutes,
        Status status,
        BigDecimal totalMarks,
        boolean isSectionOrderStrict,
        boolean shuffleSections,
        boolean negativeMarkingEnabled,
        String instructions,
        boolean isFree,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<SectionSummaryDto> sections
) {
    public record SectionSummaryDto(
            UUID id,
            String title,
            Integer sequenceOrder,
            Integer durationMinutes,
            boolean shuffleQuestions,
            int questionCount,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {}
}
