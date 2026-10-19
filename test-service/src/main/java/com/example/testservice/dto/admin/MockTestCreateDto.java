package com.example.testservice.dto.admin;

import java.time.Instant;

public record MockTestCreateDto(
        String title,
        Integer durationMinutes,
        boolean isSectionOrderStrict,
        boolean shuffleSections,
        boolean negativeMarkingEnabled,
        String instructions,
        boolean isFree,
        String accessCode,
        Instant windowStartTime,
        Instant windowEndTime
) {}