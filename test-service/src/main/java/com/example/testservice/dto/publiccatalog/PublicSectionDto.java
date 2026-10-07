package com.example.testservice.dto.publiccatalog;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

public record PublicSectionDto(
        UUID sectionId,
        String title,
        Integer sequenceOrder,
        Integer durationMinutes,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        List<PublicQuestionDto> questions
) {}
