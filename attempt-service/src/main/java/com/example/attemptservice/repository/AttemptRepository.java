package com.example.attemptservice.repository;

import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, String> {

    List<Attempt> findByUserIdOrderByStartedAtDesc(String userId);

    Page<Attempt> findByUserId(String userId, Pageable pageable);

    List<Attempt> findByStatus(AttemptStatus status);

    Optional<Attempt> findFirstByUserIdAndTestIdAndStatusOrderByStartedAtDesc(
            String userId,
            String testId,
            AttemptStatus status
    );

    boolean existsByUserIdAndTestIdAndStatus(
            String userId,
            String testId,
            AttemptStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Attempt a where a.id = :attemptId")
    Optional<Attempt> findByIdForUpdate(@Param("attemptId") String attemptId);
}