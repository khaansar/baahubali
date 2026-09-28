package com.example.testservice.service.admin.impl;

import com.example.testservice.dto.admin.BulkAttachQuestionsDto;
import com.example.testservice.dto.admin.AdminSectionDetailDto;
import com.example.testservice.dto.admin.SectionCreateDto;
import com.example.testservice.entity.*;
import com.example.testservice.exception.ResourceConflictException;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.QuestionRepository;
import com.example.testservice.repository.SectionQuestionRepository;
import com.example.testservice.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.testservice.repository.MockTestRepository;
import com.example.testservice.exception.ValidationException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSectionServiceImpl {

    private final SectionRepository sectionRepository;
    private final QuestionRepository questionRepository;
    private final SectionQuestionRepository sectionQuestionRepository; 
    private final MockTestRepository mockTestRepository;

    @Transactional
    public AdminSectionDetailDto attachQuestions(UUID sectionId, BulkAttachQuestionsDto request, String adminId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        // OPTION A REQUIREMENT: Block structural edits if published
        if (section.getMockTest().getStatus() == Status.PUBLISHED) {
            throw new ResourceConflictException("Cannot modify questions in a PUBLISHED test. Revert to DRAFT first.");
        }

        int currentMaxSequence = section.getSectionQuestions().stream()
                .mapToInt(SectionQuestion::getSequenceOrder)
                .max()
                .orElse(0);

        List<Question> questions = questionRepository.findAllById(request.questionIds());
        if (questions.size() != request.questionIds().size()) {
            throw new ResourceNotFoundException("One or more questions not found in the bank");
        }

        for (Question question : questions) {
            // Prevent duplicate mappings
            boolean alreadyExists = section.getSectionQuestions().stream()
                    .anyMatch(sq -> sq.getQuestion().getId().equals(question.getId()));
            
            if (alreadyExists) continue;

            currentMaxSequence++;
            
            SectionQuestion sq = new SectionQuestion();
            sq.setSection(section);
            sq.setQuestion(question);
            sq.setSequenceOrder(currentMaxSequence);
            sq.setPositiveMarksOverride(request.positiveMarksOverride());
            sq.setNegativeMarksOverride(request.negativeMarksOverride() != null ? request.negativeMarksOverride() : section.getDefaultNegativeMarks());
            sq.setCreatedBy(adminId);
            sq.setUpdatedBy(adminId);
            
            sectionQuestionRepository.save(sq);
            section.getSectionQuestions().add(sq);
        }
        
        // Touch the parent test's updated_at timestamp to invalidate caches and increment version
        section.getMockTest().setUpdatedAt(java.time.Instant.now());
        return getSectionDetail(sectionId);
    }

    @Transactional
    public UUID createSection(UUID testId, SectionCreateDto dto, String adminId) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Mock Test not found"));

        if (test.getStatus() == Status.PUBLISHED) {
            throw new ResourceConflictException("Cannot add sections to a PUBLISHED test. Revert to DRAFT first.");
        }

        if (dto.durationMinutes() != null && test.getDurationMinutes() != null) {
            int currentTotalDuration = test.getSections().stream()
                    .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                    .sum();
            
            if (currentTotalDuration + dto.durationMinutes() > test.getDurationMinutes()) {
                throw new ValidationException(String.format(
                        "Total section duration (%d min) cannot exceed test duration (%d min). You have %d min left.",
                        currentTotalDuration + dto.durationMinutes(), 
                        test.getDurationMinutes(),
                        Math.max(0, test.getDurationMinutes() - currentTotalDuration)
                ));
            }
        }

        // Auto-assign sequence order
        int nextOrder = test.getSections().stream()
                .mapToInt(Section::getSequenceOrder)
                .max()
                .orElse(0) + 1;

        Section section = new Section();
        section.setMockTest(test);
        section.setTitle(dto.title());
        section.setDurationMinutes(dto.durationMinutes());
        section.setShuffleQuestions(dto.shuffleQuestions());
        section.setDefaultNegativeMarks(dto.defaultNegativeMarks());
        section.setSequenceOrder(nextOrder);
        section.setCreatedBy(adminId);
        section.setUpdatedBy(adminId);

        return sectionRepository.save(section).getId();
    }

    @Transactional
    public AdminSectionDetailDto updateSection(UUID sectionId, SectionCreateDto dto, String adminId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        MockTest test = section.getMockTest();
        if (test.getStatus() == Status.PUBLISHED) {
            throw new ResourceConflictException("Cannot modify sections in a PUBLISHED test. Revert to DRAFT first.");
        }

        if (dto.durationMinutes() != null && test.getDurationMinutes() != null) {
            int currentTotalDuration = test.getSections().stream()
                    .filter(s -> !s.getId().equals(sectionId))
                    .mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                    .sum();
            
            if (currentTotalDuration + dto.durationMinutes() > test.getDurationMinutes()) {
                throw new ValidationException(String.format(
                        "Total section duration (%d min) cannot exceed test duration (%d min). You have %d min left.",
                        currentTotalDuration + dto.durationMinutes(), 
                        test.getDurationMinutes(),
                        Math.max(0, test.getDurationMinutes() - currentTotalDuration)
                ));
            }
        }

        section.setTitle(dto.title());
        section.setDurationMinutes(dto.durationMinutes());
        section.setShuffleQuestions(dto.shuffleQuestions());
        section.setDefaultNegativeMarks(dto.defaultNegativeMarks());
        section.setUpdatedBy(adminId);
        
        if (dto.defaultNegativeMarks() != null) {
            for (SectionQuestion sq : section.getSectionQuestions()) {
                sq.setNegativeMarksOverride(dto.defaultNegativeMarks());
            }
        }
        
        test.setUpdatedAt(java.time.Instant.now());
        sectionRepository.save(section);
        
        return getSectionDetail(sectionId);
    }

    @Transactional(readOnly = true)
    public AdminSectionDetailDto getSectionDetail(UUID sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        return new AdminSectionDetailDto(
                section.getId(),
                section.getMockTest().getId(),
                section.getTitle(),
                section.getSequenceOrder(),
                section.getDurationMinutes(),
                section.isShuffleQuestions(),
                section.getDefaultNegativeMarks(),
                section.getSectionQuestions().size(),
                section.getCreatedAt(),
                section.getUpdatedAt(),
                section.getSectionQuestions().stream()
                        .sorted(java.util.Comparator.comparingInt(SectionQuestion::getSequenceOrder))
                        .map(sq -> new AdminSectionDetailDto.QuestionMappingDto(
                                sq.getQuestion().getId(), sq.getSequenceOrder(),
                                sq.getPositiveMarksOverride(), sq.getNegativeMarksOverride()))
                        .toList()
        );
    }
    
    @Transactional
    public List<AdminSectionDetailDto> reorderSections(UUID testId, java.util.List<UUID> orderedSectionIds) {
        MockTest test = mockTestRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Mock Test not found"));

        if (test.getStatus() == Status.PUBLISHED) {
            throw new ResourceConflictException("Cannot reorder sections in a PUBLISHED test.");
        }
        
        if (test.getSections().size() != orderedSectionIds.size()) {
            throw new ValidationException("Reorder list must contain all existing section IDs exactly once.");
        }

        for (int i = 0; i < orderedSectionIds.size(); i++) {
            UUID id = orderedSectionIds.get(i);
            Section section = test.getSections().stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Invalid section ID provided in reorder list: " + id));
            
            section.setSequenceOrder(i + 1);
        }
        
        // Touch test updated_at
        test.setUpdatedAt(java.time.Instant.now());
        mockTestRepository.save(test);
        return test.getSections().stream()
                .sorted(java.util.Comparator.comparingInt(Section::getSequenceOrder))
                .map(section -> getSectionDetail(section.getId()))
                .toList();
    }

    @Transactional
    public AdminSectionDetailDto removeQuestionFromSection(UUID sectionId, UUID questionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        if (section.getMockTest().getStatus() == Status.PUBLISHED) {
            throw new ResourceConflictException("Cannot remove questions from a PUBLISHED test. Revert to DRAFT first.");
        }

        SectionQuestion sqToRemove = section.getSectionQuestions().stream()
                .filter(sq -> sq.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Question not found in this section"));

        section.getSectionQuestions().remove(sqToRemove);
        sectionQuestionRepository.delete(sqToRemove);

        // Re-sequence remaining questions to close the gap
        int newOrder = 1;
        for (SectionQuestion sq : section.getSectionQuestions()) {
            sq.setSequenceOrder(newOrder++);
        }
        
        section.getMockTest().setUpdatedAt(java.time.Instant.now());
        return getSectionDetail(sectionId);
    }

    @Transactional
    public AdminSectionDetailDto reorderQuestions(UUID sectionId, java.util.List<UUID> orderedQuestionIds) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        if (section.getMockTest().getStatus() == Status.PUBLISHED) {
            throw new ResourceConflictException("Cannot reorder questions in a PUBLISHED test.");
        }

        if (section.getSectionQuestions().size() != orderedQuestionIds.size()) {
            throw new ValidationException("Reorder list must contain all existing question IDs exactly once.");
        }

        for (int i = 0; i < orderedQuestionIds.size(); i++) {
            UUID qId = orderedQuestionIds.get(i);
            SectionQuestion sq = section.getSectionQuestions().stream()
                    .filter(mapping -> mapping.getQuestion().getId().equals(qId))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Invalid question ID provided in reorder list: " + qId));
            
            sq.setSequenceOrder(i + 1);
        }

        section.getMockTest().setUpdatedAt(java.time.Instant.now());
        return getSectionDetail(sectionId);
    }

    @Transactional
    public AdminSectionDetailDto updateMarksOverride(UUID sectionId, UUID questionId, java.math.BigDecimal positive, java.math.BigDecimal negative) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        SectionQuestion sq = section.getSectionQuestions().stream()
                .filter(mapping -> mapping.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Question not found in this section"));

        // Marks can be updated even if published, but it won't affect active attempts due to Option A
        sq.setPositiveMarksOverride(positive);
        sq.setNegativeMarksOverride(negative);
        
        // Triggers total_marks recalculation if you want to implement a sync, 
        // though typically total_marks is only frozen at publish time.
        return getSectionDetail(sectionId);
    }
}
