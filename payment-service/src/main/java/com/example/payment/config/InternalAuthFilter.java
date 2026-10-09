package com.example.payment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalAuthFilter extends OncePerRequestFilter {

    private static final String CALLER_HEADER = "X-Service-Caller";
    private static final String AUTH_HEADER = "X-Service-Auth";

    private final Map<String, String> allowedClients;

    public InternalAuthFilter(InternalAuthProperties properties) {
        this.allowedClients = Map.copyOf(properties.getAllowedClients());

        if (allowedClients.isEmpty()) {
            throw new IllegalStateException(
                    "internal.auth.allowed-clients must not be empty");
        }
        allowedClients.forEach((caller, secret) -> {
            if (!StringUtils.hasText(caller) || !StringUtils.hasText(secret)) {
                throw new IllegalStateException(
                        "Every internal.auth.allowed-clients entry requires a caller and secret");
            }
        });
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Liveness/readiness probes may access actuator without service credentials.
        String path = request.getServletPath();
        return path != null
                && (path.equals("/actuator")
                || path.startsWith("/actuator/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String caller = request.getHeader(CALLER_HEADER);
        String supplied = request.getHeader(AUTH_HEADER);
        String expected = caller == null ? null : allowedClients.get(caller);

        if (!StringUtils.hasText(caller)
                || !StringUtils.hasText(supplied)
                || expected == null
                || !MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"code\":\"INTERNAL_AUTH_FAILED\","
                            + "\"message\":\"Service authentication failed\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}