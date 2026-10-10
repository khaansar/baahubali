# Payment Service (Commerce / Billing boundary)

## Ownership
Payment Service owns products, prices, orders, payments, coupons, refunds, entitlements, provider integration,
webhooks, reconciliation and the commercial audit trail. Test Service owns test content; Attempt Service owns
attempts; IAM owns identity. Payment Service never reads another service's DB (Test Service is called over HTTP).

## Money
All amounts are BIGINT minor units (INR paise). Percentage coupons store basis points (2000 = 20%), floored.
Currency is stored explicitly on price, order, payment and refund.

## State machines
Order:  CREATED -> PAYMENT_PENDING -> PAID -> FULFILLED -> (PARTIALLY_REFUNDED)* -> REFUNDED
        CREATED/PAYMENT_PENDING -> FAILED | EXPIRED | CANCELLED
        EXPIRED/CANCELLED/FAILED -> PAID   (late capture: money taken, so honour it)
Refund: REQUESTED -> PROCESSING -> SUCCEEDED | FAILED ; REQUESTED -> FAILED
Payment: CREATED -> AUTHORIZED -> CAPTURED -> PARTIALLY_REFUNDED -> REFUNDED ; CREATED/AUTHORIZED -> FAILED/CANCELLED
Transitions are enforced in Order.transitionTo / Refund.transitionTo; invalid ones throw INVALID_STATE_TRANSITION (409).

## Purchase lifecycle
1. POST /payments-api/checkout/quote  (UX only, never trusted)
2. POST /payments-api/orders (Idempotency-Key required; body = productId + couponCode ONLY).
   Server recalculates price + coupon, snapshots order/items, reserves coupon, creates the Razorpay order
   (no DB txn open during the provider call), then creates the payment row and moves the order to PAYMENT_PENDING.
3. Browser opens Razorpay Checkout; its callback only triggers POST /payments/{id}/verify (a hint that fetches provider state).
4. Authoritative confirmation: Razorpay webhook (payment.captured) or reconciliation.
5. One transaction: payment CAPTURED + order PAID->FULFILLED + coupon CONFIRMED + entitlement ACTIVE + outbox rows.
6. Outbox relay publishes to Kafka. Access never depends on Kafka.

## Webhooks
POST /payments-api/webhooks/razorpay (public, HMAC-SHA256 over the RAW body, constant-time compare).
Events: payment.authorized, payment.captured, payment.failed, refund.processed, refund.failed.
Dedup: unique (provider, provider_event_id) in payment_webhook_events (event id from X-Razorpay-Event-Id, else body SHA-256).
Processing failure => 5xx so Razorpay retries; unknown provider orders are acknowledged and logged.
Invalid signatures => 400 and nothing stored.

## Coupons
Code normalised to UPPERCASE, unique. Checks: status, window, product applicability, minimum order, global limit,
first-order-only, per-user limit. Redemption is race-safe: atomic `UPDATE ... WHERE used_count < usage_limit`
plus a per-user counter row; both inside the order transaction. Failed/expired/cancelled orders release the slot.

## Refunds
Admin-only (payment.refund-roles). Idempotency-Key required; unique (payment_id, idempotency_key).
Refundable = captured - (REQUESTED + PROCESSING + SUCCEEDED) refunds, computed under a payment row lock.
Provider call uses X-Refund-Idempotency = refund id. A timeout leaves the refund REQUESTED; reconciliation settles it.
Policy (RefundEntitlementPolicy): full refund => revoke entitlement; partial => keep access.

## Entitlements
Sources: PURCHASE (always has source_order_id), ADMIN_GRANT, PROMOTION, COMPENSATION.
At most one ACTIVE entitlement per (user, productType, referenceId) via a generated-column unique index.
Test access = direct TEST entitlement OR active TEST_SERIES entitlement for the test's series
(series resolved via GET test-service /internal/tests/{id}/series). Fails closed if Test Service is down.

## Events (outbox -> Kafka)
Topics: order-events, payment-events, refund-events, entitlement-events. Envelope: eventId, eventType, occurredAt,
aggregateId, userId, payload. Key = aggregateId. outbox_events.id == eventId; consumers must dedup on it.
Types: OrderCreated, OrderPaid, PaymentCreated, PaymentAuthorized, PaymentCaptured, PaymentFailed,
RefundRequested, RefundSucceeded, RefundFailed, EntitlementGranted, EntitlementRevoked.

## Environment variables
PAYMENT_DB_URL, PAYMENT_DB_USERNAME, PAYMENT_DB_PASSWORD, REDIS_HOST, KAFKA_BOOTSTRAP_SERVERS,
INTERNAL_SERVICE_TOKEN, TEST_SERVICE_URL, PAYMENT_ORDER_TTL_MINUTES, RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET,
RAZORPAY_WEBHOOK_SECRET, PAYMENT_SERVICE_URL (gateway, attempt-service, test-service).

## Razorpay setup
1. Dashboard -> API Keys -> generate test keys.
2. Settings -> Payment Capture: enable AUTO-CAPTURE (otherwise payments stay "authorized").
3. Webhooks -> add https://<host>/payments-api/webhooks/razorpay, choose the 5 events above, set the secret = RAZORPAY_WEBHOOK_SECRET.
4. Local: expose the gateway with a tunnel (e.g. ngrok) and use that URL for the webhook.

## Local development
docker compose up -d mysql redis kafka ; create payment_db ; set the env vars ; docker compose up payment-service api-gateway.
Swagger: <gateway>/swagger-ui (Payment entry points at /payments-api/v3/api-docs).

## Testing
mvn -pl payment-service test        # unit tests
mvn -pl payment-service verify      # includes Testcontainers ITs (needs Docker)
mvn -pl attempt-service test