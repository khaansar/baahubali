package com.example.communityservice.kafka;

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

    @KafkaListener(
            topics = "test-events", 
            groupId = "community-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeTestEvent(DomainEvent event) {
        log.info("Received {} event [{}] for {} ID: {}", 
                event.eventType(), event.eventId(), event.aggregateType(), event.aggregateId());

        if ("TEST_DELETED".equals(event.eventType()) && "TEST".equals(event.aggregateType())) {
            reviewRepository.softDeleteByTargetId(event.aggregateId());
            log.info("Successfully soft-deleted reviews for testId: {}", event.aggregateId());
        }
    }
}