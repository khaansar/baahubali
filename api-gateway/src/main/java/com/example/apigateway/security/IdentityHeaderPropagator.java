package com.example.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class IdentityHeaderPropagator {

    private static final String SERVICE_CALLER = "X-Service-Caller";
    private static final String SERVICE_AUTH = "X-Service-Auth";

    private final String gatewaySecret;

    public IdentityHeaderPropagator(
            @Value("${gateway.service-auth.secret}") String gatewaySecret) {
        if (!StringUtils.hasText(gatewaySecret)) {
            throw new IllegalStateException(
                    "gateway.service-auth.secret must be configured");
        }
        this.gatewaySecret = gatewaySecret;
    }

    /**
     * Used for public routes. User identity supplied by the client is never
     * trusted, and service credentials supplied by the client are overwritten.
     */
    public ServerHttpRequest sanitize(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(GatewayHeaders.USER_ID);
                    headers.remove(GatewayHeaders.USER_ROLE);
                    headers.remove(SERVICE_CALLER);
                    headers.remove(SERVICE_AUTH);

                    headers.set(SERVICE_CALLER, "api-gateway");
                    headers.set(SERVICE_AUTH, gatewaySecret);
                })
                .build();
    }

    /**
     * Used after JWT validation. Only validated user identity is propagated.
     */
    public ServerHttpRequest propagate(
            ServerHttpRequest request,
            AuthenticatedUser user) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(SERVICE_CALLER);
                    headers.remove(SERVICE_AUTH);

                    headers.set(SERVICE_CALLER, "api-gateway");
                    headers.set(SERVICE_AUTH, gatewaySecret);
                    headers.set(GatewayHeaders.USER_ID, user.userId());
                    headers.set(GatewayHeaders.USER_ROLE, user.role());
                })
                .build();
    }
}