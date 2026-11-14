package com.example.testservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode; // Added import
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.VARCHAR) 
    private UUID id;

    @Column(name = "created_by", updatable = false, nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Version
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}