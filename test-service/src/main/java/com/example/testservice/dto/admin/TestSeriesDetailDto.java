package com.example.testservice.dto.admin;

import com.example.testservice.entity.Status;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TestSeriesDetailDto(
        UUID id,
        String title,
        BigDecimal basePrice,
        Status status,
        UUID categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<MockTestSummaryDto> mockTests
) {
    public record MockTestSummaryDto(
            UUID testId,
            String title,
            Status status,
            Integer durationMinutes,
            BigDecimal totalMarks,
            boolean isFree,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {}
}
