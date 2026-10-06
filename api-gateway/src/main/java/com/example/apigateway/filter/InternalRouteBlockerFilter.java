package com.example.apigateway.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Internal APIs are a service-mesh concern, never part of the browser-facing
 * gateway surface.  This guard remains effective even when a broad route is
 * added later for a downstream service.
 */
@Component
public class InternalRouteBlockerFilter implements GlobalFilter, Ordered {

    private static final int ORDER = JwtAuthenticationGlobalFilter.ORDER - 10;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (path.contains("/internal/")) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return response.setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
