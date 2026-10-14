package com.example.payment.controller;

import com.example.payment.dto.ApiResponse;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.service.EntitlementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/entitlements")
@RequiredArgsConstructor
public class InternalEntitlementController {

    private final EntitlementService service;

    public record CheckRequest(
            @NotNull UUID userId,
            @NotNull UUID testId
    ) {}

    public record SeriesCheckRequest(
            @NotNull UUID userId,
            @NotNull UUID seriesId
    ) {}

    public record CheckResponse(boolean allowed) {}

    public record BatchRequest(
            @NotNull UUID userId,
            @NotEmpty @Size(max = 200) List<UUID> testIds
    ) {}

    @PostMapping("/check")
    public ApiResponse<CheckResponse> check(
            @Valid @RequestBody CheckRequest request
    ) {
        return ApiResponse.ok(
                new CheckResponse(
                        service.canAccessTest(
                                request.userId(),
                                request.testId()
                        )
                )
        );
    }

    @PostMapping("/check-batch")
    public ApiResponse<Map<UUID, Boolean>> batch(
            @Valid @RequestBody BatchRequest request
    ) {
        Map<UUID, Boolean> result = new LinkedHashMap<>();

        request.testIds().forEach(id ->
                result.put(
                        id,
                        service.canAccessTest(request.userId(), id)
                )
        );

        return ApiResponse.ok(result);
    }

    @PostMapping("/check-series")
    public ApiResponse<CheckResponse> checkSeries(
            @Valid @RequestBody SeriesCheckRequest request
    ) {
        boolean allowed = service.hasActive(
                request.userId(),
                ProductType.TEST_SERIES,
                request.seriesId()
        );

        return ApiResponse.ok(new CheckResponse(allowed));
    }
}