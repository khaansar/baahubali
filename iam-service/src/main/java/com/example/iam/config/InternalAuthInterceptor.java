package com.example.iam.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
@Slf4j
public class InternalAuthInterceptor implements HandlerInterceptor {

    @Value("#{${internal.auth.allowed-clients}}")
    private Map<String, String> allowedClients;

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
