package com.example.testservice.service.admin.impl;

import com.example.testservice.client.AttemptServiceClient;
import com.example.testservice.dto.admin.MockTestCreateDto;
import com.example.testservice.dto.admin.AdminMockTestDetailDto;
import com.example.testservice.dto.event.TestPublishedEvent;
import com.example.testservice.dto.internal.QuestionBlueprintDto;
import com.example.testservice.dto.internal.QuestionTranslationBlueprintDto;
import com.example.testservice.dto.internal.SectionBlueprintDto;
import com.example.testservice.dto.internal.TestBlueprintDto;
import com.example.testservice.dto.publiccatalog.PublicMockTestStructureDto;
import com.example.testservice.dto.publiccatalog.PublicQuestionDto;
import com.example.testservice.dto.publiccatalog.PublicSectionDto;
import com.example.testservice.entity.*;
import com.example.testservice.exception.ResourceConflictException;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.exception.ValidationException;
import com.example.testservice.repository.MockTestRepository;
import com.example.testservice.repository.QuestionRepository;
import com.example.testservice.repository.TestSeriesRepository;
import com.example.testservice.service.KafkaPublisherService;
import com.example.testservice.service.LanguageValidationUtil;
import com.example.testservice.service.SlugService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.common.audit.AuditEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMockTestServiceImpl {

    private final MockTestRepository mockTestRepository;
    private final TestSeriesRepository testSeriesRepository;
    private final QuestionRepository questionRepository;
    private final AttemptServiceClient attemptServiceClient;
    private final KafkaPublisherService kafkaPublisherService;
    private final ObjectMapper objectMapper;
    private final SlugService slugService;

    @Value("${internal.auth.secret:secret123}")
    private String internalSecret;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "baahubali:test:test", key = "#testId + ':structure'"),
        @CacheEvict(value = "baahubali:test:homepage:mock-tests:featured", allEntries = true),
        @CacheEvict(value = "baahubali:test:series", key = "#result.seriesId"),
        @CacheEvict(value = "baahubali:test:categories", allEntries = true)
    })
    public AdminMockTestDetailDto publishTest(UUID testId, Instant expectedUpdatedAt) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ValidationException("Test not found"));

        if (!test.getUpdatedAt().equals(expectedUpdatedAt)) {
            throw new ResourceConflictException("Test was modified by another user. Please refresh.");
        }

        List<String> validationErrors = new ArrayList<>();
        if (test.getSections().isEmpty()) {
            validationErrors.add("Test must contain at least one section.");
        }

        List<String> requiredLanguages = test.getSeries().getCategory() != null
                ? test.getSeries().getCategory().getRequiredLanguages()
                : new ArrayList<>();

        BigDecimal totalMarks = BigDecimal.ZERO;
        Set<UUID> questionIdsToLock = new java.util.HashSet<>();

        for (Section section : test.getSections()) {
            if (section.getSectionQuestions().isEmpty()) {
                validationErrors.add(String.format("Section '%s' must contain at least one question.", section.getTitle()));
                continue;
            }

            for (SectionQuestion sq : section.getSectionQuestions()) {
                Question q = sq.getQuestion();

                Set<String> providedLangs = q.getTranslations().stream()
                        .map(QuestionTranslation::getLanguage)
                        .collect(Collectors.toSet());

                try {
                    LanguageValidationUtil.validateTranslationsSet(providedLangs, requiredLanguages);
                } catch (ValidationException e) {
                    validationErrors.add(String.format("Question '%s' in section '%s' is missing a required translation. %s",
                            q.getId(), section.getTitle(), e.getMessage()));
                }

                BigDecimal marksForQuestion = sq.getPositiveMarksOverride() != null
                        ? sq.getPositiveMarksOverride()
                        : q.getPositiveMarks();
                totalMarks = totalMarks.add(marksForQuestion);

                questionIdsToLock.add(q.getId());
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException("Publish validation failed", validationErrors);
        }

        TestSeries series = test.getSeries();

        test.setTotalMarks(totalMarks);
        test.setStatus(Status.PUBLISHED);
        test.setPublishedAt(Instant.now());

        if (series.getStatus() != Status.PUBLISHED) {
            series.setStatus(Status.PUBLISHED);
            testSeriesRepository.save(series);
        }

        mockTestRepository.save(test);

        if (!questionIdsToLock.isEmpty()) {
            questionRepository.lockQuestions(questionIdsToLock);
        }

        TestPublishedEvent event = new TestPublishedEvent(
                test.getId(),
                test.getSeries().getId(),
                test.getTitle(),
                test.getPublishedAt()
        );
        kafkaPublisherService.emitTestPublished(event);
        return getAdminMockTestDetail(testId);
    }

    @Transactional
    @CacheEvict(value = "baahubali:test:series", key = "#seriesId")
    public UUID createMockTest(UUID seriesId, MockTestCreateDto dto, String adminId) {
        TestSeries series = testSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResourceNotFoundException("Series not found"));

        if (series.isDeleted()) {
            throw new ResourceConflictException("Cannot add test to a deleted series");
        }

        MockTest test = new MockTest();
        test.setSeries(series);
        test.setTitle(dto.title());
        test.setSlug(slugService.generateUniqueSlug(dto.title(), mockTestRepository::existsBySlug));
        test.setDurationMinutes(dto.durationMinutes());
        test.setSectionOrderStrict(dto.isSectionOrderStrict());
        test.setShuffleSections(dto.shuffleSections());
        test.setNegativeMarkingEnabled(dto.negativeMarkingEnabled());
        test.setInstructions(dto.instructions());
        test.setFree(dto.isFree());

        test.setStatus(Status.DRAFT);
        test.setTotalMarks(java.math.BigDecimal.ZERO);
        test.setCreatedBy(adminId);
        test.setUpdatedBy(adminId);

        MockTest saved = mockTestRepository.save(test);

        kafkaPublisherService.emitAuditEvent(new AuditEvent(
                UUID.randomUUID(),
                adminId,
                "ADMIN",
                "TEST_CREATED",
                "TEST",
                saved.getId().toString(),
                "test-service",
                "/admin/mock-tests/series/" + seriesId + "/mock-tests",
                "POST",
                201,
                null,
                Map.of("seriesId", seriesId.toString()),
                Instant.now()
        ));

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public AdminMockTestDetailDto getAdminMockTestDetail(UUID testId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        return new AdminMockTestDetailDto(
                test.getId(),
                test.getSlug(),
                test.getSeries().getId(),
                test.getSeries().getSlug(),
                test.getTitle(),
                test.getDurationMinutes(),
                test.getStatus(),
                test.getTotalMarks(),
                test.isSectionOrderStrict(),
                test.isShuffleSections(),
                test.isNegativeMarkingEnabled(),
                test.getInstructions(),
                test.isFree(),
                test.getPublishedAt(),
                test.getCreatedAt(),
                test.getUpdatedAt(),
                test.getDeletedAt(),
                test.getSections().stream()
                        .map(section -> new AdminMockTestDetailDto.SectionSummaryDto(
                                section.getId(),
                                section.getTitle(),
                                section.getSequenceOrder(),
                                section.getDurationMinutes(),
                                section.isShuffleQuestions(),
                                section.getSectionQuestions().size(),
                                section.getCreatedAt(), section.getUpdatedAt(), section.getDeletedAt()
                        ))
                        .toList()
        );
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "baahubali:test:test", key = "#testId + ':structure'"),
        @CacheEvict(value = "baahubali:test:homepage:mock-tests:featured", allEntries = true),
        @CacheEvict(value = "baahubali:test:series", key = "#result.seriesId")
    })
    public AdminMockTestDetailDto updateMockTest(UUID testId, MockTestCreateDto dto) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        if (test.getStatus() == Status.PUBLISHED) {
            if (!test.getDurationMinutes().equals(dto.durationMinutes()) ||
                    test.isSectionOrderStrict() != dto.isSectionOrderStrict()) {
                throw new ResourceConflictException("Cannot change exam duration or section order rules while PUBLISHED. Revert to DRAFT first.");
            }
        }

        if (dto.durationMinutes() != null) {
            int currentTotalDuration = test.getSections().stream()
                    .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                    .sum();
            if (currentTotalDuration > dto.durationMinutes()) {
                throw new ValidationException(String.format("Cannot reduce test duration to %d min. Current sections already total %d min.", dto.durationMinutes(), currentTotalDuration));
            }
        }

        test.setTitle(dto.title());
        test.setDurationMinutes(dto.durationMinutes());
        test.setSectionOrderStrict(dto.isSectionOrderStrict());
        test.setShuffleSections(dto.shuffleSections());
        test.setNegativeMarkingEnabled(dto.negativeMarkingEnabled());
        test.setInstructions(dto.instructions());
        test.setFree(dto.isFree());

        mockTestRepository.save(test);
        return getAdminMockTestDetail(testId);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "baahubali:test:test", key = "#testId + ':structure'"),
        @CacheEvict(value = "baahubali:test:homepage:mock-tests:featured", allEntries = true),
        @CacheEvict(value = "baahubali:test:series", key = "#result.seriesId"),
        @CacheEvict(value = "baahubali:test:categories", allEntries = true)
    })
    public AdminMockTestDetailDto archiveTest(UUID testId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));
        TestSeries series = test.getSeries();

        test.setStatus(Status.ARCHIVED);
        mockTestRepository.save(test);

        boolean hasPublishedTests = series.getMockTests().stream()
                .anyMatch(mockTest ->
                        !mockTest.getId().equals(testId)
                                && mockTest.getStatus() == Status.PUBLISHED);

        if (!hasPublishedTests) {
            series.setStatus(Status.DRAFT);
            testSeriesRepository.save(series);
        }
        return getAdminMockTestDetail(testId);
    }

    @Transactional
    @CacheEvict(value = "baahubali:test:series", key = "#result.seriesId")
    public AdminMockTestDetailDto cloneTest(UUID sourceTestId, String newTitle, String adminId) {
        MockTest source = mockTestRepository.findById(sourceTestId)
                .orElseThrow(() -> new ResourceNotFoundException("Source test not found"));

        MockTest clone = new MockTest();
        String cloneTitle = newTitle != null && !newTitle.isBlank()
                ? newTitle
                : source.getTitle() + " (Copy)";

        clone.setTitle(cloneTitle);
        clone.setSlug(slugService.generateUniqueSlug(cloneTitle, mockTestRepository::existsBySlug));
        clone.setSeries(source.getSeries());
        clone.setDurationMinutes(source.getDurationMinutes());
        clone.setSectionOrderStrict(source.isSectionOrderStrict());
        clone.setShuffleSections(source.isShuffleSections());
        clone.setNegativeMarkingEnabled(source.isNegativeMarkingEnabled());
        clone.setInstructions(source.getInstructions());
        clone.setFree(source.isFree());

        clone.setStatus(Status.DRAFT);
        clone.setTotalMarks(source.getTotalMarks());
        clone.setCreatedBy(adminId);
        clone.setUpdatedBy(adminId);

        for (Section sourceSec : source.getSections()) {
            Section clonedSec = new Section();
            clonedSec.setMockTest(clone);
            clonedSec.setTitle(sourceSec.getTitle());
            clonedSec.setSequenceOrder(sourceSec.getSequenceOrder());
            clonedSec.setDurationMinutes(sourceSec.getDurationMinutes());
            clonedSec.setShuffleQuestions(sourceSec.isShuffleQuestions());
            clonedSec.setCreatedBy(adminId);
            clonedSec.setUpdatedBy(adminId);

            clone.getSections().add(clonedSec);

            for (SectionQuestion sourceSq : sourceSec.getSectionQuestions()) {
                SectionQuestion clonedSq = new SectionQuestion();
                clonedSq.setSection(clonedSec);
                clonedSq.setQuestion(sourceSq.getQuestion());
                clonedSq.setSequenceOrder(sourceSq.getSequenceOrder());
                clonedSq.setPositiveMarksOverride(sourceSq.getPositiveMarksOverride());
                clonedSq.setNegativeMarksOverride(sourceSq.getNegativeMarksOverride());

                clonedSec.getSectionQuestions().add(clonedSq);
            }
        }

        UUID cloneId = mockTestRepository.save(clone).getId();

        kafkaPublisherService.emitAuditEvent(new AuditEvent(
                UUID.randomUUID(),
                adminId,
                "ADMIN",
                "TEST_CLONED",
                "TEST",
                cloneId.toString(),
                "test-service",
                "/admin/mock-tests/" + sourceTestId + "/clone",
                "POST",
                201,
                null,
                Map.of("sourceTestId", sourceTestId.toString()),
                Instant.now()
        ));

        return getAdminMockTestDetail(cloneId);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "baahubali:test:test", key = "#testId + ':structure'"),
        @CacheEvict(value = "baahubali:test:homepage:mock-tests:featured", allEntries = true),
        @CacheEvict(value = "baahubali:test:series", key = "#result.seriesId"),
        @CacheEvict(value = "baahubali:test:categories", allEntries = true)
    })
    public AdminMockTestDetailDto revertToDraft(UUID testId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ValidationException("Test not found"));

        boolean hasActiveAttempts = attemptServiceClient.hasActiveAttempts(testId, internalSecret);
        if (hasActiveAttempts) {
            throw new ResourceConflictException("Cannot revert: active attempts in progress. Wait for completion or force-submit attempts.");
        }

        TestSeries series = test.getSeries();

        boolean hasPublishedTests = series.getMockTests().stream()
                .anyMatch(mockTest ->
                        !mockTest.getId().equals(testId)
                                && mockTest.getStatus() == Status.PUBLISHED);

        if (!hasPublishedTests) {
            series.setStatus(Status.DRAFT);
            testSeriesRepository.save(series);
        }
        return getAdminMockTestDetail(testId);
    }

    @Transactional(readOnly = true)
    public TestBlueprintDto getTestWithAnswers(UUID testId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        return mapToBlueprintDto(test);
    }

    @Transactional(readOnly = true)
    public PublicMockTestStructureDto getMockTestSummary(UUID testId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        return new PublicMockTestStructureDto(
                test.getId(),
                test.getSlug(),
                test.getTitle(),
                test.getDurationMinutes(),
                test.getInstructions(),
                test.getTotalMarks(),
                test.isFree(),
                test.getCreatedAt(),
                test.getUpdatedAt(),
                test.getDeletedAt(),
                test.getSections().stream().map(section -> new PublicSectionDto(
                        section.getId(),
                        section.getTitle(),
                        section.getSequenceOrder(),
                        section.getDurationMinutes(),
                        section.getCreatedAt(),
                        section.getUpdatedAt(),
                        section.getDeletedAt(),
                        section.getSectionQuestions().stream().map(sq -> {
                            Map<String, Object> options = null;
                            if (!sq.getQuestion().getTranslations().isEmpty() && sq.getQuestion().getTranslations().get(0).getOptionsJson() != null) {
                                try {
                                    options = objectMapper.readValue(sq.getQuestion().getTranslations().get(0).getOptionsJson(), new TypeReference<>() {});
                                } catch (Exception ignored) {}
                            }

                            return new PublicQuestionDto(
                                    sq.getQuestion().getId(),
                                    sq.getSequenceOrder(),
                                    sq.getQuestion().getQuestionType().name(),
                                    sq.getQuestion().getTranslations().isEmpty() ? "" : sq.getQuestion().getTranslations().get(0).getQuestionText(),
                                    options,
                                    sq.getPositiveMarksOverride() != null ? sq.getPositiveMarksOverride() : sq.getQuestion().getPositiveMarks(),
                                    sq.getNegativeMarksOverride() != null ? sq.getNegativeMarksOverride() : sq.getQuestion().getNegativeMarks(),
                                    sq.getQuestion().getCreatedAt(),
                                    sq.getQuestion().getUpdatedAt(),
                                    sq.getQuestion().getDeletedAt()
                            );
                        }).toList()
                )).toList()
        );
    }

    private TestBlueprintDto mapToBlueprintDto(MockTest test) {
        return new TestBlueprintDto(
                test.getId(),
                test.getSeries().getCategory() != null ? test.getSeries().getCategory().getId() : null,
                test.getSeries().getId(),
                test.getTitle(),
                test.getDurationMinutes(),
                test.getInstructions(),
                test.isFree(),
                test.isSectionOrderStrict(),
                test.isShuffleSections(),
                test.isNegativeMarkingEnabled(),
                test.getTotalMarks(),
                test.getCreatedAt(),
                test.getUpdatedAt(),
                test.getDeletedAt(),
                test.getSections().stream().map(section -> new SectionBlueprintDto(
                        section.getId(),
                        section.getTitle(),
                        section.getSequenceOrder(),
                        section.getDurationMinutes(),
                        section.isShuffleQuestions(),
                        section.getCreatedAt(),
                        section.getUpdatedAt(),
                        section.getDeletedAt(),
                        section.getSectionQuestions().stream().map(sq -> new QuestionBlueprintDto(
                                sq.getQuestion().getId(),
                                sq.getSequenceOrder(),
                                sq.getQuestion().getQuestionType().name(),
                                sq.getQuestion().getTopic(),
                                sq.getQuestion().getTranslations().stream().map(t -> new QuestionTranslationBlueprintDto(
                                        t.getLanguage(),
                                        t.getQuestionText(),
                                        t.getOptionsJson(),
                                        t.getCreatedAt(),
                                        t.getUpdatedAt(),
                                        t.getDeletedAt()
                                )).toList(),
                                sq.getQuestion().getCorrectAnswerJson(),
                                sq.getPositiveMarksOverride() != null ? sq.getPositiveMarksOverride() : sq.getQuestion().getPositiveMarks(),
                                sq.getNegativeMarksOverride() != null ? sq.getNegativeMarksOverride() : sq.getQuestion().getNegativeMarks(),
                                sq.getQuestion().getExplanation(),
                                sq.getQuestion().getCreatedAt(),
                                sq.getQuestion().getUpdatedAt(),
                                sq.getQuestion().getDeletedAt()
                        )).toList()
                )).toList()
        );
    }
}