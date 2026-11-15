package com.example.iam.controller;

import com.example.iam.dto.ApiResponse;
import com.example.iam.dto.UpdateUserStatusRequest;
import com.example.iam.dto.UserResponse;
import com.example.iam.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {

        UUID adminId = (UUID) authentication.getPrincipal();
        UserResponse user = UserResponse.from(adminUserService.setActive(adminId, userId, request.active()));

        return ApiResponse.success(
                HttpStatus.OK.value(),
                request.active() ? "User reactivated" : "User deactivated",
                user
        );
    }
}
