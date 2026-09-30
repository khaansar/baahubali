package com.example.iam.controller;

import com.example.iam.dto.ApiResponse;
import com.example.iam.dto.UserResponse;
import com.example.iam.entity.User;
import com.example.iam.entity.Role;
import com.example.iam.exception.InvalidCredentialsException;
import com.example.iam.repository.UserRepository;
import com.example.iam.repository.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

import com.example.iam.service.CalendarAnalyticsService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final CalendarAnalyticsService calendarAnalyticsService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);
                
        UserResponse data = UserResponse.from(user);
        return ApiResponse.success(HttpStatus.OK.value(), "User retrieved successfully", data);
    }

    @GetMapping
    public ApiResponse<org.springframework.data.domain.Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(required = false) Integer minTestsAttemptedCount,
            @RequestParam(required = false) Integer maxTestsAttemptedCount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant updatedBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant deletedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant deletedBefore,
            org.springframework.data.domain.Pageable pageable) {
        var filters = UserSpecifications.buildFilter(
                search, firstName, lastName, email, role, isActive, isDeleted,
                minTestsAttemptedCount, maxTestsAttemptedCount,
                createdAfter, createdBefore, updatedAfter, updatedBefore, deletedAfter, deletedBefore);
        org.springframework.data.domain.Page<UserResponse> users = userRepository.findAll(filters, pageable)
                .map(UserResponse::from);
        return ApiResponse.success(HttpStatus.OK.value(), "Users retrieved successfully", users);
    }

    @GetMapping("/calendar")
    public ApiResponse<com.example.iam.dto.CalendarAnalyticsResponse> getCalendarAnalytics(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        com.example.iam.dto.CalendarAnalyticsResponse data = calendarAnalyticsService.getCalendarAnalytics(userId.toString(), year, month);
        return ApiResponse.success(HttpStatus.OK.value(), "Calendar analytics retrieved successfully", data);
    }
}
