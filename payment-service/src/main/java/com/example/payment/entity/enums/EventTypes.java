package com.example.payment.enums;

public final class EventTypes {

    public static final String ORDER_CREATED =
        "OrderCreated";

    public static final String ORDER_PAID =
        "OrderPaid";

    public static final String PAYMENT_CREATED =
        "PaymentCreated";

    public static final String PAYMENT_AUTHORIZED =
        "PaymentAuthorized";

    public static final String PAYMENT_CAPTURED =
        "PaymentCaptured";

    public static final String PAYMENT_FAILED =
        "PaymentFailed";

    public static final String REFUND_REQUESTED =
        "RefundRequested";

    public static final String REFUND_SUCCEEDED =
        "RefundSucceeded";

    public static final String REFUND_FAILED =
        "RefundFailed";

    public static final String ENTITLEMENT_GRANTED =
        "EntitlementGranted";

    public static final String ENTITLEMENT_REVOKED =
        "EntitlementRevoked";

    public static final String COUPON_REDEEMED =
        "CouponRedeemed";

    private EventTypes() {
    }
}