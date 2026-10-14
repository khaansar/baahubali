package com.example.testservice.controller;

import com.example.testservice.dto.ApiResponse;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.repository.TestSeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/internal/series")
@RequiredArgsConstructor
public class InternalSeriesReviewAccessController {

    private final TestSeriesRepository testSeriesRepository;

    public record SeriesReviewAccessDto(
            UUID seriesId,
            boolean paid
    ) {}

    @GetMapping("/{seriesId}/review-access")
    public ResponseEntity<ApiResponse<SeriesReviewAccessDto>> getReviewAccess(
            @PathVariable UUID seriesId
    ) {
        TestSeries series = testSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Test series not found."
                ));

        BigDecimal price = series.getBasePrice();

        boolean paid = price != null
                && price.compareTo(BigDecimal.ZERO) > 0;

        SeriesReviewAccessDto result = new SeriesReviewAccessDto(
                series.getId(),
                paid
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Test series review access details retrieved successfully.",
                        result
                )
        );
    }
}