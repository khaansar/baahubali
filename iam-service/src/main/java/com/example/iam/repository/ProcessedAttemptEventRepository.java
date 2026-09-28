package com.example.iam.repository;

import com.example.iam.entity.ProcessedAttemptEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedAttemptEventRepository
        extends JpaRepository<ProcessedAttemptEvent, String> {
}