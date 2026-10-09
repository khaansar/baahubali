package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.client.TestServiceClient;
import com.example.payment.entity.Entitlement;
import com.example.payment.entity.enums.EntitlementSource;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.exception.PaymentException;
import com.example.payment.repository.EntitlementRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntitlementServiceTest {
    @Mock EntitlementRepository repo; @Mock TestServiceClient testClient; @Mock DomainEventPublisher events; @Mock AuditService audit;
    EntitlementService service;
    UUID user = UUID.randomUUID(), test = UUID.randomUUID(), series = UUID.randomUUID();

    @BeforeEach void setUp() { service = new EntitlementService(repo, testClient, events, audit, new SimpleMeterRegistry()); }

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
}