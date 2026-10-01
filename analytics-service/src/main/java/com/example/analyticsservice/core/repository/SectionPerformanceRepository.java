package com.example.analyticsservice.core.repository;

import com.example.analyticsservice.core.entity.SectionPerformanceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SectionPerformanceRepository extends JpaRepository<SectionPerformanceEntity, Long> {

    List<SectionPerformanceEntity> findByUserId(String userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SectionPerformanceEntity s where s.attemptId = :attemptId")
    int deleteByAttemptId(@Param("attemptId") String attemptId);
}