package com.example.payment.service;

import com.example.payment.audit.AuditService;
import com.example.payment.config.PaymentProperties;
import com.example.payment.config.RazorpayProperties;
import com.example.payment.dto.CreateOrderRequest;
import com.example.payment.dto.CreateOrderResponse;
import com.example.payment.dto.QuoteRequest;
import com.example.payment.dto.QuoteResponse;
import com.example.payment.entity.Coupon;
import com.example.payment.entity.Order;
import com.example.payment.entity.OrderItem;
import com.example.payment.entity.Payment;
import com.example.payment.entity.Product;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.event.DomainEventPublisher;
import com.example.payment.event.EventTypes;
import com.example.payment.gateway.PaymentGateway;
import com.example.payment.gateway.ProviderOrder;
import com.example.payment.repository.OrderItemRepository;
import com.example.payment.repository.OrderRepository;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.ProductPriceRepository;
import com.example.payment.repository.ProductRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final ProductRepository products;
    private final ProductPriceRepository prices;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final PaymentRepository payments;
    private final CouponService couponService;
    private final PricingEngine pricing;
    private final PaymentGateway gateway;
    private final IdempotencyService idem;
    private final EntitlementService entitlements;
    private final DomainEventPublisher events;
    private final AuditService audit;
    private final PaymentProperties props;
    private final RazorpayProperties razorpayProperties;
    private final TransactionTemplate tx;
    private final MeterRegistry metrics;

    // ---------- QUOTE (UX only; never trusted) ----------

    @Transactional(readOnly = true)
    public QuoteResponse quote(UUID userId, QuoteRequest req) {
        Product p = purchasable(req.productId());
        long unit = currentPrice(p.getId(), props.getCurrencyDefault());
        Coupon c = StringUtils.hasText(req.couponCode())
            ? couponService.validate(req.couponCode(), userId, p.getId(), unit)
            : null;
        PriceBreakdown b = pricing.calculate(props.getCurrencyDefault(), unit, 1, c);
        return new QuoteResponse(p.getId(), p.getName(), b, Instant.now().plusSeconds(props.getQuoteTtlSeconds()));
    }

    // ---------- ORDER CREATION ----------

    public CreateOrderResponse createOrder(UUID userId, String idemKey, CreateOrderRequest req) {
        var begun = idem.begin(userId, "CREATE_ORDER", idemKey, req);
        if (begun.replay()) return idem.replay(begun.row(), CreateOrderResponse.class);

        try {
            Order order = tx.execute(s -> persistOrder(userId, req));
            ProviderOrder po;

            try {
                po = gateway.createOrder(order.getOrderNumber(), order.getTotalAmount(), order.getCurrency(), Map.of("orderId", order.getId().toString(), "userId", userId.toString()));
            } catch (PaymentException e) {
                tx.executeWithoutResult(s -> failOrder(order.getId(), "Provider order creation failed"));
                throw e;
            }

            CreateOrderResponse resp = tx.execute(s -> attachPayment(order.getId(), po));
            idem.complete(begun.row(), 201, resp);
            metrics.counter("orders_created_total").increment();
            return resp;
        } catch (RuntimeException e) {
            idem.fail(begun.row());
            throw e;
        }
    }

    private Order persistOrder(UUID userId, CreateOrderRequest req) {
        Product p = purchasable(req.productId());

        if (entitlements.hasActive(userId, p.getProductType(), p.getReferenceId())) {
            throw new PaymentException(ErrorCode.ORDER_ALREADY_PAID, "You already own this product");
        }

        long unit = currentPrice(p.getId(), props.getCurrencyDefault());
        Coupon c = StringUtils.hasText(req.couponCode())
            ? couponService.validate(req.couponCode(), userId, p.getId(), unit)
            : null;
        PriceBreakdown b = pricing.calculate(props.getCurrencyDefault(), unit, 1, c);

        Order o = new Order();
        o.setOrderNumber("ORD-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        o.setUserId(userId);
        o.setCurrency(b.currency());
        o.setSubtotalAmount(b.subtotal());
        o.setDiscountAmount(b.discount());
        o.setTaxableAmount(b.taxable());
        o.setTaxAmount(b.tax());
        o.setTotalAmount(b.total());
        o.setCouponCode(b.couponCode());
        o.setStatus(OrderStatus.CREATED);
        o.setExpiresAt(Instant.now().plus(Duration.ofMinutes(props.getOrderTtlMinutes())));
        orders.save(o);

        OrderItem it = new OrderItem();
        it.setOrderId(o.getId());
        it.setProductId(p.getId());
        it.setProductType(p.getProductType());
        it.setProductReferenceId(p.getReferenceId());
        it.setProductNameSnapshot(p.getName());
        it.setQuantity(1);
        it.setUnitPriceAmount(unit);
        it.setDiscountAmount(b.discount());
        it.setTaxAmount(b.tax());
        it.setFinalAmount(b.total());
        orderItems.save(it);

        if (c != null) couponService.reserve(c, userId, o.getId(), b.discount());

        events.publish("order", "ORDER", o.getId(), userId, EventTypes.ORDER_CREATED, Map.of("orderNumber", o.getOrderNumber(), "total", o.getTotalAmount(), "currency", o.getCurrency()));
        audit.record("USER", userId.toString(), "ORDER_CREATED", "ORDER", o.getId(), o.getId(), c == null ? null : "coupon=" + c.getCode());
        log.info("order created orderId={} userId={} total={}", o.getId(), userId, o.getTotalAmount());
        return o;
    }

    private CreateOrderResponse attachPayment(UUID orderId, ProviderOrder po) {
        Order o = orders.findById(orderId).orElseThrow();
        Payment pay = new Payment();
        pay.setOrderId(orderId);
        pay.setProvider(gateway.name());
        pay.setProviderOrderId(po.id());
        pay.setProviderStatus(po.status());
        pay.setAmount(o.getTotalAmount());
        pay.setCurrency(o.getCurrency());
        payments.save(pay);
        o.transitionTo(OrderStatus.PAYMENT_PENDING);

        events.publish("payment", "PAYMENT", pay.getId(), o.getUserId(), EventTypes.PAYMENT_CREATED, Map.of("orderId", orderId.toString(), "amount", pay.getAmount()));
        audit.record("SYSTEM", null, "PROVIDER_ORDER_CREATED", "PAYMENT", pay.getId(), orderId, "providerOrderId=" + po.id());

        return new CreateOrderResponse(o.getId(), o.getOrderNumber(), pay.getId(), gateway.name(), po.id(), razorpayProperties.getKeyId(), o.getTotalAmount(), o.getCurrency(), o.getExpiresAt());
    }

    private void failOrder(UUID orderId, String why) {
        Order o = orders.findById(orderId).orElseThrow();
        o.transitionTo(OrderStatus.FAILED);
        couponService.release(orderId);
        audit.record("SYSTEM", null, "ORDER_FAILED", "ORDER", orderId, orderId, why);
    }

    private Product purchasable(UUID productId) {
        return products.findById(productId)
            .filter(Product::isActive)
            .orElseThrow(() -> new PaymentException(ErrorCode.PRODUCT_NOT_PURCHASABLE, "Product is not available for purchase"));
    }

    private long currentPrice(UUID productId, String currency) {
        return prices.findActive(productId, currency, Instant.now())
            .orElseThrow(() -> new PaymentException(ErrorCode.PRICE_NOT_FOUND, "No active price"))
            .getAmountMinor();
    }
}