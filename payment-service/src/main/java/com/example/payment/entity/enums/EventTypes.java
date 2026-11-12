package com.example.payment.event;

public final class EventTypes {

    private EventTypes() {}

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_PAID = "ORDER_PAID";
    public static final String PAYMENT_CREATED = "PAYMENT_CREATED";
    public static final String PAYMENT_AUTHORIZED = "PAYMENT_AUTHORIZED";
    public static final String PAYMENT_CAPTURED = "PAYMENT_CAPTURED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String REFUND_REQUESTED = "REFUND_REQUESTED";
    public static final String REFUND_SUCCEEDED = "REFUND_SUCCEEDED";
    public static final String REFUND_FAILED = "REFUND_FAILED";
    public static final String ENTITLEMENT_GRANTED = "ENTITLEMENT_GRANTED";
    public static final String ENTITLEMENT_REVOKED = "ENTITLEMENT_REVOKED";
    /** User-facing notice for a manual grant; published on the payment topic the notification service consumes. */
    public static final String ACCESS_GRANTED = "ACCESS_GRANTED";
}