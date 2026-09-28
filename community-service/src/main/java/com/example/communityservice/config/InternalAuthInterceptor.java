package com.example.communityservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.HashMap;

@Component
@ConfigurationProperties(prefix = "internal.auth")
@Setter
@Slf4j
public class InternalAuthInterceptor implements HandlerInterceptor {

    private Map<String, String> allowedClients = new HashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String caller = request.getHeader("X-Service-Caller");
        String authSecret = request.getHeader("X-Service-Auth");

        if (caller == null || authSecret == null) {
            log.warn("Internal auth failed: Missing headers");
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing internal auth headers");
            return false;
        }

        String expectedSecret = allowedClients.get(caller);
        if (expectedSecret == null || !expectedSecret.equals(authSecret)) {
            log.warn("Internal auth failed for caller: {}", caller);
            response.sendError(HttpStatus.FORBIDDEN.value(), "Invalid internal auth credentials");
            return false;
        }

        return true;
    }
}