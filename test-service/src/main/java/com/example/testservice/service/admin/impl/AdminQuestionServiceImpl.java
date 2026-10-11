package com.example.testservice.service.admin.impl;

import com.example.common.audit.AuditEvent;
import com.example.testservice.dto.QuestionImageDto;
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
import com.example.testservice.service.KafkaPublisherService;
import com.example.testservice.service.MathValidationUtil;
import com.example.testservice.validation.QuestionValidationUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminQuestionServiceImpl {

    private final QuestionRepository questionRepository;
    private final KafkaPublisherService kafkaPublisherService;
    private final ObjectMapper objectMapper;
    private final QuestionImageMetadataValidator questionImageMetadataValidator;

   @Transactional
    @CacheEvict(value = {"baahubali:test:test", "test_blueprints"}, allEntries = true)
    public UUID createQuestion(QuestionCreateDto dto, String adminId) {
        validateTranslations(dto.translations());
        validateQuestionImages(dto.translations(), dto.explanationImages());

        Map<String, Object> validationOptions =
                extractOptionsForValidation(dto.translations());

        if (!QuestionValidationUtil.isValidAnswerSchema(
                dto.questionType(), dto.correctAnswerJson(), validationOptions)) {
            throw new ValidationException(
                    "Invalid answer schema or missing options for question type: "
                            + dto.questionType());
        }

        Question question = new Question();
        question.setQuestionType(dto.questionType());
        question.setCorrectAnswerJson(dto.correctAnswerJson());
        question.setPositiveMarks(dto.positiveMarks());
        question.setNegativeMarks(dto.negativeMarks());
        question.setExplanation(dto.explanation());
        question.setExplanationImages(toImageMaps(dto.explanationImages()));
        question.setTopic(dto.topic());
        question.setDifficulty(dto.difficulty());
        question.setCreatedBy(adminId);
        question.setUpdatedBy(adminId);
        question.setLocked(false);
        question.setTranslations(
                buildTranslations(dto.translations(), question, adminId));

        Question saved = questionRepository.save(question);

        kafkaPublisherService.emitAuditEvent(new AuditEvent(
                UUID.randomUUID(),
                adminId,
                "ADMIN",
                "QUESTION_CREATED",
                "QUESTION",
                saved.getId().toString(),
                "test-service",
                "/admin/questions",
                "POST",
                201,
                null,
                Map.of(),
                Instant.now()
        ));

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public PaginatedResponseDto<QuestionListDto> getQuestions(
            String search,
            String type,
            String difficulty,
            Boolean isLocked,
            Boolean unusedOnly,
            int page,
            int limit) {

        var spec = QuestionSpecifications.buildFilter(
                search, type, difficulty, isLocked, unusedOnly);

        var pageable = org.springframework.data.domain.PageRequest.of(
                page - 1,
                limit,
                org.springframework.data.domain.Sort.by("createdAt").descending());

        var questionPage = questionRepository.findAll(spec, pageable);

        List<QuestionListDto> data = questionPage.getContent().stream().map(q -> {
            String fullText = q.getTranslations().isEmpty()
                    ? "No Translation"
                    : q.getTranslations().get(0).getQuestionText();

            String shortText = fullText.length() > 60
                    ? fullText.substring(0, 57) + "..."
                    : fullText;

            return new QuestionListDto(
                    q.getId(),
                    q.getQuestionType(),
                    shortText,
                    q.getTopic(),
                    q.getPositiveMarks(),
                    q.getDifficulty(),
                    q.isLocked(),
                    q.getCreatedAt(),
                    q.getUpdatedAt(),
                    q.getDeletedAt()
            );
        }).toList();

        PageMetaDto meta = new PageMetaDto(
                page,
                limit,
                questionPage.getTotalElements(),
                questionPage.getTotalPages()
        );

        return new PaginatedResponseDto<>(data, meta);
    }

    @Transactional(readOnly = true)
    public QuestionDetailDto getQuestionById(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Question not found"));

        List<QuestionTranslationDto> translationDtos =
                question.getTranslations().stream().map(this::toTranslationDto)
                        .collect(Collectors.toList());

        return new QuestionDetailDto(
                question.getId(),
                question.getQuestionType(),
                translationDtos,
                question.getCorrectAnswerJson(),
                question.getPositiveMarks(),
                question.getNegativeMarks(),
                question.getExplanation(),
                toImageDtos(question.getExplanationImages()),
                question.getTopic(),
                question.getDifficulty(),
                question.isLocked(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                question.getDeletedAt(),
                question.getCreatedBy(),
                null
        );
    }

    @Transactional
    @CacheEvict(value = {"baahubali:test:test", "test_blueprints"}, allEntries = true)
    public BulkImportResultDto bulkImportQuestions(List<QuestionCreateDto> rows, String adminId) {

        int imported = 0;
        int failed = 0;

        List<BulkImportResultDto.ImportError> errors = new ArrayList<>();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            QuestionCreateDto row = rows.get(i);

            try {
                validateTranslations(row.translations());
                validateQuestionImages(row.translations(), row.explanationImages());

                Map<String, Object> validationOptions =
                        extractOptionsForValidation(row.translations());

                if (!QuestionValidationUtil.isValidAnswerSchema(
                        row.questionType(),
                        row.correctAnswerJson(),
                        validationOptions)) {
                    throw new ValidationException(
                            "Invalid answer schema or missing options.");
                }

                Question question = new Question();
                question.setQuestionType(row.questionType());
                question.setCorrectAnswerJson(row.correctAnswerJson());
                question.setPositiveMarks(row.positiveMarks());
                question.setNegativeMarks(row.negativeMarks());
                question.setExplanation(row.explanation());
                question.setExplanationImages(
                        toImageMaps(row.explanationImages()));
                question.setTopic(row.topic());
                question.setDifficulty(row.difficulty());
                question.setCreatedBy(adminId);
                question.setUpdatedBy(adminId);
                question.setLocked(false);
                question.setTranslations(
                        buildTranslations(row.translations(), question, adminId));

                Question saved = questionRepository.save(question);

                imported++;
                createdIds.add(saved.getId());

            } catch (Exception e) {
                failed++;
                errors.add(new BulkImportResultDto.ImportError(
                        i + 1,
                        e.getMessage() == null
                                ? "Question import failed."
                                : e.getMessage()));
            }
        }

        if (!createdIds.isEmpty()) {
            kafkaPublisherService.emitAuditEvent(new AuditEvent(
                    UUID.randomUUID(),
                    adminId,
                    "ADMIN",
                    "QUESTIONS_BULK_IMPORTED",
                    "QUESTION",
                    null,
                    "test-service",
                    "/admin/questions/bulk",
                    "POST",
                    201,
                    null,
                    Map.of(
                            "createdCount", String.valueOf(createdIds.size()),
                            "createdIds", String.join(",",
                                    createdIds.stream()
                                            .map(UUID::toString)
                                            .toList())
                    ),
                    Instant.now()
            ));
        }

        return new BulkImportResultDto(imported, failed, errors, createdIds);
    }

    @Transactional
    @CacheEvict(value = {"baahubali:test:test", "test_blueprints"}, allEntries = true)
    public QuestionDetailDto updateQuestion(UUID id, QuestionUpdateDto dto, String adminId) {

        Question question = questionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Question not found"));

        if (dto.questionType() == null
                || question.getQuestionType() != dto.questionType()) {
            throw new ValidationException(
                    "Cannot change question_type. Delete and recreate the question instead.");
        }

        validateTranslations(dto.translations());
        validateQuestionImages(dto.translations(), dto.explanationImages());

        Map<String, Object> validationOptions =
                extractOptionsForValidation(dto.translations());

        if (!QuestionValidationUtil.isValidAnswerSchema(
                question.getQuestionType(),
                dto.correctAnswerJson(),
                validationOptions)) {
            throw new ValidationException(
                    "Invalid answer schema for question type "
                            + question.getQuestionType());
        }

        question.setCorrectAnswerJson(dto.correctAnswerJson());
        question.setPositiveMarks(dto.positiveMarks());
        question.setNegativeMarks(dto.negativeMarks());
        question.setExplanation(dto.explanation());
        question.setExplanationImages(toImageMaps(dto.explanationImages()));
        question.setTopic(dto.topic());
        question.setDifficulty(dto.difficulty());
        question.setUpdatedBy(adminId);

        question.getTranslations().clear();

        // Flush orphan deletions before replacement rows are inserted.
        // This preserves the unique constraint on (question_id, language).
        questionRepository.flush();

        List<QuestionTranslation> translations =
                buildTranslations(dto.translations(), question, adminId);

        question.getTranslations().addAll(translations);
        questionRepository.save(question);

        String warning = question.isLocked()
                ? "This question is used in one or more published tests. "
                + "Changes apply to future attempts only, per current attempt "
                + "snapshots already taken."
                : null;

        QuestionDetailDto updatedQuestion = getQuestionById(question.getId());

        return new QuestionDetailDto(
                updatedQuestion.id(),
                updatedQuestion.questionType(),
                updatedQuestion.translations(),
                updatedQuestion.correctAnswerJson(),
                updatedQuestion.positiveMarks(),
                updatedQuestion.negativeMarks(),
                updatedQuestion.explanation(),
                updatedQuestion.explanationImages(),
                updatedQuestion.topic(),
                updatedQuestion.difficulty(),
                updatedQuestion.isLocked(),
                updatedQuestion.createdAt(),
                updatedQuestion.updatedAt(),
                updatedQuestion.deletedAt(),
                updatedQuestion.createdBy(),
                warning
        );
    }

    @Transactional
    @CacheEvict(value = {"baahubali:test:test", "test_blueprints"}, allEntries = true)
    public QuestionDetailDto deleteQuestion(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Question not found"));

        if (!question.getSectionQuestions().isEmpty()) {
            throw new ValidationException(
                    "Cannot delete question: It is currently attached to one "
                            + "or more test sections. Detach it first.");
        }

        question.setDeletedAt(Instant.now());
        questionRepository.save(question);

        return getQuestionById(id);
    }

    private void validateTranslations(List<QuestionTranslationDto> translations) {
        if (translations == null || translations.isEmpty()) {
            throw new ValidationException(
                    "At least one translation is required.");
        }
    }

    private List<QuestionTranslation> buildTranslations(
            List<QuestionTranslationDto> translationDtos,
            Question question,
            String adminId) {

        return translationDtos.stream().map(dto -> {
            MathValidationUtil.validateBalancedMathDelimiters(
                    dto.getQuestionText());

            if (dto.getOptionsJson() != null) {
                MathValidationUtil.validateBalancedMathDelimiters(
                        dto.getOptionsJson());
            }

            QuestionTranslation translation = new QuestionTranslation();
            translation.setQuestion(question);
            translation.setLanguage(dto.getLanguage());
            translation.setQuestionText(dto.getQuestionText());
            translation.setOptionsJson(dto.getOptionsJson());
            translation.setQuestionImages(toImageMaps(dto.getQuestionImages()));
            translation.setOptionImages(toOptionImageMaps(dto.getOptionImages()));
            translation.setCreatedBy(adminId);
            translation.setUpdatedBy(adminId);

            return translation;
        }).collect(Collectors.toList());
    }

    private QuestionTranslationDto toTranslationDto(
            QuestionTranslation translation) {

        QuestionTranslationDto dto = new QuestionTranslationDto();
        dto.setLanguage(translation.getLanguage());
        dto.setQuestionText(translation.getQuestionText());
        dto.setOptionsJson(translation.getOptionsJson());
        dto.setQuestionImages(toImageDtos(translation.getQuestionImages()));
        dto.setOptionImages(toOptionImageDtos(translation.getOptionImages()));
        dto.setCreatedAt(translation.getCreatedAt());
        dto.setUpdatedAt(translation.getUpdatedAt());
        dto.setDeletedAt(translation.getDeletedAt());

        return dto;
    }

    private List<Map<String, Object>> toImageMaps(
            List<QuestionImageDto> images) {

        if (images == null) {
            return null;
        }

        return objectMapper.convertValue(
                images,
                new TypeReference<List<Map<String, Object>>>() {});
    }

    private Map<String, List<Map<String, Object>>> toOptionImageMaps(
            Map<String, List<QuestionImageDto>> optionImages) {

        if (optionImages == null) {
            return null;
        }

        return objectMapper.convertValue(
                optionImages,
                new TypeReference<Map<String, List<Map<String, Object>>>>() {});
    }

    private List<QuestionImageDto> toImageDtos(
            List<Map<String, Object>> images) {

        if (images == null) {
            return null;
        }

        return objectMapper.convertValue(
                images,
                new TypeReference<List<QuestionImageDto>>() {});
    }

    private Map<String, List<QuestionImageDto>> toOptionImageDtos(
            Map<String, List<Map<String, Object>>> optionImages) {

        if (optionImages == null) {
            return null;
        }

        return objectMapper.convertValue(
                optionImages,
                new TypeReference<Map<String, List<QuestionImageDto>>>() {});
    }

    private Map<String, Object> extractOptionsForValidation(
            List<QuestionTranslationDto> translations) {

        try {
            for (QuestionTranslationDto translation : translations) {
                if (translation.getOptionsJson() != null
                        && !translation.getOptionsJson().isBlank()) {
                    return objectMapper.readValue(
                            translation.getOptionsJson(),
                            new TypeReference<>() {});
                }
            }
        } catch (Exception e) {
            throw new ValidationException(
                    "Invalid JSON format in options string.");
        }

        return null;
    }

    private void validateQuestionImages(List<QuestionTranslationDto> translations, List<QuestionImageDto> explanationImages) {
        questionImageMetadataValidator.validateImages(explanationImages);
        if (translations == null) return;
        for (QuestionTranslationDto translation : translations) {
            questionImageMetadataValidator.validateImages(translation.getQuestionImages());
            if (translation.getOptionImages() != null) {
                for (List<QuestionImageDto> images : translation.getOptionImages().values()) {
                    questionImageMetadataValidator.validateImages(images);
                }
            }
        }
    }
}