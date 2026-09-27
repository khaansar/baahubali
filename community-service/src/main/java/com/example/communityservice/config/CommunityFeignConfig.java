package com.example.communityservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class CommunityFeignConfig {

    @Value("${internal.auth.my-secret}")
    private String mySecret;

    @Bean
    public RequestInterceptor communityServiceRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Service-Caller", "community-service");
            requestTemplate.header("X-Service-Auth", mySecret);
        };
    }
}