package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.dto.QuestionImageDto;
import com.example.testservice.dto.publiccatalog.*;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.Question;
import com.example.testservice.entity.QuestionTranslation;
import com.example.testservice.entity.Status;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.MockTestRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl {

    private final MockTestRepository mockTestRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "baahubali:test:test", key = "#testId + ':structure'")
    public PublicMockTestStructureDto getTestStructure(UUID testId) {
        return mapPublishedTest(
                mockTestRepository.findById(testId)
                        .orElseThrow(() -> new ResourceNotFoundException("Test not found"))
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "baahubali:test:test", key = "'slug:' + #slug + ':structure'")
    public PublicMockTestStructureDto getTestStructureBySlug(String slug) {
        return mapPublishedTest(
                mockTestRepository.findBySlug(slug)
                        .orElseThrow(() -> new ResourceNotFoundException("Test not found"))
        );
    }

    private PublicMockTestStructureDto mapPublishedTest(MockTest test) {
        if (test.getStatus() != Status.PUBLISHED) {
            throw new ResourceNotFoundException(
                    "Test is not available in the public catalog."
            );
        }

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
                test.getSections().stream()
                        .map(section -> new PublicSectionDto(
                                section.getId(),
                                section.getTitle(),
                                section.getSequenceOrder(),
                                section.getDurationMinutes(),
                                section.getCreatedAt(),
                                section.getUpdatedAt(),
                                section.getDeletedAt(),
                                section.getSectionQuestions().stream()
                                        .map(sq -> {
                                            Question question = sq.getQuestion();

                                            QuestionTranslation translation =
                                                    question.getTranslations().isEmpty()
                                                            ? null
                                                            : question.getTranslations().get(0);

                                            Map<String, Object> options = null;

                                            if (translation != null
                                                    && translation.getOptionsJson() != null) {
                                                try {
                                                    options = objectMapper.readValue(
                                                            translation.getOptionsJson(),
                                                            new TypeReference<Map<String, Object>>() {}
                                                    );
                                                } catch (Exception ignored) {
                                                    // Invalid options JSON must not break the catalog response.
                                                }
                                            }

                                            List<QuestionImageDto> questionImages =
                                                    translation == null
                                                            || translation.getQuestionImages() == null
                                                            ? null
                                                            : objectMapper.convertValue(
                                                                    translation.getQuestionImages(),
                                                                    new TypeReference<List<QuestionImageDto>>() {}
                                                            );

                                            Map<String, List<QuestionImageDto>> optionImages =
                                                    translation == null
                                                            || translation.getOptionImages() == null
                                                            ? null
                                                            : objectMapper.convertValue(
                                                                    translation.getOptionImages(),
                                                                    new TypeReference<Map<String, List<QuestionImageDto>>>() {}
                                                            );

                                            return new PublicQuestionDto(
                                                    question.getId(),
                                                    sq.getSequenceOrder(),
                                                    question.getQuestionType().name(),
                                                    translation == null
                                                            ? ""
                                                            : translation.getQuestionText(),
                                                    options,
                                                    questionImages,
                                                    optionImages,
                                                    sq.getPositiveMarksOverride() != null
                                                            ? sq.getPositiveMarksOverride()
                                                            : question.getPositiveMarks(),
                                                    sq.getNegativeMarksOverride() != null
                                                            ? sq.getNegativeMarksOverride()
                                                            : question.getNegativeMarks(),
                                                    question.getCreatedAt(),
                                                    question.getUpdatedAt(),
                                                    question.getDeletedAt()
                                            );
                                        })
                                        .collect(Collectors.toList())
                        ))
                        .collect(Collectors.toList())
        );
    }
}