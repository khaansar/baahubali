package com.example.payment.config;

import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Identity comes ONLY from gateway-injected headers; never from request bodies. */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {
    private final PaymentProperties props;

    public UUID userId(HttpServletRequest r) {
        String h = r.getHeader("X-User-Id");
        if (h == null || h.isBlank()) throw new PaymentException(ErrorCode.UNAUTHORIZED, "Unauthenticated");
        try { return UUID.fromString(h.trim()); }
        catch (IllegalArgumentException e) { throw new PaymentException(ErrorCode.UNAUTHORIZED, "Unauthenticated"); }
    }
    public UUID requireAdmin(HttpServletRequest r) { UUID id = userId(r); requireRole(r, props.getAdminRoles()); return id; }
    public UUID requireRefundAdmin(HttpServletRequest r) { UUID id = userId(r); requireRole(r, props.getRefundRoles()); return id; }

    private void requireRole(HttpServletRequest r, String csv) {
        String role = r.getHeader("X-User-Role");
        Set<String> allowed = Arrays.stream(csv.split(",")).map(String::trim).collect(Collectors.toSet());
        if (role == null || !allowed.contains(role.trim()))
            throw new PaymentException(ErrorCode.FORBIDDEN, "Admin access required");
    }
}