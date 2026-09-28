package com.example.attemptservice.controller.internal;

import com.example.attemptservice.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/attempts")
@RequiredArgsConstructor
@Tag(name = "Internal Attempt API", description = "Internal service-to-service endpoints for the Attempt Engine")
public class InternalAttemptController {

    private final AttemptService attemptService;

    @Operation(summary = "Verify if a user has completed a specific test (Internal)")
    @GetMapping("/verify")
    public ResponseEntity<Boolean> hasUserAttemptedTest(
            @RequestParam("userId") String userId,
            @RequestParam("testId") String testId) {
        
        boolean hasAttempted = attemptService.hasUserAttemptedTest(userId, testId);
        return ResponseEntity.ok(hasAttempted);
    }
}