package com.example.attemptservice.controller.internal;

import com.example.attemptservice.dto.ApiResponse;
import com.example.attemptservice.dto.FinalScoreUpdateRequest;
import com.example.attemptservice.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/attempts")
@RequiredArgsConstructor
@Tag(name = "Internal Attempt API", description = "Internal service-to-service endpoints for the Attempt Engine")
public class InternalAttemptController {

    private final AttemptService attemptService;

    @Operation(summary = "Verify if a user has completed a specific test (Internal)")
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> hasUserAttemptedTest(
            @RequestParam("userId") String userId,
            @RequestParam("testId") String testId) {
        
        boolean hasAttempted = attemptService.hasUserAttemptedTest(userId, testId);
        return ResponseEntity.ok(ApiResponse.success(
                200, "Attempt verification completed successfully", Map.of("hasAttempted", hasAttempted)));
    }

    @Operation(summary = "Store the calculated final score for an attempt (Internal)")
    @PutMapping("/{attemptId}/final-score")
    public ResponseEntity<ApiResponse<Map<String, Double>>> updateFinalScore(
            @PathVariable String attemptId,
            @Valid @RequestBody FinalScoreUpdateRequest request) {
        attemptService.updateFinalScore(attemptId, request.getFinalScore());
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(), "Attempt final score updated successfully",
                Map.of("finalScore", request.getFinalScore())));
    }

}
