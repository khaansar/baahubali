package com.example.communityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;

    public enum TargetType { TEST, SERIES }
    public enum ReviewStatus { PENDING, APPROVED, REJECTED }
}