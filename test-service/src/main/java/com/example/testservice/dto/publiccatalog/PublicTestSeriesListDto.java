package com.example.testservice.dto.publiccatalog;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.Instant;

public record PublicTestSeriesListDto(
        UUID id,
        String title,
        BigDecimal basePrice,
        String categoryName,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}
