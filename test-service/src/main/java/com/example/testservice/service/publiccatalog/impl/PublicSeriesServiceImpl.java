package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.dto.common.PageMetaDto;
import com.example.testservice.dto.common.PaginatedResponseDto;
import com.example.testservice.dto.publiccatalog.PublicTestSeriesDetailDto;
import com.example.testservice.dto.publiccatalog.PublicTestSeriesListDto;
import com.example.testservice.entity.Status;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.TestSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.example.testservice.entity.MockTest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicSeriesServiceImpl {

    private final TestSeriesRepository testSeriesRepository;

    @Cacheable(
            value = "baahubali:test:series:list",
            key = "'cat=' + (#categoryId != null ? #categoryId : 'all')"
                    + " + ':page=' + T(java.lang.Math).max(#page, 1)"
                    + " + ':limit=' + T(java.lang.Math).max(1, T(java.lang.Math).min(#limit, 100))"
    )
    @Transactional(readOnly = true)
    public PaginatedResponseDto<PublicTestSeriesListDto> getPublishedSeries(
            UUID categoryId,
            int page,
            int limit) {

        int effectivePage = Math.max(page, 1);
        int effectiveLimit = Math.max(1, Math.min(limit, 100));

        Specification<TestSeries> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    cb.equal(root.get("status"), Status.PUBLISHED)
            );
            predicates.add(cb.isNull(root.get("deletedAt")));

            predicates.add(cb.isNotNull(root.get("category")));
            predicates.add(
                    cb.isNull(root.get("category").get("deletedAt"))
            );

            if (categoryId != null) {
                predicates.add(
                        cb.equal(root.get("category").get("id"), categoryId)
                );
            }

            jakarta.persistence.criteria.Subquery<Integer> subquery =
                    query.subquery(Integer.class);

            jakarta.persistence.criteria.Root<MockTest> mockTestRoot =
                    subquery.from(MockTest.class);

            subquery.select(cb.literal(1))
                    .where(
                            cb.equal(
                                    mockTestRoot.get("series").get("id"),
                                    root.get("id")
                            ),
                            cb.equal(
                                    mockTestRoot.get("status"),
                                    Status.PUBLISHED
                            ),
                            cb.isNull(mockTestRoot.get("deletedAt"))
                    );

            predicates.add(cb.exists(subquery));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(
                effectivePage - 1,
                effectiveLimit,
                Sort.by("createdAt").descending()
        );

        Page<TestSeries> seriesPage =
                testSeriesRepository.findAll(spec, pageable);

        List<PublicTestSeriesListDto> data = seriesPage.getContent()
                .stream()
                .map(series -> new PublicTestSeriesListDto(
                        series.getId(),
                        series.getSlug(),
                        series.getTitle(),
                        series.getBasePrice(),
                        series.getCategory().getName(),
                        series.getCreatedAt(),
                        series.getUpdatedAt(),
                        series.getDeletedAt()
                ))
                .collect(Collectors.toList());

        return new PaginatedResponseDto<>(
                data,
                new PageMetaDto(
                        effectivePage,
                        effectiveLimit,
                        seriesPage.getTotalElements(),
                        seriesPage.getTotalPages()
                )
        );
    }
    @Transactional(readOnly = true)
    @Cacheable(value = "baahubali:test:series", key = "#id")
    public PublicTestSeriesDetailDto getSeriesById(UUID id) {
        TestSeries series = getPublishedSeriesEntityById(id);
        return mapSeries(series);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "baahubali:test:series", key = "'slug:' + #slug")
    public PublicTestSeriesDetailDto getSeriesBySlug(String slug) {
        TestSeries series = testSeriesRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Test Series not found"));

        validatePublicVisibility(series);
        return mapSeries(series);
    }

    private TestSeries getPublishedSeriesEntityById(UUID id) {
        TestSeries series = testSeriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test Series not found"));

        validatePublicVisibility(series);
        return series;
    }

    private void validatePublicVisibility(TestSeries series) {
        if (series.getStatus() != Status.PUBLISHED
                || series.getDeletedAt() != null) {
            throw new ResourceNotFoundException(
                    "Test Series is not available in the public catalog."
            );
        }

        if (series.getCategory() == null
                || series.getCategory().getDeletedAt() != null) {
            throw new ResourceNotFoundException(
                    "Test Series is not available in the public catalog."
            );
        }

        boolean hasPublishedTests = series.getMockTests()
                .stream()
                .anyMatch(test ->
                        test.getStatus() == Status.PUBLISHED
                                && test.getDeletedAt() == null
                );

        if (!hasPublishedTests) {
            throw new ResourceNotFoundException(
                    "Test Series is not available in the public catalog."
            );
        }
    }

    private PublicTestSeriesDetailDto mapSeries(TestSeries series) {
        List<PublicTestSeriesDetailDto.PublicMockTestSummaryDto> mockTests =
                series.getMockTests()
                    .stream()
                    .filter(test -> test.getStatus() == Status.PUBLISHED)
                    .filter(test -> test.getDeletedAt() == null)
                        .map(test -> new PublicTestSeriesDetailDto.PublicMockTestSummaryDto(
                                test.getId(),
                                test.getSlug(),
                                test.getTitle(),
                                test.getDurationMinutes(),
                                test.getTotalMarks(),
                                test.isFree(),
                                test.getCreatedAt(),
                                test.getUpdatedAt(),
                                test.getDeletedAt()
                        ))
                        .collect(Collectors.toList());

        return new PublicTestSeriesDetailDto(
                series.getId(),
                series.getSlug(),
                series.getTitle(),
                series.getBasePrice(),
                series.getCategory() != null ? series.getCategory().getId() : null,
                series.getCategory() != null ? series.getCategory().getSlug() : null,
                series.getCategory() != null ? series.getCategory().getName() : null,
                series.getCreatedAt(),
                series.getUpdatedAt(),
                series.getDeletedAt(),
                mockTests
        );
    }
}