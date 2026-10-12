package com.example.analyticsservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class AnalyticsFeignConfig {

    @Value("${internal.auth.my-secret}")
    private String serviceSecret;

    @Bean
    public RequestInterceptor analyticsServiceRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Service-Caller", "analytics-service");
            requestTemplate.header("X-Service-Auth", serviceSecret);
        };
    }
}
