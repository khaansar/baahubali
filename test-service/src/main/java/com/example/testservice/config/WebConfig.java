package com.example.testservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminRoleInterceptor adminRoleInterceptor;
    private final InternalAuthInterceptor internalAuthInterceptor;
    private final AuditLoggingInterceptor auditLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalAuthInterceptor)
                .addPathPatterns("/internal/**");

        registry.addInterceptor(adminRoleInterceptor)
                .addPathPatterns("/admin/**");

        registry.addInterceptor(auditLoggingInterceptor)
                .addPathPatterns("/admin/**");
    }
}