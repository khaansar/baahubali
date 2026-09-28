package com.example.iam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "processed_attempt_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedAttemptEvent {

    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}