package com.example.analyticsservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import com.example.analyticsservice.web.dto.ApiErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Service-to-service authentication for /internal/** endpoints.
 * Expects headers X-Service-Caller and X-Service-Auth. Never logs secrets.
 */
@Component
@ConfigurationProperties(prefix = "internal.auth")
public class InternalAuthInterceptor implements HandlerInterceptor {

    public static final String CALLER_HEADER = "X-Service-Caller";
    public static final String AUTH_HEADER = "X-Service-Auth";
    private static final Logger log = LoggerFactory.getLogger(InternalAuthInterceptor.class);

    private final JsonMapper jsonMapper;
    private final Map<String, String> allowedClients = new HashMap<>();

    @Autowired
    public InternalAuthInterceptor(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Map<String, String> getAllowedClients() {
        return allowedClients;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String caller = request.getHeader(CALLER_HEADER);
        String secret = request.getHeader(AUTH_HEADER);

        if (isBlank(caller) || isBlank(secret)) {
            return reject(response, "missing credentials headers", caller);
        }
        String expected = allowedClients.get(caller);
        if (expected == null) {
            return reject(response, "unknown caller", caller);
        }
        boolean matches = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            return reject(response, "invalid secret", caller);
        }
        return true;
    }

    private boolean reject(HttpServletResponse response, String reason, String caller) throws IOException {
        String traceId = "err-" + UUID.randomUUID();
        log.warn("Internal auth rejected trace_id={} reason={} caller={}", traceId, reason, caller);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(ApiErrorResponse.failure(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Internal service authentication failed",
                "INTERNAL_AUTH_FAILED",
                List.of(),
                traceId)));
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
