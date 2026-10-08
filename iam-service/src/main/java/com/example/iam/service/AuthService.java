package com.example.iam.service;

import com.example.iam.dto.AuthenticationResult;
import com.example.iam.dto.LoginRequest;
import com.example.iam.dto.RegisterRequest;
import com.example.iam.entity.User;

public interface AuthService {
    User register(RegisterRequest request);
    AuthenticationResult authenticate(LoginRequest request);
    AuthenticationResult refresh(String refreshToken);
    void logout(String userId);
    void verifyEmail(String token);
    boolean isEmailVerificationTokenValid(String token);
    void resendVerificationEmail(String email);
    void forgotPassword(String email);
    boolean isPasswordResetTokenValid(String token);
    void resetPassword(String token, String newPassword);
}