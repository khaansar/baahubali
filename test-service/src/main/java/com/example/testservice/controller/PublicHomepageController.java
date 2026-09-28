package com.example.testservice.controller;

import com.example.testservice.dto.ApiResponse;
import com.example.testservice.dto.publiccatalog.FeaturedMockTestDto;
import com.example.testservice.dto.publiccatalog.PopularSeriesDto;
import com.example.testservice.dto.publiccatalog.PublicCategoryDto;
import com.example.testservice.service.publiccatalog.impl.PublicHomepageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicHomepageController {

    private final PublicHomepageService publicHomepageService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<PublicCategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(publicHomepageService.getCategories()));
    }

    @GetMapping("/series/popular")
    public ResponseEntity<ApiResponse<List<PopularSeriesDto>>> getPopularSeries() {
        return ResponseEntity.ok(ApiResponse.success(publicHomepageService.getPopularSeries()));
    }

    @GetMapping("/mock-tests/featured")
    public ResponseEntity<ApiResponse<List<FeaturedMockTestDto>>> getFeaturedMockTests() {
        return ResponseEntity.ok(ApiResponse.success(publicHomepageService.getFeaturedMockTests()));
    }
}
