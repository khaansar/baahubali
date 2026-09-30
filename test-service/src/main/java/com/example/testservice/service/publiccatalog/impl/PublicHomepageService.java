package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.dto.publiccatalog.FeaturedMockTestDto;
import com.example.testservice.dto.publiccatalog.PopularSeriesDto;
import com.example.testservice.dto.publiccatalog.PublicCategoryDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicHomepageService {

    private final EntityManager entityManager;

    @Cacheable(value = "baahubali:test:categories")
    @Transactional(readOnly = true)
    public List<PublicCategoryDto> getCategories() {
        String jpql =
            "SELECT c.id, c.name, " +
            "(SELECT COUNT(m.id) " +
            " FROM MockTest m JOIN m.series s " +
            " WHERE s.category.id = c.id " +
            " AND s.status = 'PUBLISHED' " +
            " AND m.status = 'PUBLISHED') " +
            "FROM Category c " +
            "WHERE EXISTS (" +
            " SELECT m2.id " +
            " FROM MockTest m2 JOIN m2.series s2 " +
            " WHERE s2.category.id = c.id " +
            " AND s2.status = 'PUBLISHED' " +
            " AND m2.status = 'PUBLISHED'" +
            ")";

        List<Object[]> results = entityManager.createQuery(jpql, Object[].class).getResultList();

        return results.stream().map(row -> {
            String id = row[0] != null ? row[0].toString() : null;
            String name = (String) row[1];
            Long testCount = ((Number) row[2]).longValue();

            String slug = name != null
                    ? name.toLowerCase().replace(" ", "-")
                    : null;

            return new PublicCategoryDto(id, name, slug, testCount);
        }).collect(Collectors.toList());
    }

    @Cacheable(value = "baahubali:test:homepage:series:popular")
    @Transactional(readOnly = true)
    public List<PopularSeriesDto> getPopularSeries() {
        String jpql = "SELECT ts.id, ts.slug, ts.title, " +
                "(SELECT COUNT(m.id) FROM MockTest m WHERE m.series.id = ts.id), " +
                "(SELECT SUM(m.durationMinutes) FROM MockTest m WHERE m.series.id = ts.id) " +
                "FROM TestSeries ts " +
                "WHERE ts.status = 'PUBLISHED' " +
                "ORDER BY (SELECT COUNT(m2.id) FROM MockTest m2 WHERE m2.series.id = ts.id) DESC";

        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setMaxResults(4)
                .getResultList();

        return results.stream().map(row -> {
            String id = row[0] != null ? row[0].toString() : null;
            String slug = (String) row[1];
            String title = (String) row[2];
            Long testCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            Integer durationMinutes = row[4] != null ? ((Number) row[4]).intValue() : 0;
            String badge = testCount > 5 ? "Popular" : "New";
            String thumbnailUrl = "/images/home/series-gate.webp";
            return new PopularSeriesDto(id, slug, title, badge, testCount, durationMinutes, thumbnailUrl);
        }).collect(Collectors.toList());
    }

    @Cacheable(value = "baahubali:test:homepage:mock-tests:featured")
    @Transactional(readOnly = true)
    public List<FeaturedMockTestDto> getFeaturedMockTests() {
        String jpql = "SELECT m.id, m.slug, m.title, c.name, m.durationMinutes, m.totalMarks " +
                "FROM MockTest m " +
                "JOIN m.series s " +
                "JOIN s.category c " +
                "WHERE m.status = 'PUBLISHED' " +
                "ORDER BY m.createdAt DESC";

        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setMaxResults(4)
                .getResultList();

        return results.stream().map(row -> {
            String id = row[0] != null ? row[0].toString() : null;
            String slug = (String) row[1];
            String title = (String) row[2];
            String categoryName = (String) row[3];
            Integer durationMinutes = row[4] != null ? ((Number) row[4]).intValue() : 0;
            java.math.BigDecimal totalMarks = (java.math.BigDecimal) row[5];
            return new FeaturedMockTestDto(id, slug, title, categoryName, durationMinutes, totalMarks);
        }).collect(Collectors.toList());
    }
}