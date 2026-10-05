package com.example.communityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "reviews", uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_user_target_review", 
        columnNames = {"user_id", "target_id", "target_type"}
    )
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // Sourced from UserContextHolder

    @Column(nullable = false)
    private String targetId; // e.g., "test-123"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType; // TEST or SERIES

    @Column(nullable = false)
    private Integer rating; // 1 to 5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.APPROVED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum TargetType { TEST, SERIES, PLATFORM }
    public enum ReviewStatus { PENDING, APPROVED, REJECTED }
}
