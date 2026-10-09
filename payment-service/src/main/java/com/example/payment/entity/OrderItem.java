package com.example.payment.entity;

import com.example.payment.entity.enums.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity @Table(name = "order_items")
public class OrderItem extends AssignedIdEntity {
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID orderId;
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID productId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProductType productType;
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID productReferenceId;
    @Column(nullable = false) private String productNameSnapshot;
    private int quantity = 1;
    private long unitPriceAmount;
    private long discountAmount;
    private long taxAmount;
    private long finalAmount;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}