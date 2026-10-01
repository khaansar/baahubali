package com.example.iam.repository;

import com.example.iam.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    boolean existsByEventId(UUID eventId);

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE (:actorId IS NULL OR a.actorId = :actorId)
              AND (:action IS NULL OR a.action = :action)
              AND (:resourceType IS NULL OR a.resourceType = :resourceType)
              AND (:service IS NULL OR a.service = :service)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    Page<AuditLog> search(
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("service") String service,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}