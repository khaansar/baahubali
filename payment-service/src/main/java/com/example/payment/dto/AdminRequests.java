package com.example.payment.dto;

import com.example.payment.entity.enums.DiscountType;
import com.example.payment.entity.enums.EntitlementSource;
import com.example.payment.entity.enums.ProductType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AdminRequests {
    private AdminRequests() {}

    public record CouponRequest(
        @NotBlank @Size(max = 64) String code, @Size(max = 255) String description,
        @NotNull DiscountType discountType, @Positive long discountValue,
        @PositiveOrZero long minimumOrderAmount, @Positive Long maximumDiscountAmount,
        @NotNull Instant startsAt, @NotNull Instant expiresAt,
        @Positive Integer usageLimit, @Positive Integer perUserUsageLimit,
        boolean stackable, boolean firstOrderOnly, Set<UUID> productIds) {}

    public record RefundRequest(@Min(1) long amount, @NotBlank @Size(max = 500) String reason) {}

    public record GrantRequest(@NotNull UUID userId, @NotNull ProductType productType, @NotNull UUID productReferenceId,
                               @NotNull EntitlementSource source, @NotBlank @Size(max = 255) String reason, Instant expiresAt) {}

    public record RevokeRequest(@NotBlank @Size(max = 255) String reason) {}
}