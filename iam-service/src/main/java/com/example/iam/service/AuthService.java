package com.example.iam.service;

import com.example.iam.dto.AuthenticationResult;

public interface AuthService {
    AuthenticationResult register(com.example.iam.dto.RegisterRequest request);
    AuthenticationResult authenticate(com.example.iam.dto.LoginRequest request);
    AuthenticationResult refresh(String refreshToken);
    void logout(String userId);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
}