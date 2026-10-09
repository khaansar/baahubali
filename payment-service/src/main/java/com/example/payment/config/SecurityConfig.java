package com.example.payment.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<InternalAuthFilter> internalAuthFilter(
            InternalAuthProperties properties) {

        FilterRegistrationBean<InternalAuthFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(new InternalAuthFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setName("internalServiceAuthenticationFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);

        return registration;
    }
}