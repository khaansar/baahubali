package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.dto.publiccatalog.*;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.Status;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.MockTestRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl {

    private final MockTestRepository mockTestRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "public_test_structures", key = "#testId")
    public PublicMockTestStructureDto getTestStructure(UUID testId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found"));

        if (test.getStatus() != Status.PUBLISHED) {
            throw new ResourceNotFoundException("Test is not available in the public catalog.");
        }

        return new PublicMockTestStructureDto(
                test.getId(),
                test.getTitle(),
                test.getDurationMinutes(),
                test.getInstructions(),
                test.getTotalMarks(),
                test.isFree(),
                test.getCreatedAt(), test.getUpdatedAt(), test.getDeletedAt(),
                test.getSections().stream().map(section -> new PublicSectionDto(
                        section.getId(),
                        section.getTitle(),
                        section.getSequenceOrder(),
                        section.getCreatedAt(), section.getUpdatedAt(), section.getDeletedAt(),
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
                                    sq.getQuestion().getCreatedAt(), sq.getQuestion().getUpdatedAt(), sq.getQuestion().getDeletedAt()
                            );
                        }).collect(Collectors.toList())
                )).collect(Collectors.toList())
        );
    }
}
