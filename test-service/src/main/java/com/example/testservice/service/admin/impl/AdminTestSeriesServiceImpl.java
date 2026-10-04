package com.example.testservice.service.admin.impl;

import com.example.common.audit.AuditEvent;
import com.example.testservice.dto.admin.TestSeriesCreateDto;
import com.example.testservice.dto.admin.TestSeriesDetailDto;
import com.example.testservice.dto.admin.TestSeriesListDto;
import com.example.testservice.dto.admin.TestSeriesUpdateDto;
import com.example.testservice.dto.common.PageMetaDto;
import com.example.testservice.dto.common.PaginatedResponseDto;
import com.example.testservice.entity.Category;
import com.example.testservice.entity.Status;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.exception.ResourceConflictException;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.CategoryRepository;
import com.example.testservice.repository.MockTestRepository;
import com.example.testservice.repository.TestSeriesRepository;
import com.example.testservice.service.KafkaPublisherService;
import com.example.testservice.service.SlugService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTestSeriesServiceImpl {

    private final TestSeriesRepository testSeriesRepository;
    private final MockTestRepository mockTestRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaPublisherService kafkaPublisherService;
    private final SlugService slugService;

    @Transactional
    @CacheEvict(value = {"baahubali:test:series:list", "baahubali:test:homepage:series:popular"}, allEntries = true)
    public UUID createSeries(TestSeriesCreateDto dto, String adminId) {
        Category category = null;

        if (dto.categoryId() != null) {
            category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        TestSeries series = new TestSeries();
        series.setTitle(dto.title());
        series.setSlug(slugService.generateUniqueSlug(dto.title(), testSeriesRepository::existsBySlug));
        series.setBasePrice(dto.basePrice());
        series.setCategory(category);
        series.setStatus(Status.DRAFT);
        series.setCreatedBy(adminId);
        series.setUpdatedBy(adminId);

        TestSeries saved = testSeriesRepository.save(series);

        kafkaPublisherService.emitAuditEvent(new AuditEvent(
                UUID.randomUUID(),
                adminId,
                "ADMIN",
                "TEST_SERIES_CREATED",
                "TEST_SERIES",
                saved.getId().toString(),
                "test-service",
                "/admin/series",
                "POST",
                201,
                null,
                Map.of(),
                Instant.now()
        ));

        return saved.getId();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "baahubali:test:series", key = "#seriesId"),
            @CacheEvict(value = "baahubali:test:series:list", allEntries = true),
            @CacheEvict(value = "baahubali:test:homepage:series:popular", allEntries = true)
    })
    public TestSeriesDetailDto deleteSeries(UUID seriesId) {
        TestSeries series = testSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Series not found"));

        boolean hasPublishedTests = series.getMockTests().stream()
                .anyMatch(test -> test.getStatus() == Status.PUBLISHED);

        if (hasPublishedTests) {
            throw new ResourceConflictException(
                    "Cannot delete series containing PUBLISHED tests. Archive or revert tests to DRAFT first."
            );
        }

        Instant deletedAt = Instant.now();
        series.setDeletedAt(deletedAt);
        series.getMockTests().forEach(test -> test.setDeletedAt(deletedAt));

        testSeriesRepository.save(series);
        return getSeriesById(seriesId);
    }

    @Transactional(readOnly = true)
    public PaginatedResponseDto<TestSeriesListDto> getSeriesList(
            String search,
            String statusStr,
            UUID categoryId,
            int page,
            int limit) {

        org.springframework.data.jpa.domain.Specification<TestSeries> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("title")),
                                "%" + search.toLowerCase() + "%"
                        )
                );
            }

            if (statusStr != null && !statusStr.isBlank()) {
                predicates.add(
                        cb.equal(
                                root.get("status"),
                                Status.valueOf(statusStr.toUpperCase())
                        )
                );
            }

            if (categoryId != null) {
                predicates.add(
                        cb.equal(root.get("category").get("id"), categoryId)
                );
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        var pageable = org.springframework.data.domain.PageRequest.of(
                page - 1,
                limit,
                org.springframework.data.domain.Sort.by("createdAt").descending()
        );

        var seriesPage = testSeriesRepository.findAll(spec, pageable);

        var data = seriesPage.getContent().stream()
                .map(s -> new TestSeriesListDto(
                        s.getId(),
                        s.getSlug(),
                        s.getTitle(),
                        s.getBasePrice(),
                        s.getStatus(),
                        s.getCategory() != null ? s.getCategory().getName() : null,
                        s.getCreatedAt(),
                        s.getUpdatedAt(),
                        s.getDeletedAt()
                ))
                .toList();

        return new PaginatedResponseDto<>(
                data,
                new PageMetaDto(
                        page,
                        limit,
                        seriesPage.getTotalElements(),
                        seriesPage.getTotalPages()
                )
        );
    }

    @Transactional(readOnly = true)
    public TestSeriesDetailDto getSeriesById(UUID id) {
        TestSeries series = testSeriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test Series not found"));

        return mapDetail(series);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "baahubali:test:series", key = "#id"),
            @CacheEvict(value = "baahubali:test:series:list", allEntries = true),
            @CacheEvict(value = "baahubali:test:homepage:series:popular", allEntries = true)
    })
    public TestSeriesDetailDto updateSeries(
            UUID id,
            TestSeriesUpdateDto dto,
            String adminId) {

        TestSeries series = testSeriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test Series not found"));

        Category category = null;

        if (dto.categoryId() != null) {
            category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        BigDecimal oldPrice = series.getBasePrice();
        BigDecimal newPrice = dto.basePrice();

        series.setTitle(dto.title());
        series.setBasePrice(newPrice);
        series.setCategory(category);
        series.setUpdatedBy(adminId);

        testSeriesRepository.save(series);

        if (oldPrice == null || newPrice == null || oldPrice.compareTo(newPrice) != 0) {
            boolean isFree = newPrice != null && newPrice.compareTo(BigDecimal.ZERO) == 0;
            Instant updatedAt = Instant.now();

            mockTestRepository.updateFreeStatusBySeriesId(
                    id,
                    isFree,
                    updatedAt
            );
        }

        return getSeriesById(id);
    }

    private TestSeriesDetailDto mapDetail(TestSeries series) {
        var mockTests = series.getMockTests()
                .stream()
                .map(test -> new TestSeriesDetailDto.MockTestSummaryDto(
                        test.getId(),
                        test.getSlug(),
                        test.getTitle(),
                        test.getStatus(),
                        test.getDurationMinutes(),
                        test.getTotalMarks(),
                        test.isFree(),
                        test.getCreatedAt(),
                        test.getUpdatedAt(),
                        test.getDeletedAt()
                ))
                .toList();

        return new TestSeriesDetailDto(
                series.getId(),
                series.getSlug(),
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
}