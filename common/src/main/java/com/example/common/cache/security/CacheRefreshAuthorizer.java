package com.example.common.cache.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Restricts cache refresh operations to callers possessing the configured
 * operational refresh token. The token must never be supplied in the URL.
 */
@Component
public class CacheRefreshAuthorizer {

    private static final String REFRESH_TOKEN_HEADER = "X-Cache-Refresh-Token";

    private final String configuredRefreshToken;

    public CacheRefreshAuthorizer(
            @Value("${cache.refresh.secret:}") String configuredRefreshToken) {
        this.configuredRefreshToken = configuredRefreshToken == null
                ? ""
                : configuredRefreshToken.trim();
    }

    public void authorize(String suppliedToken) {
        if (configuredRefreshToken.isBlank() || suppliedToken == null || suppliedToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cache refresh is not authorized"
            );
        }

        byte[] expected = configuredRefreshToken.getBytes(StandardCharsets.UTF_8);
        byte[] supplied = suppliedToken.getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expected, supplied)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cache refresh is not authorized"
            );
        }
    }

    public String getRefreshTokenHeaderName() {
        return REFRESH_TOKEN_HEADER;
    }
}