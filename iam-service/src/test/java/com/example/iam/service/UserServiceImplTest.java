package com.example.iam.service;

import com.example.iam.dto.ChangePasswordRequest;
import com.example.iam.dto.UpdateUserProfileRequest;
import com.example.iam.dto.UserResponse;
import com.example.iam.entity.Role;
import com.example.iam.entity.User;
import com.example.iam.repository.UserRepository;
import com.example.iam.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("1234567890")
                .targetExam("SSC CGL 2025")
                .avatarUrl("https://example.com/avatar.png")
                .passwordHash("$2a$12$hashedPassword")
                .role(Role.STUDENT)
                .emailVerified(true)
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("updateProfile should successfully update profile fields")
    void updateProfile_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Jane",
                "Smith",
                "9876543210",
                "UPSC CSE",
                null,
                false
        );

        UserResponse response = userService.updateProfile(userId, request);

        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Smith");
        assertThat(response.phone()).isEqualTo("9876543210");
        assertThat(response.targetExam()).isEqualTo("UPSC CSE");
        assertThat(response.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateProfile should throw when first name is blank")
    void updateProfile_BlankFirstName_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "   ",
                "Smith",
                null,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("First name cannot be empty");
    }

    @Test
    @DisplayName("updateProfile should remove avatar when removeAvatar is true")
    void updateProfile_RemoveAvatar_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null,
                null,
                null,
                null,
                null,
                true
        );

        UserResponse response = userService.updateProfile(userId, request);

        assertThat(response.avatarUrl()).isNull();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("removeAvatar should set avatarUrl to null")
    void removeAvatar_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.removeAvatar(userId);

        assertThat(response.avatarUrl()).isNull();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("changePassword should succeed when current password matches")
    void changePassword_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("$2a$12$newHashedPassword");

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");

        userService.changePassword(userId, request);

        assertThat(testUser.getPasswordHash()).isEqualTo("$2a$12$newHashedPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("changePassword should throw when current password is wrong")
    void changePassword_WrongCurrentPassword_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", "$2a$12$hashedPassword")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword", "NewPassword123!");

        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    @DisplayName("changePassword should throw when new password is same as current password")
    void changePassword_SamePassword_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("SamePassword123!", "$2a$12$hashedPassword")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest("SamePassword123!", "SamePassword123!");

        assertThatThrownBy(() -> userService.changePassword(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New password must be different from current password");
    }
}
