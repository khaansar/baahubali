package com.example.testservice.controller;

import com.example.common.cache.annotation.CacheRefreshable;
import com.example.testservice.dto.ApiResponse;
import com.example.testservice.dto.publiccatalog.PublicMockTestDetailDto;
import com.example.testservice.service.publiccatalog.impl.PublicMockTestServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/mock-tests")
@RequiredArgsConstructor
public class PublicMockTestDetailController {

    private final PublicMockTestServiceImpl publicMockTestService;

    @CacheRefreshable(
            cacheNames = "baahubali:test:mocktests:detail",
            keyExpression = "'slug:' + #arg0"
    )
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<PublicMockTestDetailDto>> getBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        publicMockTestService.getPublishedMockTestBySlug(slug)
                )
        );
    }
}