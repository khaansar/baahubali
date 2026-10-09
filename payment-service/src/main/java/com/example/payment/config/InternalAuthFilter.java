package com.example.payment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalAuthFilter extends OncePerRequestFilter {

    private final byte[] token;

    public InternalAuthFilter(PaymentProperties properties) {
        this.token = properties.getInternalToken().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader("X-Internal-Token");

        if (header == null || !MessageDigest.isEqual(token, header.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(401);
            return;
        }

        chain.doFilter(request, response);
    }
}