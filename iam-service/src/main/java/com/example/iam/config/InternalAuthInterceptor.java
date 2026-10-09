package com.example.iam.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@ConfigurationProperties(prefix = "internal.auth")
public class InternalAuthInterceptor implements HandlerInterceptor {

    private Map<String, String> allowedClients = new HashMap<>();

    public Map<String, String> getAllowedClients() {
        return allowedClients;
    }

    public void setAllowedClients(Map<String, String> allowedClients) {
        this.allowedClients = new HashMap<>(allowedClients);
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

        response.sendError(
                HttpStatus.UNAUTHORIZED.value(),
                "Service authentication failed");
        return false;
    }
}