package com.example.testservice.controller;

import com.example.testservice.dto.ApiResponse;
import com.example.testservice.dto.common.PaginatedResponseDto;
import com.example.testservice.dto.publiccatalog.PublicTestSeriesDetailDto;
import com.example.testservice.dto.publiccatalog.PublicTestSeriesListDto;
import com.example.testservice.service.publiccatalog.impl.PublicSeriesServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/public/series")
@RequiredArgsConstructor
public class PublicSeriesController {

    private final PublicSeriesServiceImpl publicSeriesService;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<PublicTestSeriesListDto>>> getPublishedSeries(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        PaginatedResponseDto<PublicTestSeriesListDto> response =
                publicSeriesService.getPublishedSeries(categoryId, page, limit);

        return ResponseEntity.ok(ApiResponse.paginated(response.data(), response.meta()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicTestSeriesDetailDto>> getSeriesById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(publicSeriesService.getSeriesById(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<PublicTestSeriesDetailDto>> getSeriesBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(publicSeriesService.getSeriesBySlug(slug)));
    }
}