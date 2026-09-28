package com.example.testservice.service.admin.impl;

import com.example.testservice.dto.admin.TestSeriesCreateDto;
import com.example.testservice.entity.Category;
import com.example.testservice.entity.Status;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.exception.ResourceConflictException;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.CategoryRepository;
import com.example.testservice.repository.TestSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.testservice.dto.admin.TestSeriesListDto;
import com.example.testservice.dto.admin.TestSeriesDetailDto;
import com.example.testservice.dto.admin.TestSeriesUpdateDto;
import com.example.testservice.dto.common.PaginatedResponseDto;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTestSeriesServiceImpl {

    private final TestSeriesRepository testSeriesRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public UUID createSeries(TestSeriesCreateDto dto, String adminId) {
        Category category = null;
        if (dto.categoryId() != null) {
            category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        TestSeries series = new TestSeries();
        series.setTitle(dto.title());
        series.setBasePrice(dto.basePrice());
        series.setCategory(category);
        series.setStatus(Status.DRAFT); // Forced to DRAFT on creation
        series.setCreatedBy(adminId);
        series.setUpdatedBy(adminId);

        return testSeriesRepository.save(series).getId();
    }

    @Transactional
    public TestSeriesDetailDto deleteSeries(UUID seriesId) {
        TestSeries series = testSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Series not found"));

        boolean hasPublishedTests = series.getMockTests().stream()
                .anyMatch(test -> test.getStatus() == Status.PUBLISHED);

        if (hasPublishedTests) {
            throw new ResourceConflictException("Cannot delete series containing PUBLISHED tests. Archive or revert tests to DRAFT first.");
        }

        // Soft delete cascades to mock tests natively via our application layer, 
        // but to keep it simple, we timestamp the parent and rely on queries or cascade logic
        series.setDeletedAt(java.time.Instant.now());
        series.getMockTests().forEach(test -> test.setDeletedAt(java.time.Instant.now()));
        testSeriesRepository.save(series);
        return getSeriesById(seriesId);
    }

    @Transactional(readOnly = true)
    public PaginatedResponseDto<TestSeriesListDto> getSeriesList(String search, String statusStr, UUID categoryId, int page, int limit) {
        
        // Dynamic Specification Builder
        org.springframework.data.jpa.domain.Specification<TestSeries> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }
            if (statusStr != null && !statusStr.isBlank()) {
                predicates.add(cb.equal(root.get("status"), Status.valueOf(statusStr.toUpperCase())));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, limit, 
                org.springframework.data.domain.Sort.by("createdAt").descending());
                
        var seriesPage = testSeriesRepository.findAll(spec, pageable);

        java.util.List<TestSeriesListDto> data = seriesPage.getContent().stream().map(s -> new TestSeriesListDto(
                s.getId(),
                s.getTitle(),
                s.getBasePrice(),
                s.getStatus(),
                s.getCategory() != null ? s.getCategory().getName() : null,
                s.getCreatedAt(),
                s.getUpdatedAt(),
                s.getDeletedAt()
        )).toList();

        return new PaginatedResponseDto<>(data, new com.example.testservice.dto.common.PageMetaDto(
                page, limit, seriesPage.getTotalElements(), seriesPage.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public TestSeriesDetailDto getSeriesById(UUID id) {
        TestSeries series = testSeriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test Series not found"));

        var mockTests = series.getMockTests().stream()
                .map(test -> new TestSeriesDetailDto.MockTestSummaryDto(
                        test.getId(),
                        test.getTitle(),
                        test.getStatus(),
                        test.getDurationMinutes(),
                        test.getTotalMarks(),
                        test.isFree(),
                        test.getCreatedAt(), test.getUpdatedAt(), test.getDeletedAt()
                )).toList();

        return new TestSeriesDetailDto(
                series.getId(),
                series.getTitle(),
                series.getBasePrice(),
                series.getStatus(),
                series.getCategory() != null ? series.getCategory().getId() : null,
                series.getCategory() != null ? series.getCategory().getName() : null,
                series.getCreatedAt(),
                series.getUpdatedAt(),
                series.getDeletedAt(),
                mockTests
        );
    }

    @Transactional
    public TestSeriesDetailDto updateSeries(UUID id, TestSeriesUpdateDto dto, String adminId) {
        TestSeries series = testSeriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test Series not found"));

        Category category = null;
        if (dto.categoryId() != null) {
            category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        series.setTitle(dto.title());
        series.setBasePrice(dto.basePrice());
        series.setCategory(category);
        series.setUpdatedBy(adminId);
        
        testSeriesRepository.save(series);
        return getSeriesById(id);
    }
}
