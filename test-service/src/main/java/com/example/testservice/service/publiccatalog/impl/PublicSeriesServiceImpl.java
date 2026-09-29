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

    @Cacheable(value = "baahubali:test:series:list", key = "'cat=' + (#categoryId != null ? #categoryId : 'all') + ':page=' + #page + ':limit=' + #limit")
    @Transactional(readOnly = true)
    public PaginatedResponseDto<PublicTestSeriesListDto> getPublishedSeries(UUID categoryId, int page, int limit) {
        
        Specification<TestSeries> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Only published series
            predicates.add(cb.equal(root.get("status"), Status.PUBLISHED));
            
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<TestSeries> seriesPage = testSeriesRepository.findAll(spec, pageable);

        List<PublicTestSeriesListDto> data = seriesPage.getContent().stream().map(s -> new PublicTestSeriesListDto(
                s.getId(),
                s.getTitle(),
                s.getBasePrice(),
                s.getCategory() != null ? s.getCategory().getName() : null,
                s.getCreatedAt(), s.getUpdatedAt(), s.getDeletedAt()
        )).collect(Collectors.toList());

        return new PaginatedResponseDto<>(data, new PageMetaDto(
                page, limit, seriesPage.getTotalElements(), seriesPage.getTotalPages()));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "baahubali:test:series", key = "#id")
    public PublicTestSeriesDetailDto getSeriesById(UUID id) {
        TestSeries series = testSeriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test Series not found"));

        if (series.getStatus() != Status.PUBLISHED) {
            throw new ResourceNotFoundException("Test Series is not available in the public catalog.");
        }

        List<PublicTestSeriesDetailDto.PublicMockTestSummaryDto> mockTests = series.getMockTests().stream()
                .filter(test -> test.getStatus() == Status.PUBLISHED)
                .map(test -> new PublicTestSeriesDetailDto.PublicMockTestSummaryDto(
                        test.getId(),
                        test.getTitle(),
                        test.getDurationMinutes(),
                        test.getTotalMarks(),
                        test.isFree(),
                        test.getCreatedAt(), test.getUpdatedAt(), test.getDeletedAt()
                )).collect(Collectors.toList());

        return new PublicTestSeriesDetailDto(
                series.getId(),
                series.getTitle(),
                series.getBasePrice(),
                series.getCategory() != null ? series.getCategory().getId() : null,
                series.getCategory() != null ? series.getCategory().getName() : null,
                series.getCreatedAt(), series.getUpdatedAt(), series.getDeletedAt(),
                mockTests
        );
    }
}
