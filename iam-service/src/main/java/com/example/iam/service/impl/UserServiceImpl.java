package com.example.iam.service.impl;

import com.example.iam.dto.ChangePasswordRequest;
import com.example.iam.dto.UpdateUserProfileRequest;
import com.example.iam.dto.UserResponse;
import com.example.iam.entity.User;
import com.example.iam.exception.InvalidCredentialsException;
import com.example.iam.repository.UserRepository;
import com.example.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (request.firstName() != null) {
            String trimmed = request.firstName().trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("First name cannot be empty");
            }
            user.setFirstName(trimmed);
        }

        if (request.lastName() != null) {
            String trimmed = request.lastName().trim();
            user.setLastName(trimmed.isEmpty() ? null : trimmed);
        }

        if (request.phone() != null) {
            String trimmed = request.phone().trim();
            user.setPhone(trimmed.isEmpty() ? null : trimmed);
        }

        if (request.targetExam() != null) {
            String trimmed = request.targetExam().trim();
            user.setTargetExam(trimmed.isEmpty() ? null : trimmed);
        }

        if (Boolean.TRUE.equals(request.removeAvatar())) {
            user.setAvatarUrl(null);
        } else if (request.avatarUrl() != null) {
            String trimmed = request.avatarUrl().trim();
            user.setAvatarUrl(trimmed.isEmpty() ? null : trimmed);
        }

        User updatedUser = userRepository.save(user);
        return UserResponse.from(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse removeAvatar(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        user.setAvatarUrl(null);
        User updatedUser = userRepository.save(user);
        return UserResponse.from(updatedUser);
    }
}
