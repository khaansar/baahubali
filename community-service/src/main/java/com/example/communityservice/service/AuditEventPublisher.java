package com.example.communityservice.service;

import com.example.common.audit.AuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    private static final String AUDIT_EVENTS_TOPIC = "audit.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAfterCommit(
            String actorId,
            String action,
            String resourceType,
            String resourceId,
            String endpoint,
            String httpMethod,
            int statusCode,
            Map<String, String> metadata) {

        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                actorId,
                "ADMIN",
                action,
                resourceType,
                resourceId,
                "community-service",
                endpoint,
                httpMethod,
                statusCode,
                null,
                metadata,
                Instant.now());

        Runnable publish = () -> send(event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
            return;
        }

        publish.run();
    }

    private void send(AuditEvent event) {
        String key = event.resourceId() == null ? event.eventId().toString() : event.resourceId();
        try {
            kafkaTemplate.send(AUDIT_EVENTS_TOPIC, key, event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish audit event {} for action {}",
                                    event.eventId(), event.action(), exception);
                        }
                    });
        } catch (Exception exception) {
            log.error("Failed to enqueue audit event {} for action {}",
                    event.eventId(), event.action(), exception);
        }
    }
}
