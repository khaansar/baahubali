package com.example.testservice.dto.internal;

import com.example.testservice.dto.QuestionImageDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QuestionTranslationBlueprintDto(
        String language,
        String questionText,
        String optionsJson,
        List<QuestionImageDto> questionImages,
        Map<String, List<QuestionImageDto>> optionImages,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}