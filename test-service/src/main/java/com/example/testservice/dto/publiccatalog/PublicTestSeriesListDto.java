package com.example.testservice.dto.publiccatalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PublicTestSeriesListDto(
        UUID id,
        String slug,
        String title,
        BigDecimal basePrice,
        String categoryName,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}