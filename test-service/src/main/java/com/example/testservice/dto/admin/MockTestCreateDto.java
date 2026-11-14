package com.example.testservice.dto.admin;

public record MockTestCreateDto(
        String title,
        Integer durationMinutes,
        boolean isSectionOrderStrict,
        boolean shuffleSections,
        boolean negativeMarkingEnabled,
        String instructions
) {}