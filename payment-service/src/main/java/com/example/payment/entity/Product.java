package com.example.payment.entity;

import com.example.payment.entity.BaseEntity;
import com.example.payment.enums.ProductType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @Entity @Table(name = "products")
public class Product extends BaseEntity {
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProductType productType;
    @Column(nullable = false, columnDefinition = "BINARY(16)") private UUID referenceId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String status = "ACTIVE";
    private Instant deletedAt;
    public boolean isActive() { return "ACTIVE".equals(status) && deletedAt == null; }
}