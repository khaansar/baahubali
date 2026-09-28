package com.example.attemptservice.controller;

import com.example.attemptservice.dto.ApiResponse;
import com.example.attemptservice.dto.InProgressAttemptDto;
import com.example.attemptservice.dto.StreakResponseDto;
import com.example.attemptservice.service.AttemptDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AttemptDashboardController {

    private final AttemptDashboardService dashboardService;

    @GetMapping("/attempts/in-progress")
    public ResponseEntity<ApiResponse<InProgressAttemptDto>> getInProgressAttempt(
            @RequestHeader("X-User-Id") String userId) {
        InProgressAttemptDto response = dashboardService.getInProgressAttempt(userId);
        return ResponseEntity.ok(ApiResponse.success(200, "Success", response));
    }

    @GetMapping("/streak/yearly")
    public ResponseEntity<ApiResponse<StreakResponseDto>> getYearlyStreak(
            @RequestHeader("X-User-Id") String userId) {
        StreakResponseDto response = dashboardService.getYearlyStreak(userId);
        return ResponseEntity.ok(ApiResponse.success(200, "Success", response));
    }
}
