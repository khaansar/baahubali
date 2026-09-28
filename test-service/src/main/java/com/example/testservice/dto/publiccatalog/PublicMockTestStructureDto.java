package com.example.testservice.dto.publiccatalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

public record PublicMockTestStructureDto(
        UUID testId,
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
