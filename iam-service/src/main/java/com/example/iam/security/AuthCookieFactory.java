package com.example.iam.security;

import com.example.iam.config.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieFactory {

    private final JwtProperties jwtProperties;

    public AuthCookieFactory(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie buildAuthCookie(String token) {
        ResponseCookie.ResponseCookieBuilder builder = baseBuilder(token)
                .maxAge(jwtProperties.expirationMs() / 1000);

        return builder.build();
    }

    public ResponseCookie buildRefreshCookie(String token) {
        return refreshBuilder(token)
                .maxAge(jwtProperties.refreshExpirationMs() / 1000)
                .build();
    }

    public ResponseCookie buildExpiredAuthCookie() {
        return baseBuilder("").maxAge(0).build();
    }

    public ResponseCookie buildExpiredRefreshCookie() {
        return refreshBuilder("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        return cookieBuilder(jwtProperties.cookieName(), value);
    }

    private ResponseCookie.ResponseCookieBuilder refreshBuilder(String value) {
        return cookieBuilder(jwtProperties.refreshCookieName(), value);
    }

    private ResponseCookie.ResponseCookieBuilder cookieBuilder(String name, String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .path("/")
                .sameSite(jwtProperties.cookieSameSite());

        if (jwtProperties.cookieDomain() != null && !jwtProperties.cookieDomain().isBlank()) {
            builder.domain(jwtProperties.cookieDomain());
        }

        return builder;
    }
}
