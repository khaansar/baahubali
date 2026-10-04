package com.example.apigateway.filter;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.example.apigateway.security.PublicRouteMatcher;

import reactor.core.publisher.Mono;

/**
 * Cookie authentication needs a browser-origin defence even with SameSite
 * cookies. Non-browser clients without cookies are unaffected.
 */
@Component
public class CookieOriginProtectionFilter implements GlobalFilter, Ordered {

    private final Set<String> allowedOrigins;
    private final PublicRouteMatcher publicRouteMatcher;

    public CookieOriginProtectionFilter(
            @Value("${CORS_ALLOWED_ORIGINS}") String configuredOrigins,
            PublicRouteMatcher publicRouteMatcher) {

        this.publicRouteMatcher = publicRouteMatcher;

        this.allowedOrigins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(this::canonicalOrigin)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (publicRouteMatcher.isPublic(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        HttpMethod method = exchange.getRequest().getMethod();
        boolean unsafeMethod = method != null && !(method == HttpMethod.GET || method == HttpMethod.HEAD
                || method == HttpMethod.OPTIONS || method == HttpMethod.TRACE);
        boolean hasCookie = !exchange.getRequest().getCookies().isEmpty();
        if (!unsafeMethod || !hasCookie) {
            return chain.filter(exchange);
        }

        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin == null || !allowedOrigins.contains(canonicalOrigin(origin))) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private String canonicalOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            if (uri.getScheme() == null || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null) {
                return "invalid:" + origin;
            }
            int port = uri.getPort();
            String authority = uri.getHost().toLowerCase(Locale.ROOT)
                    + (port == -1 ? "" : ":" + port);
            return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + authority;
        } catch (IllegalArgumentException ex) {
            return "invalid:" + origin;
        }
    }

    @Override
    public int getOrder() {
        return JwtAuthenticationGlobalFilter.ORDER - 5;
    }
}
