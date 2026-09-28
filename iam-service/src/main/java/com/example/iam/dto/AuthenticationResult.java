package com.example.iam.dto;

import com.example.iam.entity.User;

public record AuthenticationResult(
        User user,
        String token,
        String refreshToken
) {}
