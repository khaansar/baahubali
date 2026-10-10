package com.example.payment.repository;

import com.example.payment.entity.Entitlement;
import com.example.payment.entity.enums.EntitlementStatus;
import com.example.payment.entity.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntitlementRepository extends JpaRepository<Entitlement, UUID>, JpaSpecificationExecutor<Entitlement> {
    Optional<Entitlement> findByUserIdAndProductTypeAndProductReferenceIdAndStatus(UUID u, ProductType t, UUID r, EntitlementStatus s);
    List<Entitlement> findBySourceOrderIdAndStatus(UUID orderId, EntitlementStatus s);
    Page<Entitlement> findByUserId(UUID userId, Pageable p);

    default Optional<Entitlement> findActive(UUID u, ProductType t, UUID r) {
        return findByUserIdAndProductTypeAndProductReferenceIdAndStatus(u, t, r, EntitlementStatus.ACTIVE);
    }
    default List<Entitlement> findActiveByOrder(UUID orderId) {
        return findBySourceOrderIdAndStatus(orderId, EntitlementStatus.ACTIVE);
    }
}