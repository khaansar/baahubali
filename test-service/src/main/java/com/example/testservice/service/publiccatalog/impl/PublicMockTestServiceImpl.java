package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.dto.common.PageMetaDto;
import com.example.testservice.dto.common.PaginatedResponseDto;
import com.example.testservice.dto.publiccatalog.PublicMockTestListDto;
import com.example.testservice.dto.publiccatalog.PublicMockTestDetailDto;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.Status;
import com.example.testservice.repository.MockTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicMockTestServiceImpl {

    private final MockTestRepository mockTestRepository;

    @Cacheable(
            value = "baahubali:test:mocktests:list",
            key = "'cat=' + (#categoryId != null ? #categoryId : 'all')"
                    + " + ':q=' + (#query != null ? #query.trim().toLowerCase() : '')"
                    + " + ':page=' + T(java.lang.Math).max(#page, 1)"
                    + " + ':limit=' + T(java.lang.Math).max(1, T(java.lang.Math).min(#limit, 100))"
    )
    @Transactional(readOnly = true)
    public PaginatedResponseDto<PublicMockTestListDto> getPublishedMockTests(
            UUID categoryId,
            String query,
            int page,
            int limit) {

        int effectivePage = Math.max(page, 1);
        int effectiveLimit = Math.max(1, Math.min(limit, 100));
        String normalizedQuery = StringUtils.hasText(query)
                ? query.trim().toLowerCase()
                : null;

        Specification<MockTest> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), Status.PUBLISHED));
            predicates.add(cb.isNull(root.get("deletedAt")));

            Join<Object, Object> seriesJoin =
                    root.join("series", JoinType.INNER);
            Join<Object, Object> categoryJoin =
                    seriesJoin.join("category", JoinType.INNER);

            predicates.add(
                    cb.equal(seriesJoin.get("status"), Status.PUBLISHED)
            );
            predicates.add(cb.isNull(seriesJoin.get("deletedAt")));
            predicates.add(cb.isNull(categoryJoin.get("deletedAt")));

            if (categoryId != null) {
                predicates.add(
                        cb.equal(categoryJoin.get("id"), categoryId)
                );
            }

            if (normalizedQuery != null) {
                String likePattern = "%" + normalizedQuery + "%";

                Predicate titleMatch = cb.like(
                        cb.lower(root.get("title")),
                        likePattern
                );

                Predicate seriesMatch = cb.like(
                        cb.lower(seriesJoin.get("title")),
                        likePattern
                );

                predicates.add(cb.or(titleMatch, seriesMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(
                effectivePage - 1,
                effectiveLimit,
                Sort.by(Sort.Direction.DESC, "publishedAt")
        );

        Page<MockTest> mockTestPage =
                mockTestRepository.findAll(spec, pageRequest);

        List<PublicMockTestListDto> dtoList =
                mockTestPage.getContent()
                        .stream()
                        .map(this::mapToListDto)
                        .collect(Collectors.toList());

        PageMetaDto meta = new PageMetaDto(
                effectivePage,
                effectiveLimit,
                mockTestPage.getTotalElements(),
                mockTestPage.getTotalPages()
        );

        return new PaginatedResponseDto<>(dtoList, meta);
    }

    private PublicMockTestListDto mapToListDto(MockTest mockTest) {
        String seriesName = mockTest.getSeries() != null ? mockTest.getSeries().getTitle() : null;
        String catName = null;
        String catSlug = null;
        
        if (mockTest.getSeries() != null && mockTest.getSeries().getCategory() != null) {
            catName = mockTest.getSeries().getCategory().getName();
            catSlug = mockTest.getSeries().getCategory().getSlug();
        }

        return PublicMockTestListDto.builder()
                .id(mockTest.getId())
                .slug(mockTest.getSlug())
                .title(mockTest.getTitle())
                .type(seriesName) // Map series name to 'type'
                .categoryName(catName)
                .categorySlug(catSlug)
                .durationMinutes(mockTest.getDurationMinutes())
                .totalMarks(mockTest.getTotalMarks())
                .isFree(mockTest.isFree())
                .build();
    }

    
    @Transactional(readOnly = true)
    @Cacheable(value = "baahubali:test:mocktests:detail", key = "'slug:' + #slug")
    public PublicMockTestDetailDto getPublishedMockTestBySlug(String slug) {
        MockTest test = mockTestRepository.findBySlug(slug)
                .filter(mockTest -> mockTest.getStatus() == Status.PUBLISHED)
                .filter(mockTest -> mockTest.getDeletedAt() == null)
                .filter(mockTest -> mockTest.getSeries() != null)
                .filter(mockTest -> mockTest.getSeries().getStatus() == Status.PUBLISHED)
                .filter(mockTest -> mockTest.getSeries().getDeletedAt() == null)
                .filter(mockTest -> mockTest.getSeries().getCategory() != null)
                .filter(mockTest -> mockTest.getSeries().getCategory().getDeletedAt() == null)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Mock test not found"));

        return new PublicMockTestDetailDto(
                test.getId(),
                test.getSlug(),
                test.getTitle(),
                test.getSeries().getSlug(),
                test.getSeries().getTitle(),
                test.getSeries().getCategory().getSlug(),
                test.getSeries().getCategory().getName(),
                test.getDurationMinutes(),
                test.getTotalMarks(),
                test.isFree(),
                test.getWindowStartTime(),
                test.getWindowEndTime(),
                test.getAccessCode() != null && !test.getAccessCode().isBlank()
        );
    }
}
