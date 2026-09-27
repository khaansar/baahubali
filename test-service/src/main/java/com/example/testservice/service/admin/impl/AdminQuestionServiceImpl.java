package com.example.testservice.service.admin.impl;

import com.example.testservice.dto.QuestionTranslationDto;
import com.example.testservice.dto.admin.*;
import com.example.testservice.dto.common.PageMetaDto;
import com.example.testservice.dto.common.PaginatedResponseDto;
import com.example.testservice.entity.Question;
import com.example.testservice.entity.QuestionTranslation;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.exception.ValidationException;
import com.example.testservice.repository.QuestionRepository;
import com.example.testservice.repository.specification.QuestionSpecifications;
import com.example.testservice.service.MathValidationUtil;
import com.example.testservice.validation.QuestionValidationUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminQuestionServiceImpl {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID createQuestion(QuestionCreateDto dto, String adminId) {
        if (dto.translations() == null || dto.translations().isEmpty()) {
            throw new ValidationException("At least one translation is required.");
        }

        Map<String, Object> validationOptions = extractOptionsForValidation(dto.translations());

        if (!QuestionValidationUtil.isValidAnswerSchema(dto.questionType(), dto.correctAnswerJson(), validationOptions)) {
            throw new ValidationException("Invalid answer schema or missing options for question type: " + dto.questionType());
        }

        Question question = new Question();
        question.setQuestionType(dto.questionType());
        question.setCorrectAnswerJson(dto.correctAnswerJson());
        question.setPositiveMarks(dto.positiveMarks());
        question.setNegativeMarks(dto.negativeMarks());
        question.setExplanation(dto.explanation());
        question.setDifficulty(dto.difficulty());
        question.setCreatedBy(adminId);
        question.setUpdatedBy(adminId);
        question.setLocked(false);

        List<QuestionTranslation> translations = dto.translations().stream().map(t -> {
            MathValidationUtil.validateBalancedMathDelimiters(t.getQuestionText());
            if (t.getOptionsJson() != null) {
                MathValidationUtil.validateBalancedMathDelimiters(t.getOptionsJson());
            }

            QuestionTranslation qt = new QuestionTranslation();
            qt.setQuestion(question);
            qt.setLanguage(t.getLanguage());
            qt.setQuestionText(t.getQuestionText());
            qt.setOptionsJson(t.getOptionsJson());
            qt.setCreatedBy(adminId);
            qt.setUpdatedBy(adminId);
            return qt;
        }).collect(Collectors.toList());

        question.setTranslations(translations);
        questionRepository.save(question);
        return question.getId();
    }

    @Transactional(readOnly = true)
    public PaginatedResponseDto<QuestionListDto> getQuestions(
            String search, String type, String difficulty, Boolean isLocked, Boolean unusedOnly, int page, int limit) {

        var spec = QuestionSpecifications.buildFilter(search, type, difficulty, isLocked, unusedOnly);
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, limit,
                org.springframework.data.domain.Sort.by("createdAt").descending());

        var questionPage = questionRepository.findAll(spec, pageable);

        List<QuestionListDto> data = questionPage.getContent().stream().map(q -> {
            String fullText = q.getTranslations().isEmpty() ? "No Translation" : q.getTranslations().get(0).getQuestionText();
            String shortText = fullText.length() > 60 ? fullText.substring(0, 57) + "..." : fullText;

            return new QuestionListDto(
                    q.getId(), q.getQuestionType(), shortText, q.getPositiveMarks(),
                    q.getDifficulty(), q.isLocked(), q.getCreatedAt()
            );
        }).toList();

        PageMetaDto meta = new PageMetaDto(
                page, limit, questionPage.getTotalElements(), questionPage.getTotalPages()
        );

        return new PaginatedResponseDto<>(data, meta);
    }

    @Transactional(readOnly = true)
    public QuestionDetailDto getQuestionById(UUID id) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        List<QuestionTranslationDto> translationDtos = q.getTranslations().stream().map(t -> {
            QuestionTranslationDto dto = new QuestionTranslationDto();
            dto.setLanguage(t.getLanguage());
            dto.setQuestionText(t.getQuestionText());
            dto.setOptionsJson(t.getOptionsJson());
            return dto;
        }).collect(Collectors.toList());

        return new QuestionDetailDto(
                q.getId(), q.getQuestionType(), translationDtos,
                q.getCorrectAnswerJson(), q.getPositiveMarks(), q.getNegativeMarks(),
                q.getExplanation(), q.getDifficulty(), q.isLocked(), q.getCreatedAt(), q.getCreatedBy(), null
        );
    }

    @Transactional
    public BulkImportResultDto bulkImportQuestions(List<QuestionCreateDto> rows, String adminId) {
        int imported = 0;
        int failed = 0;
        List<BulkImportResultDto.ImportError> errors = new ArrayList<>();
        List<java.util.UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            QuestionCreateDto row = rows.get(i);
            try {
                if (row.translations() == null || row.translations().isEmpty()) {
                    throw new ValidationException("At least one translation is required.");
                }

                Map<String, Object> validationOptions = extractOptionsForValidation(row.translations());

                if (!QuestionValidationUtil.isValidAnswerSchema(row.questionType(), row.correctAnswerJson(), validationOptions)) {
                    throw new ValidationException("Invalid answer schema or missing options.");
                }

                Question question = new Question();
                question.setQuestionType(row.questionType());
                question.setCorrectAnswerJson(row.correctAnswerJson());
                question.setPositiveMarks(row.positiveMarks());
                question.setNegativeMarks(row.negativeMarks());
                question.setExplanation(row.explanation());
                question.setDifficulty(row.difficulty());
                question.setCreatedBy(adminId);
                question.setUpdatedBy(adminId);
                question.setLocked(false);

                List<QuestionTranslation> translations = row.translations().stream().map(t -> {
                    MathValidationUtil.validateBalancedMathDelimiters(t.getQuestionText());
                    if (t.getOptionsJson() != null) {
                        MathValidationUtil.validateBalancedMathDelimiters(t.getOptionsJson());
                    }

                    QuestionTranslation qt = new QuestionTranslation();
                    qt.setQuestion(question);
                    qt.setLanguage(t.getLanguage());
                    qt.setQuestionText(t.getQuestionText());
                    qt.setOptionsJson(t.getOptionsJson());
                    qt.setCreatedBy(adminId);
                    qt.setUpdatedBy(adminId);
                    return qt;
                }).collect(Collectors.toList());

                question.setTranslations(translations);
                questionRepository.save(question);
                imported++;
                createdIds.add(question.getId());

            } catch (Exception e) {
                failed++;
                errors.add(new BulkImportResultDto.ImportError(i + 1, e.getMessage()));
            }
        }
        return new BulkImportResultDto(imported, failed, errors, createdIds);
    }

    @Transactional
    public QuestionDetailDto updateQuestion(UUID id, QuestionUpdateDto dto, String adminId) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (!question.getQuestionType().name().equals(dto.questionType().name())) {
            throw new ValidationException("Cannot change question_type. Delete and recreate the question instead.");
        }

        if (dto.translations() == null || dto.translations().isEmpty()) {
            throw new ValidationException("At least one translation is required.");
        }

        Map<String, Object> validationOptions = extractOptionsForValidation(dto.translations());

        if (!QuestionValidationUtil.isValidAnswerSchema(question.getQuestionType(), dto.correctAnswerJson(), validationOptions)) {
            throw new ValidationException("Invalid answer schema for question type " + question.getQuestionType());
        }

        question.setCorrectAnswerJson(dto.correctAnswerJson());
        question.setPositiveMarks(dto.positiveMarks());
        question.setNegativeMarks(dto.negativeMarks());
        question.setExplanation(dto.explanation());
        question.setDifficulty(dto.difficulty());
        question.setUpdatedBy(adminId);

        question.getTranslations().clear();
        // Delete the orphaned rows before inserting replacements. The database
        // has a unique constraint on (question_id, language), and without this
        // flush Hibernate may insert the replacement before deleting the old row.
        questionRepository.flush();
        List<QuestionTranslation> translations = dto.translations().stream().map(t -> {
            MathValidationUtil.validateBalancedMathDelimiters(t.getQuestionText());
            if (t.getOptionsJson() != null) {
                MathValidationUtil.validateBalancedMathDelimiters(t.getOptionsJson());
            }

            QuestionTranslation qt = new QuestionTranslation();
            qt.setQuestion(question);
            qt.setLanguage(t.getLanguage());
            qt.setQuestionText(t.getQuestionText());
            qt.setOptionsJson(t.getOptionsJson());
            qt.setCreatedBy(adminId);
            qt.setUpdatedBy(adminId);
            return qt;
        }).collect(Collectors.toList());

        question.getTranslations().addAll(translations);
        questionRepository.save(question);

        String warning = question.isLocked() ? "This question is used in one or more published tests. Changes apply to future attempts only, per current attempt snapshots already taken." : null;
        QuestionDetailDto updatedQuestion = getQuestionById(question.getId());
        return new QuestionDetailDto(updatedQuestion.id(), updatedQuestion.questionType(), updatedQuestion.translations(),
                updatedQuestion.correctAnswerJson(), updatedQuestion.positiveMarks(), updatedQuestion.negativeMarks(),
                updatedQuestion.explanation(), updatedQuestion.difficulty(), updatedQuestion.isLocked(),
                updatedQuestion.createdAt(), updatedQuestion.createdBy(), warning);
    }

    @Transactional
    public QuestionDetailDto deleteQuestion(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (!question.getSectionQuestions().isEmpty()) {
            throw new ValidationException("Cannot delete question: It is currently attached to one or more test sections. Detach it first.");
        }

        question.setDeletedAt(java.time.Instant.now());
        questionRepository.save(question);
        return getQuestionById(id);
    }

    private Map<String, Object> extractOptionsForValidation(List<QuestionTranslationDto> translations) {
        try {
            for (QuestionTranslationDto translation : translations) {
                if (translation.getOptionsJson() != null && !translation.getOptionsJson().isBlank()) {
                    return objectMapper.readValue(translation.getOptionsJson(), new TypeReference<>() {});
                }
            }
        } catch (Exception e) {
            throw new ValidationException("Invalid JSON format in options string.");
        }
        return null;
    }
}
