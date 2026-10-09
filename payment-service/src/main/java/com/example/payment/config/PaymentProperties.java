package com.example.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;

@Getter @Setter
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private String currencyDefault = "INR";
    private int orderTtlMinutes = 30;
    private int quoteTtlSeconds = 300;
    private String internalToken;
    private String testServiceUrl;
    private String adminRoles = "ADMIN,SUPER_ADMIN";
    private String refundRoles = "ADMIN,SUPER_ADMIN";
    private Map<String, String> topics = new HashMap<>();
    private Reconciliation reconciliation = new Reconciliation();

    @Getter @Setter
    public static class Reconciliation {
        private int pendingOlderThanMinutes = 10;
        private long fixedDelayMs = 300_000;
    }
}