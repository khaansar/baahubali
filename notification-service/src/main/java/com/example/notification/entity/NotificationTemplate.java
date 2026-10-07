package com.example.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "notification_templates",
        indexes = {
                @Index(name = "idx_template_code_channel", columnList = "template_code,channel")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_template_code_channel_version",
                        columnNames = {"template_code", "channel", "version"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "template_code", length = 150, nullable = false)
    private String templateCode;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Notification.Channel channel;

    @Column(length = 500)
    private String subject;

    @Lob
    @Column(name = "body", columnDefinition = "LONGTEXT", nullable = false)
    private String body;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
