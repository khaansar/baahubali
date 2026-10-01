package com.example.iam.service;

import com.example.common.audit.AuditEvent;
import com.example.iam.dto.AuditLogResponse;
import com.example.iam.entity.AuditLog;
import com.example.iam.entity.User;
import com.example.iam.repository.AuditLogRepository;
import com.example.iam.repository.UserRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void store(AuditEvent event) {
        if (auditLogRepository.existsByEventId(event.eventId())) {
            return;
        }

        String actorName = null;

        if (event.actorId() != null) {
            try {
                UUID actorId = UUID.fromString(event.actorId());

                actorName = userRepository.findById(actorId)
                        .map(this::buildActorName)
                        .orElse(null);
            } catch (IllegalArgumentException ex) {
                log.warn(
                        "Invalid actor ID {} in audit event {}",
                        event.actorId(),
                        event.eventId()
                );
            }
        }

        AuditLog auditLog = AuditLog.builder()
                .eventId(event.eventId())
                .actorId(parseUuid(event.actorId()))
                .actorName(actorName)
                .actorRole(event.actorRole())
                .action(event.action())
                .resourceType(event.resourceType())
                .resourceId(event.resourceId())
                .service(event.service())
                .endpoint(event.endpoint())
                .httpMethod(event.httpMethod())
                .statusCode(event.statusCode())
                .requestId(event.requestId())
                .metadata(toJson(event.metadata()))
                .createdAt(event.createdAt() != null ? event.createdAt() : Instant.now())
                .build();

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            UUID actorId,
            String action,
            String resourceType,
            String service,
            Instant from,
            Instant to,
            Pageable pageable) {

        return auditLogRepository.search(
                        actorId,
                        normalize(action),
                        normalize(resourceType),
                        normalize(service),
                        from,
                        to,
                        pageable
                )
                .map(AuditLogResponse::from);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String buildActorName(User user) {
        String firstName = user.getFirstName() == null
                ? ""
                : user.getFirstName().trim();

        String lastName = user.getLastName() == null
                ? ""
                : user.getLastName().trim();

        return (firstName + " " + lastName).trim();
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            log.warn("Could not serialize audit metadata", ex);
            return null;
        }
    }
}