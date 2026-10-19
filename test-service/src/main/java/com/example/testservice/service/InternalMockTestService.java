package com.example.testservice.service;

import com.example.testservice.dto.QuestionImageDto;
import com.example.testservice.dto.internal.*;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.Status;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.MockTestRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalMockTestService {

    private final MockTestRepository mockTestRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "test_blueprints", key = "#testId")
    public TestBlueprintDto getPublishedBlueprint(UUID testId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        if (test.getStatus() == Status.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cannot fetch blueprint for DRAFT tests"
            );
        }

        return new TestBlueprintDto(
                test.getId(),
                test.getSeries().getCategory().getId(),
                test.getSeries().getId(),
                test.getTitle(),
                test.getDurationMinutes(),
                test.getInstructions(),
                test.isFree(),
                test.getAccessCode(),
                test.getWindowStartTime(),
                test.getWindowEndTime(),
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
                        section.getSectionQuestions().stream().map(sq -> {
                            var question = sq.getQuestion();

                            List<QuestionTranslationBlueprintDto> translations =
                                    question.getTranslations().stream()
                                            .map(t -> new QuestionTranslationBlueprintDto(
                                                    t.getLanguage(),
                                                    t.getQuestionText(),
                                                    t.getOptionsJson(),
                                                    t.getQuestionImages() == null
                                                            ? null
                                                            : objectMapper.convertValue(
                                                                    t.getQuestionImages(),
                                                                    new TypeReference<List<QuestionImageDto>>() {}
                                                            ),
                                                    t.getOptionImages() == null
                                                            ? null
                                                            : objectMapper.convertValue(
                                                                    t.getOptionImages(),
                                                                    new TypeReference<Map<String, List<QuestionImageDto>>>() {}
                                                            ),
                                                    t.getCreatedAt(),
                                                    t.getUpdatedAt(),
                                                    t.getDeletedAt()
                                            ))
                                            .collect(Collectors.toList());

                            List<QuestionImageDto> explanationImages =
                                    question.getExplanationImages() == null
                                            ? null
                                            : objectMapper.convertValue(
                                                    question.getExplanationImages(),
                                                    new TypeReference<List<QuestionImageDto>>() {}
                                            );

                            return new QuestionBlueprintDto(
                                    question.getId(),
                                    sq.getSequenceOrder(),
                                    question.getQuestionType().name(),
                                    question.getTopic(),
                                    translations,
                                    question.getCorrectAnswerJson(),
                                    sq.getPositiveMarksOverride() != null
                                            ? sq.getPositiveMarksOverride()
                                            : question.getPositiveMarks(),
                                    sq.getNegativeMarksOverride() != null
                                            ? sq.getNegativeMarksOverride()
                                            : question.getNegativeMarks(),
                                    question.getExplanation(),
                                    explanationImages,
                                    question.getCreatedAt(),
                                    question.getUpdatedAt(),
                                    question.getDeletedAt()
                            );
                        }).collect(Collectors.toList())
                )).collect(Collectors.toList())
        );
    }

    @Transactional(readOnly = true)
    public TestStatusDto getLightweightStatus(UUID testId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        return new TestStatusDto(
                test.getId(),
                test.getStatus(),
                test.getTotalMarks(),
                test.getDurationMinutes(),
                test.getCreatedAt(),
                test.getUpdatedAt(),
                test.getDeletedAt()
        );
    }

    @Transactional(readOnly = true)
    public TestStatusDto getPublishedTestStatusBySlug(String slug) {
        MockTest test = mockTestRepository.findBySlug(slug.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        if (test.getStatus() != Status.PUBLISHED || test.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Test is not available in the public catalog.");
        }

        return new TestStatusDto(
                test.getId(),
                test.getStatus(),
                test.getTotalMarks(),
                test.getDurationMinutes(),
                test.getCreatedAt(),
                test.getUpdatedAt(),
                test.getDeletedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<TestBulkInfoDto> getBulkTestInfo(List<UUID> testIds) {
        return mockTestRepository.findAllById(testIds).stream()
                .map(test -> TestBulkInfoDto.builder()
                        .testId(test.getId())
                        .testName(test.getTitle())
                        .categoryName(
                                test.getSeries() != null && test.getSeries().getCategory() != null
                                        ? test.getSeries().getCategory().getName()
                                        : "Unknown"
                        )
                        .build())
                .collect(Collectors.toList());
    }
}
