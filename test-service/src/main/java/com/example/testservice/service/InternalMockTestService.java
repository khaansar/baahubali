package com.example.testservice.service;

import com.example.testservice.dto.internal.*;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.Status;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.MockTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalMockTestService {

    private final MockTestRepository mockTestRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "test_blueprints", key = "#testId + '-' + #root.target.getTestLastUpdated(#testId)")
    public TestBlueprintDto getPublishedBlueprint(UUID testId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        if (test.getStatus() == Status.DRAFT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot fetch blueprint for DRAFT tests");
        }

        return new TestBlueprintDto(
                test.getId(),
                test.getSeries().getCategory().getId(),
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
                                )).collect(Collectors.toList()),
                                sq.getQuestion().getCorrectAnswerJson(),
                                sq.getPositiveMarksOverride() != null
                                        ? sq.getPositiveMarksOverride()
                                        : sq.getQuestion().getPositiveMarks(),
                                sq.getNegativeMarksOverride() != null
                                        ? sq.getNegativeMarksOverride()
                                        : sq.getQuestion().getNegativeMarks(),
                                sq.getQuestion().getExplanation(),
                                sq.getQuestion().getCreatedAt(),
                                sq.getQuestion().getUpdatedAt(),
                                sq.getQuestion().getDeletedAt()
                        )).collect(Collectors.toList())
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

    public String getTestLastUpdated(UUID testId) {
        return mockTestRepository.findById(testId)
                .map(test -> test.getUpdatedAt().toString())
                .orElse("unknown");
    }

    @Transactional(readOnly = true)
    public java.util.List<TestBulkInfoDto> getBulkTestInfo(java.util.List<UUID> testIds) {
        return mockTestRepository.findAllById(testIds).stream()
                .map(test -> TestBulkInfoDto.builder()
                        .testId(test.getId())
                        .testName(test.getTitle())
                        .categoryName(test.getSeries() != null && test.getSeries().getCategory() != null
                                ? test.getSeries().getCategory().getName()
                                : "Unknown")
                        .build())
                .collect(Collectors.toList());
    }
}