package com.example.testservice.config;

import com.example.testservice.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@ConfigurationProperties(prefix = "internal.auth")
public class InternalAuthInterceptor implements HandlerInterceptor {

    private final Map<String, String> allowedClients = new HashMap<>();
    private final ObjectMapper objectMapper;

    public InternalAuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, String> getAllowedClients() {
        return allowedClients;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String caller = request.getHeader("X-Service-Caller");
        String supplied = request.getHeader("X-Service-Auth");
        String expected = caller == null ? null : allowedClients.get(caller);

        boolean valid = StringUtils.hasText(caller)
                && StringUtils.hasText(supplied)
                && expected != null
                && MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8));

        if (valid) {
            return true;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Service authentication failed",
                "INTERNAL_AUTH_FAILED",
                Collections.emptyList()));
        return false;
    }
}