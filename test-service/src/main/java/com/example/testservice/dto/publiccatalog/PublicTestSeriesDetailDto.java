package com.example.testservice.dto.publiccatalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicTestSeriesDetailDto(
        UUID id,
        String slug,
        String title,
        BigDecimal basePrice,
        UUID categoryId,
        String categorySlug,
        String categoryName,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<PublicMockTestSummaryDto> mockTests
) {
    public record PublicMockTestSummaryDto(
            UUID testId,
            String slug,
            String title,
            Integer durationMinutes,
            BigDecimal totalMarks,
            boolean isFree,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {}
}