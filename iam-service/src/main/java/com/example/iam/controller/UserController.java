package com.example.iam.controller;

import com.example.iam.dto.ApiResponse;
import com.example.iam.dto.UserResponse;
import com.example.iam.entity.User;
import com.example.iam.exception.InvalidCredentialsException;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

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
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<UserResponse> users = userRepository.findAll(pageable)
                .map(UserResponse::from);
        return ApiResponse.success(HttpStatus.OK.value(), "Users retrieved successfully", users);
    }
}
