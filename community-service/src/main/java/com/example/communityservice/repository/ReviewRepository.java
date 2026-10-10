package com.example.communityservice.repository;

import com.example.communityservice.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    long countByStatusAndDeletedAtIsNull(Review.ReviewStatus status);

    long countByDeletedAtIsNull();

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.status = :status AND r.deletedAt IS NULL")
    Double getAverageApprovedRating(@Param("status") Review.ReviewStatus status);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.targetId = :targetId AND r.status = :status AND r.deletedAt IS NULL")
    Double getAverageRatingForTarget(@Param("targetId") String targetId, @Param("status") Review.ReviewStatus status);

    @Modifying
    @Query("UPDATE Review r SET r.status = 'REJECTED' WHERE r.targetId = :targetId AND r.deletedAt IS NULL")
    int softDeleteByTargetId(@Param("targetId") String targetId);
}
