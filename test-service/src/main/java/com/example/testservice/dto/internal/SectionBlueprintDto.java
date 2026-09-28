package com.example.testservice.dto.internal;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

public record SectionBlueprintDto(
        UUID sectionId,
        String title,
        Integer sequenceOrder,
        Integer durationMinutes,
        boolean shuffleQuestions,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<QuestionBlueprintDto> questions
) {}
