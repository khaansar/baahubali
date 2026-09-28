package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.dto.publiccatalog.FeaturedMockTestDto;
import com.example.testservice.dto.publiccatalog.PopularSeriesDto;
import com.example.testservice.dto.publiccatalog.PublicCategoryDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicHomepageService {

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<PublicCategoryDto> getCategories() {
        String jpql = "SELECT c.id, c.name, " +
                      "(SELECT COUNT(m.id) FROM MockTest m JOIN m.series s WHERE s.category.id = c.id) " +
                      "FROM Category c";
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class).getResultList();
        
        return results.stream().map(row -> {
            String id = row[0] != null ? row[0].toString() : null;
            String name = (String) row[1];
            String slug = name != null ? name.toLowerCase().replace(" ", "-") : null;
            Long testCount = ((Number) row[2]).longValue();
            return new PublicCategoryDto(id, name, slug, testCount);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PopularSeriesDto> getPopularSeries() {
        // Fetch top 4 series by number of published mock tests
        String jpql = "SELECT ts.id, ts.title, " +
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
            String title = (String) row[1];
            Long testCount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            Integer durationMinutes = row[3] != null ? ((Number) row[3]).intValue() : 0;
            String badge = testCount > 5 ? "Popular" : "New";
            String thumbnailUrl = "/images/home/series-gate.webp"; // hardcoded fallback for now
            return new PopularSeriesDto(id, title, badge, testCount, durationMinutes, thumbnailUrl);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeaturedMockTestDto> getFeaturedMockTests() {
        String jpql = "SELECT m.id, m.title, c.name, m.durationMinutes, m.totalMarks " +
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
            String title = (String) row[1];
            String categoryName = (String) row[2];
            Integer durationMinutes = row[3] != null ? ((Number) row[3]).intValue() : 0;
            java.math.BigDecimal totalMarks = (java.math.BigDecimal) row[4];
            return new FeaturedMockTestDto(id, title, categoryName, durationMinutes, totalMarks);
        }).collect(Collectors.toList());
    }
}
