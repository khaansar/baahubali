package com.example.communityservice.event;

import com.example.communityservice.repository.FaqRepository;
import com.example.communityservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestEventConsumer {

    private final ReviewRepository reviewRepository;
    private final FaqRepository faqRepository;

    @Transactional
    @KafkaListener(topics = "test-events", groupId = "community-group")
    public void handleTestEvents(String message) {
        // Assuming a simple comma-separated payload like "DELETED,test-123" 
        // In a production environment, parse this from a JSON Event DTO
        try {
            String[] parts = message.split(",");
            if (parts.length == 2 && "DELETED".equals(parts[0])) {
                String targetId = parts[1];
                log.info("Received DELETED event for targetId: {}. Cleaning up associated community data.", targetId);
                
                reviewRepository.softDeleteByTargetId(targetId);
                faqRepository.deleteByTargetId(targetId);
            }
        } catch (Exception e) {
            log.error("Failed to process test-event message: {}", message, e);
        }
    }
}