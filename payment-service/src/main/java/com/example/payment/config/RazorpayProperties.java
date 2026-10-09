package com.example.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {
    private String baseUrl = "https://api.razorpay.com/v1";
    private String keyId;
    private String keySecret;       // never log
    private String webhookSecret;   // never log
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 8000;
}