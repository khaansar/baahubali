package com.example.attemptservice.worker;

import com.example.attemptservice.entity.OutboxEvent;
import com.example.attemptservice.repository.AttemptRepository;
import com.example.attemptservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreakReminderScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final AttemptRepository attemptRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void publishStreakReminders() {
        LocalDate today = LocalDate.now(ZONE);

        Instant yesterdayStart = today.minusDays(1).atStartOfDay(ZONE).toInstant();
        Instant todayStart = today.atStartOfDay(ZONE).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(ZONE).toInstant();

        List<String> userIds = attemptRepository.findUsersEligibleForStreakReminder(yesterdayStart, todayStart, tomorrowStart);

        for (String userId : userIds) {
            enqueueReminder(userId, today);
        }

        log.info("Streak reminder run completed: date={}, eligibleUsers={}", today, userIds.size());
    }

    private void enqueueReminder(String userId, LocalDate date) {
        /*
         * The same user/date always produces the same UUID. If the scheduled
         * job is retried or multiple service instances run it, the outbox
         * primary key prevents duplicate reminder events.
         */
        String dedupeKey = "streak-reminder:" + userId + ":" + date;
        String eventId = UUID.nameUUIDFromBytes(dedupeKey.getBytes(StandardCharsets.UTF_8)).toString();

        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("eventId", eventId);
            event.put("eventType", "STREAK_AT_RISK");
            event.put("version", 1);
            event.put("occurredAt", Instant.now().toString());
            event.put("source", "attempt-service");
            event.put("userId", userId);
            event.putNull("correlationId");
            event.putNull("recipient");

            ObjectNode payload = event.putObject("payload");
            payload.put("localDate", date.toString());
            payload.put("testsUrl", "/tests");

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(eventId)
                    .eventType("STREAK_AT_RISK")
                    .aggregateId(userId)
                    .payload(objectMapper.writeValueAsString(event))
                    .createdAt(Instant.now())
                    .build();

            outboxEventRepository.saveAndFlush(outboxEvent);

        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Streak reminder already enqueued: userId={}, date={}", userId, date);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to enqueue streak reminder for user " + userId, e);
        }
    }
}