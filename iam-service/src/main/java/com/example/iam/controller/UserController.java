package com.example.iam.controller;

import com.example.iam.dto.ApiResponse;
import com.example.iam.dto.UserResponse;
import com.example.iam.entity.User;
import com.example.iam.entity.Role;
import com.example.iam.exception.InvalidCredentialsException;
import com.example.iam.service.AvatarUploadService;
import com.example.iam.repository.UserRepository;
import com.example.iam.repository.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.example.iam.dto.ChangePasswordRequest;
import com.example.iam.dto.UpdateAvatarRequest;
import com.example.iam.dto.UpdateUserProfileRequest;
import com.example.iam.service.AvatarProfileService;
import com.example.iam.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.example.iam.service.CalendarAnalyticsService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserRepository userRepository;
    private final CalendarAnalyticsService calendarAnalyticsService;
    private final AvatarUploadService avatarUploadService;
    private final AvatarProfileService avatarProfileService;
    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);
                
        UserResponse data = UserResponse.from(user);
        return ApiResponse.success(HttpStatus.OK.value(), "User retrieved successfully", data);
    }

    @GetMapping
    public ApiResponse<java.util.List<UserResponse>> getAllUsers(
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
        return ApiResponse.paginated(HttpStatus.OK.value(), "Users retrieved successfully", users);
    }

    @GetMapping("/calendar")
    public ApiResponse<com.example.iam.dto.CalendarAnalyticsResponse> getCalendarAnalytics(
            @RequestParam @Min(value = 1, message = "Year must be positive") int year,
            @RequestParam @Min(value = 1, message = "Month must be between 1 and 12")
                    @Max(value = 12, message = "Month must be between 1 and 12") int month,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        com.example.iam.dto.CalendarAnalyticsResponse data = calendarAnalyticsService.getCalendarAnalytics(userId.toString(), year, month);
        return ApiResponse.success(HttpStatus.OK.value(), "Calendar analytics retrieved successfully", data);
    }

    @PostMapping("/me/avatar/upload-intent")
    public ApiResponse<com.example.iam.dto.AvatarUploadIntentResponse> createAvatarUploadIntent(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        var data = avatarUploadService.createUploadIntent(userId);
        return ApiResponse.success(HttpStatus.OK.value(), "Avatar upload intent created successfully", data);
    }

    @PutMapping("/me/avatar")
    public ApiResponse<UserResponse> updateAvatar(
            @jakarta.validation.Valid @RequestBody UpdateAvatarRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = avatarProfileService.updateAvatar(userId, request.avatarUrl());
        UserResponse data = UserResponse.from(user);
        return ApiResponse.success(HttpStatus.OK.value(), "Avatar updated successfully", data);
    }

    @DeleteMapping("/me/avatar")
    public ApiResponse<UserResponse> removeAvatar(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserResponse data = userService.removeAvatar(userId);
        return ApiResponse.success(HttpStatus.OK.value(), "Avatar removed successfully", data);
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateProfile(
            @jakarta.validation.Valid @RequestBody UpdateUserProfileRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserResponse data = userService.updateProfile(userId, request);
        return ApiResponse.success(HttpStatus.OK.value(), "Profile updated successfully", data);
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> patchProfile(
            @jakarta.validation.Valid @RequestBody UpdateUserProfileRequest request,
            Authentication authentication) {
        return updateProfile(request, authentication);
    }

    @PostMapping("/me/change-password")
    public ApiResponse<Map<String, Object>> changePassword(
            @jakarta.validation.Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.changePassword(userId, request);
        return ApiResponse.success(HttpStatus.OK.value(), "Password changed successfully", Map.of());
    }
}
