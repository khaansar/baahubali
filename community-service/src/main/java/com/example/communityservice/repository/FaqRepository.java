package com.example.communityservice.repository;

import com.example.communityservice.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findByTargetIdOrderByDisplayOrderAsc(String targetId);
    void deleteByTargetId(String targetId);
}