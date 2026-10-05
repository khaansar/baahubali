package com.example.analyticsservice.core.repository;

import com.example.analyticsservice.core.entity.UserTopicPerformanceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTopicPerformanceRepository extends JpaRepository<UserTopicPerformanceEntity, Long> {

    List<UserTopicPerformanceEntity> findByUserIdOrderByAccuracyPercentageDesc(String userId);

    Optional<UserTopicPerformanceEntity> findByUserIdAndTopic(String userId, String topic);
}
