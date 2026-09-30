package com.example.testservice.dto.admin;

import com.example.testservice.entity.Status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TestSeriesListDto(
        UUID id,
        String slug,
        String title,
        BigDecimal basePrice,
        Status status,
        String categoryName,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}