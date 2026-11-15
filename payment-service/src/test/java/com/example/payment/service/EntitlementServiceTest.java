package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.client.TestServiceClient;
import com.example.payment.entity.Entitlement;
import com.example.payment.entity.enums.EntitlementSource;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.exception.PaymentException;
import com.example.payment.repository.EntitlementRepository;
import com.example.payment.repository.ProductRepository;
import com.example.payment.entity.Product;
import com.example.payment.event.EventTypes;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntitlementServiceTest {
    @Mock EntitlementRepository repo; @Mock TestServiceClient testClient; @Mock DomainEventPublisher events; @Mock AuditService audit; @Mock ProductRepository products;
    EntitlementService service;
    UUID user = UUID.randomUUID(), test = UUID.randomUUID(), series = UUID.randomUUID();

    @BeforeEach void setUp() { service = new EntitlementService(repo, testClient, events, audit, new SimpleMeterRegistry(), products); }

    Entitlement active(Instant exp) { Entitlement e = new Entitlement(); e.setExpiresAt(exp); return e; }

    @Test void seriesEntitlementUnlocksTestInSeries() {
        when(repo.findActive(user, ProductType.TEST, test)).thenReturn(Optional.empty());
        when(testClient.seriesIdOf(test)).thenReturn(series);
        when(repo.findActive(user, ProductType.TEST_SERIES, series)).thenReturn(Optional.of(active(null)));
        assertTrue(service.canAccessTest(user, test));
    }
    @Test void directTestEntitlementWorksWithoutTestServiceCall() {
        when(repo.findActive(user, ProductType.TEST, test)).thenReturn(Optional.of(active(null)));
        assertTrue(service.canAccessTest(user, test));
        verify(testClient, never()).seriesIdOf(any());
    }
    @Test void noEntitlementDenied() {
        when(repo.findActive(any(), any(), any())).thenReturn(Optional.empty());
        when(testClient.seriesIdOf(test)).thenReturn(series);
        assertFalse(service.canAccessTest(user, test));
    }
    @Test void standaloneTestWithoutSeriesDenied() {
        when(repo.findActive(any(), any(), any())).thenReturn(Optional.empty());
        when(testClient.seriesIdOf(test)).thenReturn(null);
        assertFalse(service.canAccessTest(user, test));
    }
    @Test void expiredEntitlementDenied() {
        when(repo.findActive(user, ProductType.TEST, test)).thenReturn(Optional.of(active(Instant.now().minusSeconds(5))));
        when(testClient.seriesIdOf(test)).thenReturn(null);
        assertFalse(service.canAccessTest(user, test));
    }
    @Test void duplicateGrantReturnsExistingAndWritesNothing() {
        Entitlement existing = active(null);
        when(repo.findActive(user, ProductType.TEST_SERIES, series)).thenReturn(Optional.of(existing));
        assertSame(existing, service.grantPurchase(user, ProductType.TEST_SERIES, series, UUID.randomUUID()));
        verify(repo, never()).saveAndFlush(any());
    }
    @Test void manualGrantCannotUsePurchaseSource() {
        assertThrows(PaymentException.class, () -> service.grantManual(UUID.randomUUID(), user, ProductType.TEST_SERIES, series,
            EntitlementSource.PURCHASE, "x", null));
    }

    @SuppressWarnings("unchecked")
    @Test void newManualGrantPublishesAccessGrantedWithReasonTitleAndSlug() {
        when(repo.findActive(user, ProductType.TEST_SERIES, series)).thenReturn(Optional.empty());
        Product p = new Product(); p.setName("SSC CGL Full Mocks");
        when(products.findByProductTypeAndReferenceId(ProductType.TEST_SERIES, series)).thenReturn(Optional.of(p));
        when(testClient.seriesSlugOf(series)).thenReturn("ssc-cgl-full-mocks");

        service.grantManual(UUID.randomUUID(), user, ProductType.TEST_SERIES, series, EntitlementSource.PROMOTION, "  Scholarship winner ", null);

        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(events).publish(eq("payment"), eq("ENTITLEMENT"), any(), eq(user), eq(EventTypes.ACCESS_GRANTED), payload.capture());
        assertEquals("Scholarship winner", payload.getValue().get("reason"));
        assertEquals("SSC CGL Full Mocks", payload.getValue().get("seriesTitle"));
        assertEquals("ssc-cgl-full-mocks", payload.getValue().get("slug"));
        assertFalse(payload.getValue().containsKey("expiresAt"));
    }
    @Test void manualGrantStillSucceedsWhenSlugLookupFails() {
        when(repo.findActive(user, ProductType.TEST_SERIES, series)).thenReturn(Optional.empty());
        when(products.findByProductTypeAndReferenceId(any(), any())).thenReturn(Optional.empty());
        when(testClient.seriesSlugOf(series)).thenThrow(new RuntimeException("down"));
        service.grantManual(UUID.randomUUID(), user, ProductType.TEST_SERIES, series, EntitlementSource.ADMIN_GRANT, "reason", null);
        verify(events).publish(anyString(), anyString(), any(), any(), eq(EventTypes.ACCESS_GRANTED), any());
    }
    @Test void repeatManualGrantDoesNotNotifyAgain() {
        when(repo.findActive(user, ProductType.TEST_SERIES, series)).thenReturn(Optional.of(active(null)));
        service.grantManual(UUID.randomUUID(), user, ProductType.TEST_SERIES, series, EntitlementSource.ADMIN_GRANT, "reason", null);
        verify(events, never()).publish(anyString(), anyString(), any(), any(), eq(EventTypes.ACCESS_GRANTED), any());
    }

    Entitlement activeEntitlement(Instant exp) {
        Entitlement e = active(exp);
        e.setUserId(user); e.setProductType(ProductType.TEST_SERIES); e.setProductReferenceId(series);
        e.setSource(EntitlementSource.ADMIN_GRANT);
        return e;
    }
    @SuppressWarnings("unchecked")
    @Test void adminRevokePublishesAccessRevokedWithReason() {
        Entitlement e = activeEntitlement(null);
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(e));
        when(products.findByProductTypeAndReferenceId(any(), any())).thenReturn(Optional.empty());

        service.revokeById(UUID.randomUUID(), id, "Granted by mistake");

        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(events).publish(eq("payment"), eq("ENTITLEMENT"), any(), eq(user), eq(EventTypes.ACCESS_REVOKED), payload.capture());
        assertEquals("Granted by mistake", payload.getValue().get("reason"));
    }
    @Test void revokingLapsedGrantDoesNotNotify() {
        Entitlement e = activeEntitlement(Instant.now().minusSeconds(60));
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(e));
        service.revokeById(UUID.randomUUID(), id, "cleanup");
        verify(events, never()).publish(anyString(), anyString(), any(), any(), eq(EventTypes.ACCESS_REVOKED), any());
    }
    @Test void refundRevocationDoesNotSendAccessNotice() {
        Entitlement e = activeEntitlement(null);
        UUID order = UUID.randomUUID();
        when(repo.findActiveByOrder(order)).thenReturn(java.util.List.of(e));
        service.revokeForOrder(order, "refund", "SYSTEM", null);
        verify(events, never()).publish(anyString(), anyString(), any(), any(), eq(EventTypes.ACCESS_REVOKED), any());
    }
}
