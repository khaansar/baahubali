package com.example.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_id", columnList = "user_id"),
                @Index(name = "idx_notifications_status", columnList = "status"),
                @Index(name = "idx_notifications_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notifications_event_channel",
                        columnNames = {"event_id", "channel"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "channel", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Column(name = "recipient", length = 320)
    private String recipient;

    @Column(name = "template_code", nullable = false, length = 150)
    private String templateCode;

    @Lob
    @Column(name = "payload", columnDefinition = "LONGTEXT", nullable = false)
    private String payload;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "action_url", length = 1000)
    private String actionUrl;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Channel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }

    public enum Status {
        PENDING,
        PROCESSING,
        SENT,
        FAILED
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = Status.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
