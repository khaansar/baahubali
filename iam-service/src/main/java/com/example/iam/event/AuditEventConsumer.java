package com.example.iam.event;

import com.example.common.audit.AuditEvent;
import com.example.iam.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private static final String TOPIC_AUDIT_EVENTS = "audit.events";

    private final AuditLogService auditLogService;

    @KafkaListener(
            topics = TOPIC_AUDIT_EVENTS,
            groupId = "iam-audit-consumer",
            containerFactory = "auditEventKafkaListenerContainerFactory"
    )
    public void consume(AuditEvent event) {
        if (event == null) {
            log.warn("Received null audit event");
            return;
        }

        auditLogService.store(event);
    }
}