package com.example.attemptservice.worker;

import com.example.attemptservice.config.AttemptTimingProperties;
import com.example.attemptservice.entity.Attempt;
import com.example.attemptservice.entity.AttemptStatus;
import com.example.attemptservice.repository.AttemptRepository;
import com.example.attemptservice.service.AttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZombieSessionSweeper {

    private final AttemptRepository attemptRepository;
    private final AttemptService attemptService;
    private final AttemptTimingProperties timingProperties;

    @Scheduled(fixedDelayString = "120000") // Runs every 2 minutes
    public void sweepZombieSessions() {
        log.debug("Starting zombie session sweeper...");
        List<Attempt> inProgress = attemptRepository.findByStatus(AttemptStatus.IN_PROGRESS);
        
        // Wait out the save grace period so last-moment answers are flushed before finalizing.
        Instant now = Instant.now().minusSeconds(timingProperties.saveGraceSeconds());
        List<Attempt> expiredAttempts = inProgress.stream()
                .filter(a -> a.getStartedAt().plusSeconds(a.getDurationMinutes() * 60L).isBefore(now))
                .collect(Collectors.toList());

        for (Attempt attempt : expiredAttempts) {
            try {
                log.info("Expiring zombie session attemptId: {}", attempt.getId());
                attemptService.finalizeAttempt(attempt, AttemptStatus.EXPIRED);
            } catch (Exception e) {
                log.error("Failed to expire attemptId: {}", attempt.getId(), e);
            }
        }
    }
}
