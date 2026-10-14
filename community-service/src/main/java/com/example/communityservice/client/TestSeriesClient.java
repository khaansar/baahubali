package com.example.communityservice.client;

import com.example.communityservice.config.CommunityFeignConfig;
import com.example.communityservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "test-service",
        url = "${internal.services.test-url}",
        configuration = CommunityFeignConfig.class
)
public interface TestSeriesClient {

    @PostMapping("/internal/series/names")
    ApiResponse<List<SeriesNameDto>> getSeriesNames(
            @RequestBody List<UUID> seriesIds
    );

    @GetMapping("/internal/series/{seriesId}/review-access")
    ApiResponse<SeriesReviewAccessDto> getSeriesReviewAccess(
            @PathVariable("seriesId") UUID seriesId
    );

    record SeriesNameDto(
            UUID seriesId,
            String name
    ) {}

    record SeriesReviewAccessDto(
            UUID seriesId,
            boolean paid
    ) {}
}