package com.example.testservice.dto.publiccatalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

public record PublicTestSeriesDetailDto(
        UUID id,
        String title,
        BigDecimal basePrice,
        UUID categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<PublicMockTestSummaryDto> mockTests
) {
    public record PublicMockTestSummaryDto(
            UUID testId,
            String title,
            Integer durationMinutes,
            BigDecimal totalMarks,
            boolean isFree,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {}
}
