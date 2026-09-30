package com.example.iam.repository;

import com.example.iam.entity.UserDailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDailyActivityRepository extends JpaRepository<UserDailyActivity, UUID> {
    
    Optional<UserDailyActivity> findByUserIdAndActivityDate(String userId, LocalDate activityDate);
    
    List<UserDailyActivity> findByUserIdAndActivityDateBetweenOrderByActivityDateAsc(String userId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT uda.activityDate FROM UserDailyActivity uda WHERE uda.userId = :userId ORDER BY uda.activityDate DESC")
    List<LocalDate> findActivityDatesByUserIdOrderByActivityDateDesc(@Param("userId") String userId);
}
