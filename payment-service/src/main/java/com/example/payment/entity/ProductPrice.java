package com.example.payment.entity;

import com.example.payment.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @Entity @Table(name = "product_prices")
public class ProductPrice extends BaseEntity {
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID productId;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private long amountMinor;
    @Column(nullable = false) private Instant effectiveFrom;
    private Instant effectiveUntil;
    @Column(nullable = false) private String status = "ACTIVE";
}