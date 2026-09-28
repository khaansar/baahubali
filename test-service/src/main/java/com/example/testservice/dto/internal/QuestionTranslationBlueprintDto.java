package com.example.testservice.dto.internal;

import java.time.Instant;

public record QuestionTranslationBlueprintDto(
        String language,
        String questionText,
        String optionsJson,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}
