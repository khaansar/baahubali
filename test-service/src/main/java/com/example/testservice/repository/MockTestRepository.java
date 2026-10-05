package com.example.testservice.repository;

import com.example.testservice.entity.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockTestRepository extends JpaRepository<MockTest, UUID>, JpaSpecificationExecutor<MockTest> {

    List<MockTest> findBySeriesIdAndDeletedAtIsNull(UUID seriesId);

    Optional<MockTest> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Modifying
    @Query("""
        UPDATE MockTest m
        SET m.isFree = :isFree
        WHERE m.series.id = :seriesId
        AND m.deletedAt IS NULL
    """)
    int updateFreeStatusBySeriesId(@Param("seriesId") UUID seriesId, @Param("isFree") boolean isFree, @Param("updatedAt") Instant updatedAt);
}