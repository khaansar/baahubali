package com.example.testservice.dto.publiccatalog;

import com.example.testservice.dto.QuestionImageDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PublicQuestionDto(
        UUID questionId,
        Integer sequenceOrder,
        String questionType,
        String questionText,
        Map<String, Object> optionsJson,
        List<QuestionImageDto> questionImages,
        Map<String, List<QuestionImageDto>> optionImages,
        BigDecimal positiveMarks,
        BigDecimal negativeMarks,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {}