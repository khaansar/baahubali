package com.example.payment.outbox;

import com.example.payment.entity.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter
@Entity @Table(name = "outbox_events")
public class OutboxEvent extends AssignedIdEntity {      // id == eventId (consumer dedup key)
    @Column(nullable = false) private String topic;
    @Column(nullable = false) private String aggregateType;
    @Column(nullable = false) private String aggregateId;
    @Column(nullable = false) private String eventType;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT") private String payload;
    @Column(nullable = false) private String status = "PENDING";   // PENDING | PUBLISHED | DEAD
    private int attempts;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    private Instant publishedAt;
}