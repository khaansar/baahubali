package com.example.iam.service.impl;

import com.example.iam.dto.AuthenticationResult;
import com.example.iam.dto.LoginRequest;
import com.example.iam.dto.RegisterRequest;
import com.example.iam.entity.Role;
import com.example.iam.entity.User;
import com.example.iam.exception.EmailAlreadyExistsException;
import com.example.iam.exception.InvalidCredentialsException;
import com.example.iam.exception.EmailNotVerifiedException;
import com.example.iam.repository.UserRepository;
import com.example.iam.security.JwtService;
import com.example.iam.security.RefreshTokenService;
import com.example.iam.service.AuthService;
import com.example.iam.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate redisTemplate;
    private final EmailVerificationService emailVerificationService;
    private final com.example.iam.event.IamEventPublisher iamEventPublisher;

    @Override
    @Transactional
    public User register(RegisterRequest request) {

        String normalizedEmail = request.email()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(normalizedEmail)
                .phone(request.phone())
                .targetExam(request.targetExam())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.STUDENT)
                .build();

        user = userRepository.save(user);

        emailVerificationService.issueVerificationEmail(user);

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResult authenticate(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailNotVerifiedException();
        }

        String sessionId = jwtService.createSession(user);
        String token = jwtService.generateToken(user, sessionId);
        String refreshToken = refreshTokenService.issue(user, sessionId);
        return new AuthenticationResult(user, token, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResult refresh(String refreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new com.example.iam.exception.InvalidRefreshTokenException());
        String accessToken = jwtService.generateToken(user, rotation.sessionId());
        return new AuthenticationResult(user, accessToken, rotation.refreshToken());
    }

    @Override
    public void logout(String userId) {
        redisTemplate.delete("user:session:" + userId);
        refreshTokenService.revokeForUser(userId);
    }

    @Override
    public void verifyEmail(String token) {
        emailVerificationService.verifyEmail(token);
    }

    @Override
    public void resendVerificationEmail(String email) {
        emailVerificationService.resendVerificationEmail(email);
    }

    @Override
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String token = java.util.UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(
                    "reset_token:" + token,
                    user.getId().toString(),
                    java.time.Duration.ofMinutes(15)
            );
            String resetUrl = "http://localhost:3000/reset-password?token=" + token;
            iamEventPublisher.publishPasswordResetRequested(user, resetUrl);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String redisKey = "reset_token:" + token;
        String userIdStr = redisTemplate.opsForValue().get(redisKey);
        
        if (userIdStr == null) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        
        java.util.UUID userId = java.util.UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        redisTemplate.delete(redisKey);
        
        // Also revoke all active sessions for security
        logout(userId.toString());
    }
}