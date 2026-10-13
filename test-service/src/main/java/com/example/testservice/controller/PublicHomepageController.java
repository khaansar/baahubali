package com.example.testservice.controller;

import com.example.common.cache.annotation.CacheRefreshable;
import com.example.testservice.dto.ApiResponse;
import com.example.testservice.dto.CategoryDto;
import com.example.testservice.dto.publiccatalog.FeaturedMockTestDto;
import com.example.testservice.dto.publiccatalog.PopularSeriesDto;
import com.example.testservice.dto.publiccatalog.PublicCategoryDto;
import com.example.testservice.service.CategoryService;
import com.example.testservice.service.publiccatalog.impl.PublicHomepageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicHomepageController {

    private final PublicHomepageService publicHomepageService;
    private final CategoryService categoryService;

    @CacheRefreshable (cacheNames = "baahubali:test:categories")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<PublicCategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(publicHomepageService.getCategories()));
    }

    @GetMapping("/categories/{slug}")
    public ResponseEntity<ApiResponse<CategoryDto>> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryBySlug(slug)));
    }

    @CacheRefreshable(cacheNames = "baahubali:test:homepage:series:popular")
    @GetMapping("/series/popular")
    public ResponseEntity<ApiResponse<List<PopularSeriesDto>>> getPopularSeries() {
        return ResponseEntity.ok(ApiResponse.success(publicHomepageService.getPopularSeries()));
    }

    @CacheRefreshable(cacheNames = "baahubali:test:homepage:mock-tests:featured")
    @GetMapping("/mock-tests/featured")
    public ResponseEntity<ApiResponse<List<FeaturedMockTestDto>>> getFeaturedMockTests() {
        return ResponseEntity.ok(ApiResponse.success(publicHomepageService.getFeaturedMockTests()));
    }
}