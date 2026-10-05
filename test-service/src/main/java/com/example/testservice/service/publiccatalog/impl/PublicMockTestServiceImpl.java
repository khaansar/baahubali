package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.dto.common.PageMetaDto;
import com.example.testservice.dto.common.PaginatedResponseDto;
import com.example.testservice.dto.publiccatalog.PublicMockTestListDto;
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
            key = "'cat=' + (#categoryId != null ? #categoryId : 'all') + ':q=' + (#query != null ? #query : 'none') + ':page=' + #page + ':limit=' + #limit"
    )
    @Transactional(readOnly = true)
    public PaginatedResponseDto<PublicMockTestListDto> getPublishedMockTests(
            UUID categoryId,
            String query,
            int page,
            int limit) {

        Specification<MockTest> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Must be PUBLISHED
            predicates.add(cb.equal(root.get("status"), Status.PUBLISHED));
            
            // Join TestSeries and Category to filter by category
            Join<Object, Object> seriesJoin = root.join("series", JoinType.INNER);
            Join<Object, Object> categoryJoin = seriesJoin.join("category", JoinType.INNER);

            if (categoryId != null) {
                predicates.add(cb.equal(categoryJoin.get("id"), categoryId));
            }
            
            // Search query
            if (StringUtils.hasText(query)) {
                String likePattern = "%" + query.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), likePattern);
                Predicate seriesMatch = cb.like(cb.lower(seriesJoin.get("title")), likePattern);
                predicates.add(cb.or(titleMatch, seriesMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(
                Math.max(page - 1, 0),
                Math.min(limit, 100),
                Sort.by(Sort.Direction.DESC, "publishedAt")
        );

        Page<MockTest> mockTestPage = mockTestRepository.findAll(spec, pageRequest);

        List<PublicMockTestListDto> dtoList = mockTestPage.getContent().stream()
                .map(this::mapToListDto)
                .collect(Collectors.toList());

        PageMetaDto meta = new PageMetaDto(
                page,
                limit,
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
}
