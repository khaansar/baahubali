package com.example.iam.service;

import com.example.iam.dto.ChangePasswordRequest;
import com.example.iam.dto.UpdateUserProfileRequest;
import com.example.iam.dto.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse updateProfile(UUID userId, UpdateUserProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    UserResponse removeAvatar(UUID userId);
}
