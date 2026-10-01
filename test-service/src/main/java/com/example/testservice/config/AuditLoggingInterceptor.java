package com.example.testservice.config;

import com.example.common.audit.AuditEvent;
import com.example.testservice.service.KafkaPublisherService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLoggingInterceptor implements HandlerInterceptor {

    private final KafkaPublisherService kafkaPublisherService;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {

        if (ex != null) {
            return;
        }

        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            return;
        }

        String method = request.getMethod();

        if ("GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }

        String uri = request.getRequestURI();

        if (!uri.startsWith("/admin/")) {
            return;
        }

        /*
         * Create operations are audited from their services because only the
         * service has access to the generated resource ID.
         */
        if (isServiceAuditedCreate(uri, method)) {
            return;
        }

        AuditAction auditAction = resolveAction(uri, method);

        if (auditAction == null) {
            return;
        }

        String actorId = request.getHeader("X-User-Id");
        String actorRole = request.getHeader("X-User-Role");
        String requestId = request.getHeader("X-Request-Id");

        String resourceId = extractResourceId(uri, auditAction.resourceType());

        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                actorId,
                actorRole,
                auditAction.action(),
                auditAction.resourceType(),
                resourceId,
                "test-service",
                uri,
                method,
                response.getStatus(),
                requestId,
                Map.of(),
                Instant.now()
        );

        try {
            kafkaPublisherService.emitAuditEvent(event);
        } catch (Exception e) {
            // Auditing must never break an already completed admin request.
            log.error(
                    "Failed to enqueue audit event for {} {}",
                    method,
                    uri,
                    e
            );
        }
    }

    private boolean isServiceAuditedCreate(String uri, String method) {
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }

        return uri.matches(".*/admin/categories/?")
                || uri.matches(".*/admin/series/?")
                || uri.matches(".*/admin/mock-tests/series/[^/]+/mock-tests/?")
                || uri.matches(".*/admin/questions/?")
                || uri.matches(".*/admin/questions/bulk/?")
                || uri.matches(".*/admin/mock-tests/[^/]+/sections/?")
                || uri.matches(".*/admin/mock-tests/[^/]+/clone/?");
    }

    private AuditAction resolveAction(String uri, String method) {

        if ("PUT".equalsIgnoreCase(method)) {

            if (uri.matches(".*/admin/categories/[^/]+/?")) {
                return new AuditAction("CATEGORY_UPDATED", "CATEGORY");
            }

            if (uri.matches(".*/admin/series/[^/]+/?")) {
                return new AuditAction("TEST_SERIES_UPDATED", "TEST_SERIES");
            }

            if (uri.matches(".*/admin/mock-tests/[^/]+/sections/reorder/?")) {
                return new AuditAction("SECTIONS_REORDERED", "MOCK_TEST");
            }

            if (uri.matches(".*/admin/sections/[^/]+/questions/reorder/?")) {
                return new AuditAction("QUESTIONS_REORDERED_IN_SECTION", "SECTION");
            }

            if (uri.matches(".*/admin/sections/[^/]+/questions/[^/]+/?")) {
                return new AuditAction("SECTION_QUESTION_MARKS_UPDATED", "SECTION_QUESTION");
            }

            if (uri.matches(".*/admin/sections/[^/]+/questions/?")) {
                return new AuditAction("QUESTIONS_ATTACHED_TO_SECTION", "SECTION");
            }

            if (uri.matches(".*/admin/sections/[^/]+/?")) {
                return new AuditAction("SECTION_UPDATED", "SECTION");
            }

            if (uri.matches(".*/admin/mock-tests/[^/]+/?")) {
                return new AuditAction("TEST_UPDATED", "TEST");
            }
        }

        if ("DELETE".equalsIgnoreCase(method)) {

            if (uri.matches(".*/admin/series/[^/]+/?")) {
                return new AuditAction("TEST_SERIES_DELETED", "TEST_SERIES");
            }

            if (uri.matches(".*/admin/questions/[^/]+/?")) {
                return new AuditAction("QUESTION_DELETED", "QUESTION");
            }

            if (uri.matches(".*/admin/sections/[^/]+/questions/[^/]+/?")) {
                return new AuditAction("QUESTION_REMOVED_FROM_SECTION", "SECTION_QUESTION");
            }
        }

        if ("POST".equalsIgnoreCase(method)) {

            if (uri.matches(".*/admin/mock-tests/[^/]+/publish/?")) {
                return new AuditAction("TEST_PUBLISHED", "TEST");
            }

            if (uri.matches(".*/admin/mock-tests/[^/]+/archive/?")) {
                return new AuditAction("TEST_ARCHIVED", "TEST");
            }

            if (uri.matches(".*/admin/mock-tests/[^/]+/revert-to-draft/?")) {
                return new AuditAction("TEST_REVERTED_TO_DRAFT", "TEST");
            }
        }

        return null;
    }

    private String extractResourceId(String uri, String resourceType) {

        String[] parts = uri.split("/");

        if ("TEST".equals(resourceType) && parts.length >= 4) {
            return findUuid(parts);
        }

        if ("TEST_SERIES".equals(resourceType) && parts.length >= 4) {
            return findUuid(parts);
        }

        if ("CATEGORY".equals(resourceType) && parts.length >= 4) {
            return findUuid(parts);
        }

        if ("SECTION".equals(resourceType) && parts.length >= 4) {
            return findUuid(parts);
        }

        if ("QUESTION".equals(resourceType) && parts.length >= 4) {
            return findUuid(parts);
        }

        if ("SECTION_QUESTION".equals(resourceType)) {
            return findUuid(parts);
        }

        return null;
    }

    private String findUuid(String[] parts) {
        for (String part : parts) {
            try {
                return UUID.fromString(part).toString();
            } catch (IllegalArgumentException ignored) {
                // Continue looking.
            }
        }

        return null;
    }

    private record AuditAction(
            String action,
            String resourceType
    ) {
    }
}