package com.example.iam.event;

import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttemptSubmittedEventConsumer {

    private final UserRepository userRepository;

    @KafkaListener(
            topics = "attempt-submitted-events",
            groupId = "iam-service-attempt-stats",
            containerFactory = "attemptSubmittedKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(AttemptSubmittedEvent event) {
        int updatedRows = userRepository.incrementTestsAttemptedCount(event.userId());
        if (updatedRows == 0) {
            throw new IllegalStateException("No IAM user exists for attempt-submitted event userId="
                    + event.userId() + ", testId=" + event.testId());
        }
        log.debug("Incremented tests_attempted_count for userId={} after testId={} submission at {}",
                event.userId(), event.testId(), event.timestamp());
    }
}
