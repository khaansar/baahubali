package com.example.attemptservice.filter;

import com.example.attemptservice.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class GatewaySecurityFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only secure external-facing endpoints routed by API Gateway
        if (request.getRequestURI().startsWith("/attempts-api/")) {
            String userId = request.getHeader("X-User-Id");
            
            // If the header is missing, the request did not come through the trusted Gateway
            if (userId == null || userId.trim().isEmpty()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Authentication is required to access this resource.",
                        new ApiResponse.ApiError("TOKEN_MISSING", List.of()),
                        "err-" + UUID.randomUUID()));
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
