package com.example.communityservice.repository;

import com.example.communityservice.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByTargetIdAndStatus(String targetId, Review.ReviewStatus status);
    
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.targetId = :targetId AND r.status = 'APPROVED'")
    Double getAverageRatingForTarget(@Param("targetId") String targetId);

    @Modifying
    @Query("UPDATE Review r SET r.status = 'REJECTED' WHERE r.targetId = :targetId")
    void softDeleteByTargetId(@Param("targetId") String targetId);
}