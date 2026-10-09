package com.example.payment;

import com.example.payment.client.TestServiceClient;
import com.example.payment.dto.CreateOrderRequest;
import com.example.payment.dto.CreateOrderResponse;
import com.example.payment.dto.QuoteRequest;
import com.example.payment.entity.Coupon;
import com.example.payment.entity.Order;
import com.example.payment.entity.Payment;
import com.example.payment.entity.enums.DiscountType;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.gateway.PaymentGateway;
import com.example.payment.gateway.ProviderOrder;
import com.example.payment.gateway.ProviderRefund;
import com.example.payment.outbox.OutboxRepository;
import com.example.payment.repository.CouponRepository;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.service.CheckoutService;
import com.example.payment.service.EntitlementService;
import com.example.payment.service.ProductService;
import com.example.payment.service.ProductService.ProductSyncRequest;
import com.example.payment.service.RefundService;
import com.example.payment.service.WebhookProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Boot 3.4+: @MockBean is deprecated in favour of @MockitoBean; both work.
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "payment.internal-token=t", "razorpay.key-id=k", "razorpay.key-secret=s", "razorpay.webhook-secret=w",
    "spring.jpa.hibernate.ddl-auto=none" })
class PaymentFlowIT {
    @Container static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl); r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
    }
    @MockBean PaymentGateway gateway; @MockBean TestServiceClient testClient; @MockBean KafkaTemplate<String, String> kafka;

    @Autowired ProductService products; @Autowired CheckoutService checkout; @Autowired WebhookProcessor webhooks;
    @Autowired EntitlementService entitlements; @Autowired RefundService refundService;
    @Autowired CouponRepository coupons; @Autowired OrderRepository orderRepo; @Autowired PaymentRepository paymentRepo;
    @Autowired OutboxRepository outbox;

    @Test
    void purchaseFlow_priceImmutable_duplicatesSafe_thenFullRefundRevokes() {
        UUID user = UUID.randomUUID(), admin = UUID.randomUUID(), series = UUID.randomUUID(), testId = UUID.randomUUID();
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(gateway.name()).thenReturn("RAZORPAY");

        var product = products.sync(new ProductSyncRequest(ProductType.TEST_SERIES, series, "JEE Series", true, 49900L, "INR"));

        Coupon c = new Coupon(); c.setCode("SAVE20"); c.setDiscountType(DiscountType.PERCENTAGE); c.setDiscountValue(2000);
        c.setStartsAt(Instant.now().minusSeconds(60)); c.setExpiresAt(Instant.now().plusSeconds(3600)); c.setUsageLimit(10);
        coupons.save(c);

        // quote (UX only)
        assertEquals(39920, checkout.quote(user, new QuoteRequest(product.getId(), "save20")).price().total());

        // order creation, idempotent
        String provOrder = "order_" + UUID.randomUUID().toString().substring(0, 8);
        when(gateway.createOrder(anyString(), anyLong(), anyString(), anyMap())).thenReturn(new ProviderOrder(provOrder, 39920, "INR", "created"));
        CreateOrderRequest req = new CreateOrderRequest(product.getId(), "save20");
        CreateOrderResponse o1 = checkout.createOrder(user, "key-1", req);
        CreateOrderResponse o2 = checkout.createOrder(user, "key-1", req);
        assertEquals(o1.orderId(), o2.orderId());
        verify(gateway, times(1)).createOrder(anyString(), anyLong(), anyString(), anyMap());
        var conflict = assertThrows(PaymentException.class,
            () -> checkout.createOrder(user, "key-1", new CreateOrderRequest(product.getId(), null)));
        assertEquals(ErrorCode.IDEMPOTENCY_CONFLICT, conflict.getCode());
        assertEquals(1, coupons.findByCode("SAVE20").orElseThrow().getUsedCount());

        // price change must not touch the existing order
        products.sync(new ProductSyncRequest(ProductType.TEST_SERIES, series, "JEE Series", true, 79900L, "INR"));
        assertEquals(39920, orderRepo.findById(o1.orderId()).orElseThrow().getTotalAmount());

        // access denied before payment
        when(testClient.seriesIdOf(testId)).thenReturn(series);
        assertFalse(entitlements.canAccessTest(user, testId));

        // webhook, delivered twice
        String body = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_X\",\"order_id\":\""
            + provOrder + "\",\"amount\":39920,\"currency\":\"INR\",\"method\":\"upi\"}}}}";
        when(gateway.verifyWebhookSignature(eq(body), any())).thenReturn(true);
        webhooks.receive(body, "sig", "evt-1");
        webhooks.receive(body, "sig", "evt-1");

        Order paid = orderRepo.findById(o1.orderId()).orElseThrow();
        assertEquals(OrderStatus.FULFILLED, paid.getStatus());
        assertTrue(entitlements.hasActive(user, ProductType.TEST_SERIES, series));
        assertTrue(entitlements.canAccessTest(user, testId));
        assertTrue(outbox.countByEventType("PaymentCaptured") >= 1);
        assertTrue(outbox.countByEventType("EntitlementGranted") >= 1);

        // full refund revokes entitlement
        Payment pay = paymentRepo.findByOrderId(o1.orderId()).get(0);
        when(gateway.refundPayment(eq("pay_X"), eq(39920L), anyString(), anyMap())).thenReturn(new ProviderRefund("rfnd_1", "pay_X", 39920, "processed"));
        refundService.request(admin, pay.getId(), 39920, "duplicate payment", "refund-1");
        assertFalse(entitlements.hasActive(user, ProductType.TEST_SERIES, series));
        assertEquals(OrderStatus.REFUNDED, orderRepo.findById(o1.orderId()).orElseThrow().getStatus());

        // cannot refund more than captured
        assertThrows(PaymentException.class, () -> refundService.request(admin, pay.getId(), 1, "again", "refund-2"));
    }
}