package com.example.testservice.controller;

import com.example.testservice.dto.ApiResponse;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.repository.TestSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/series")
@RequiredArgsConstructor
public class InternalSeriesLookupController {

    private final TestSeriesRepository testSeriesRepository;

    public record SeriesNameDto(
            UUID seriesId,
            String name
    ) {}

    @PostMapping("/names")
    public ResponseEntity<ApiResponse<List<SeriesNameDto>>> getSeriesNames(
            @RequestBody List<UUID> seriesIds
    ) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success(
                            HttpStatus.OK.value(),
                            "Series names retrieved successfully",
                            List.of()
                    )
            );
        }

        List<SeriesNameDto> result =
                testSeriesRepository.findAllById(
                        seriesIds.stream().distinct().toList()
                ).stream()
                .map(series -> new SeriesNameDto(
                        series.getId(),
                        series.getTitle()
                ))
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Series names retrieved successfully",
                        result
                )
        );
    }
}