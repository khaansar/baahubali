package com.example.platformcommon.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class OpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI platformOpenAPI(
            @Value("${spring.application.name:service}") String serviceName,
            @Value("${app.api.version}") String apiVersion) {

        return new OpenAPI()
                .info(new Info()
                        .title(serviceName)
                        .version(apiVersion)
                        .description("Auto-generated API documentation for " + serviceName))
                .components(new Components()
                        .addSecuritySchemes("gatewayUser", gatewayUserHeaderScheme())
                        .addSecuritySchemes("gatewayRole", gatewayRoleHeaderScheme()));
    }

    private SecurityScheme gatewayUserHeaderScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-User-Id")
                .description("Injected by api-gateway after JWT validation. "
                        + "Not required when calling through the gateway.");
    }

    private SecurityScheme gatewayRoleHeaderScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-User-Role")
                .description("Injected by api-gateway after JWT validation. "
                        + "Required by role-gated endpoints (e.g. /admin/**).");
    }
}
