package com.example.testservice.controller;

import com.example.testservice.dto.ApiResponse;
import com.example.testservice.dto.common.PaginatedResponseDto;
import com.example.testservice.dto.publiccatalog.PublicTestSeriesDetailDto;
import com.example.testservice.dto.publiccatalog.PublicTestSeriesListDto;
import com.example.testservice.service.publiccatalog.impl.PublicSeriesServiceImpl;
import com.example.common.cache.annotation.CacheRefreshable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/public/series")
@RequiredArgsConstructor
public class PublicSeriesController {

    private final PublicSeriesServiceImpl publicSeriesService;

    @CacheRefreshable(
            cacheNames = "baahubali:test:series:list",
            keyExpression = "'cat=' + (#arg0 != null ? #arg0 : 'all')"
        + " + ':page=' + T(java.lang.Math).max(#arg1, 1)"
        + " + ':limit=' + T(java.lang.Math).max(1, T(java.lang.Math).min(#arg2, 100))"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<PublicTestSeriesListDto>>> getPublishedSeries(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        PaginatedResponseDto<PublicTestSeriesListDto> response =
                publicSeriesService.getPublishedSeries(categoryId, page, limit);

        return ResponseEntity.ok(ApiResponse.paginated(response.data(), response.meta()));
    }

    @CacheRefreshable(
            cacheNames = "baahubali:test:series",
            keyExpression = "#arg0"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicTestSeriesDetailDto>> getSeriesById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(publicSeriesService.getSeriesById(id)));
    }

    @CacheRefreshable(
            cacheNames = "baahubali:test:series",
            keyExpression = "'slug:' + #arg0"
    )
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<PublicTestSeriesDetailDto>> getSeriesBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(publicSeriesService.getSeriesBySlug(slug)));
    }
}
