package com.example.attemptservice.repository;

import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, String> {

    List<Attempt> findByUserIdOrderByStartedAtDesc(String userId);

    Page<Attempt> findByUserId(String userId, Pageable pageable);
    
    List<Attempt> findByStatus(AttemptStatus status);

    boolean existsByUserIdAndTestIdAndStatus(String userId, String testId, com.example.attemptservice.entity.AttemptStatus status);
}
