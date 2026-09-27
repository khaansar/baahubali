package com.example.testservice.dto.admin;

public record SectionCreateDto(
        String title,
        Integer durationMinutes,
        boolean shuffleQuestions,
        java.math.BigDecimal defaultNegativeMarks
) {}