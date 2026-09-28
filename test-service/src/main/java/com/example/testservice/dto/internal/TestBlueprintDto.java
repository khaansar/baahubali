package com.example.testservice.dto.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

public record TestBlueprintDto(
        UUID testId,
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
