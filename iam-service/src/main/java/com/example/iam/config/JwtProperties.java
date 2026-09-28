package com.example.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        long refreshExpirationMs,
        String cookieName,
        String refreshCookieName,
        String cookieDomain,
        boolean cookieSecure,
        String cookieSameSite
) {}
