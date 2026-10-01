package com.example.apigateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import java.util.UUID;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(-2) 
public class GatewayAuthenticationExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthenticationExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayAuthenticationExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        if (!(throwable instanceof GatewayAuthenticationException exception)) {
            return Mono.error(throwable);
        }

        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(throwable);
        }

        AuthErrorCode errorCode = exception.getErrorCode();
        String path = exchange.getRequest().getPath().value();
        String traceId = "err-" + UUID.randomUUID();

        log.warn("Authentication rejected trace_id={} [{} {}] code={} reason={}",
                traceId, exchange.getRequest().getMethod(), path, errorCode, rejectionReason(exception));

        response.setStatusCode(errorCode.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return response.writeWith(
                Mono.fromCallable(() -> serialize(ErrorResponse.of(errorCode, traceId), response.bufferFactory())));
    }

    private DataBuffer serialize(ErrorResponse body, DataBufferFactory bufferFactory) {
        return bufferFactory.wrap(objectMapper.writeValueAsBytes(body));
    }

    private String rejectionReason(GatewayAuthenticationException exception) {
        if (exception instanceof MissingTokenException) {
            return "TOKEN_COOKIE_MISSING";
        }
        if (exception instanceof ExpiredTokenException) {
            return "TOKEN_EXPIRED";
        }
        if (!(exception instanceof InvalidTokenException)) {
            return "AUTHENTICATION_REJECTED";
        }

        Throwable cause = exception.getCause();
        if (cause == null) {
            return "REQUIRED_JWT_CLAIM_MISSING";
        }
        if (cause instanceof io.jsonwebtoken.security.SignatureException) {
            return "JWT_SIGNATURE_INVALID";
        }
        if (cause instanceof io.jsonwebtoken.MalformedJwtException) {
            return "JWT_FORMAT_INVALID";
        }
        if (cause instanceof io.jsonwebtoken.UnsupportedJwtException) {
            return "JWT_TYPE_UNSUPPORTED";
        }
        if (cause instanceof IllegalArgumentException) {
            return "JWT_ARGUMENT_INVALID";
        }
        return "JWT_VALIDATION_FAILED";
    }
}
