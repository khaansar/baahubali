package com.example.communityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {

    private final String adminRole;

    public AdminAuthorizationInterceptor(
            @Value("${security.admin-role:ADMIN}") String adminRole) {
        this.adminRole = adminRole;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        String userId = UserContextHolder.getUserId();
        String userRole = UserContextHolder.getUserRole();

        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required.");
        }

        if (userRole == null || !adminRole.equalsIgnoreCase(userRole.trim())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin authorization is required.");
        }

        return true;
    }
}