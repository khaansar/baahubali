package com.example.iam.service;

import com.example.iam.dto.AuthenticationResult;
import com.example.iam.dto.LoginRequest;
import com.example.iam.dto.RegisterRequest;

public interface AuthService {
    AuthenticationResult register(RegisterRequest request);
    AuthenticationResult authenticate(LoginRequest request);
    AuthenticationResult refresh(String refreshToken);
    void logout(String userId);
}
