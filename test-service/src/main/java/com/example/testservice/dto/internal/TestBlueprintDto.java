package com.example.testservice.dto.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TestBlueprintDto(
        UUID testId,
        UUID categoryId,
        UUID testSeriesId,
        String title,
        Integer durationMinutes,
        String instructions,
        boolean free,
        boolean isSectionOrderStrict,
        boolean shuffleSections,
        boolean negativeMarkingEnabled,
        BigDecimal totalMarks,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<SectionBlueprintDto> sections
) {}