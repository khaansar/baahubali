package com.example.testservice.dto.admin;

import com.example.testservice.entity.Difficulty;
import com.example.testservice.entity.QuestionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuestionListDto(
        UUID id,
        QuestionType questionType,
        String shortText, // Truncated version of the question
        BigDecimal positiveMarks,
        Difficulty difficulty,
        boolean isLocked,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}
