package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.client.TestServiceClient;
import com.example.payment.entity.Entitlement;
import com.example.payment.entity.enums.EntitlementSource;
import com.example.payment.entity.enums.EntitlementStatus;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.event.EventTypes;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.repository.EntitlementRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntitlementService {
    private final EntitlementRepository repo;
    private final TestServiceClient testClient;
    private final DomainEventPublisher events;
    private final AuditService audit;
    private final MeterRegistry metrics;

    @Transactional(propagation = Propagation.MANDATORY)
    public Entitlement grantPurchase(UUID userId, ProductType type, UUID refId, UUID orderId) {
        return grant(userId, type, refId, EntitlementSource.PURCHASE, orderId, "SYSTEM", null, null, null);
    }

    @Transactional
    public Entitlement grantManual(UUID adminId, UUID userId, ProductType type, UUID refId,
                                   EntitlementSource source, String reason, Instant expiresAt) {
        if (source == EntitlementSource.PURCHASE)
            throw new PaymentException(ErrorCode.VALIDATION_FAILED, "Manual grants cannot use source PURCHASE");
        return grant(userId, type, refId, source, null, "ADMIN", adminId.toString(), reason, expiresAt);
    }

    /** Idempotent. uq_ent_active (generated column) is the real guard; a lost race propagates and the caller retries. */
    private Entitlement grant(UUID userId, ProductType type, UUID refId, EntitlementSource src, UUID orderId,
                              String actor, String actorId, String reason, Instant expiresAt) {
        Optional<Entitlement> existing = repo.findActive(userId, type, refId);
        if (existing.isPresent()) return existing.get();
        Entitlement e = new Entitlement();
        e.setUserId(userId); e.setProductType(type); e.setProductReferenceId(refId);
        e.setSource(src); e.setSourceOrderId(orderId); e.setExpiresAt(expiresAt);
        repo.saveAndFlush(e);
        events.publish("entitlement", "ENTITLEMENT", e.getId(), userId, EventTypes.ENTITLEMENT_GRANTED,
            Map.of("productType", type.name(), "productReferenceId", refId.toString(), "source", src.name()));
        audit.record(actor, actorId, "ENTITLEMENT_GRANTED", "ENTITLEMENT", e.getId(), orderId, reason);
        metrics.counter("entitlements_granted_total").increment();
        return e;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeForOrder(UUID orderId, String reason, String actor, String actorId) {
        for (Entitlement e : repo.findActiveByOrder(orderId)) revoke(e, reason, actor, actorId);
    }

    @Transactional
    public void revokeById(UUID adminId, UUID entitlementId, String reason) {
        Entitlement e = repo.findById(entitlementId)
            .orElseThrow(() -> new PaymentException(ErrorCode.NOT_FOUND, "Entitlement not found"));
        revoke(e, reason, "ADMIN", adminId.toString());
    }

    @Transactional
    public void revoke(Entitlement e, String reason, String actor, String actorId) {
        if (e.getStatus() != EntitlementStatus.ACTIVE) return;
        e.setStatus(EntitlementStatus.REVOKED); e.setRevokedAt(Instant.now()); e.setRevokeReason(reason);
        repo.saveAndFlush(e);
        events.publish("entitlement", "ENTITLEMENT", e.getId(), e.getUserId(), EventTypes.ENTITLEMENT_REVOKED,
            Map.of("productType", e.getProductType().name(), "productReferenceId", e.getProductReferenceId().toString()));
        audit.record(actor, actorId, "ENTITLEMENT_REVOKED", "ENTITLEMENT", e.getId(), e.getSourceOrderId(), reason);
        metrics.counter("entitlements_revoked_total").increment();
    }

    @Transactional(readOnly = true)
    public boolean hasActive(UUID userId, ProductType type, UUID refId) {
        return repo.findActive(userId, type, refId)
            .filter(e -> e.getExpiresAt() == null || e.getExpiresAt().isAfter(Instant.now())).isPresent();
    }

    /** Direct TEST entitlement OR entitlement on the parent TEST_SERIES (resolved via trusted Test Service API). */
    public boolean canAccessTest(UUID userId, UUID testId) {
        if (hasActive(userId, ProductType.TEST, testId)) return true;
        UUID seriesId = testClient.seriesIdOf(testId);
        return seriesId != null && hasActive(userId, ProductType.TEST_SERIES, seriesId);
    }
}