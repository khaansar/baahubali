package com.example.testservice.dto.publiccatalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicMockTestStructureDto(
        UUID testId,
        String slug,
        String title,
        Integer durationMinutes,
        String instructions,
        BigDecimal totalMarks,
        boolean isFree,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<PublicSectionDto> sections
) {}