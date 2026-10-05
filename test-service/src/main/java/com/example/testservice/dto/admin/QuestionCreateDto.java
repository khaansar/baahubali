package com.example.testservice.dto.admin;

import com.example.testservice.dto.QuestionTranslationDto;
import com.example.testservice.entity.Difficulty;
import com.example.testservice.entity.QuestionType;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record QuestionCreateDto(
        QuestionType questionType,
        List<@Valid QuestionTranslationDto> translations,
        Map<String, Object> correctAnswerJson,
        BigDecimal positiveMarks,
        BigDecimal negativeMarks,
        String explanation,
        String topic,
        Difficulty difficulty
) {}
