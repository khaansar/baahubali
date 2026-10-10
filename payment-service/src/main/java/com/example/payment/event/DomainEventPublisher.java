package com.example.payment.event;

import com.example.payment.config.PaymentProperties;
import com.example.payment.outbox.OutboxEvent;
import com.example.payment.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DomainEventPublisher {
    private final OutboxRepository outbox;
    private final PaymentProperties props;
    private final ObjectMapper mapper;

    /** Writes ONLY to the outbox, inside the caller's business transaction. Never put secrets in payload. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topicKey, String aggregateType, UUID aggregateId, UUID userId,
                        String type, Map<String, Object> payload) {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("eventId", eventId); env.put("eventType", type); env.put("occurredAt", Instant.now());
        env.put("aggregateId", aggregateId); env.put("userId", userId); env.put("payload", payload);
        OutboxEvent e = new OutboxEvent();
        e.setId(eventId);
        e.setTopic(props.getTopics().get(topicKey));
        e.setAggregateType(aggregateType); e.setAggregateId(aggregateId.toString()); e.setEventType(type);
        try { e.setPayload(mapper.writeValueAsString(env)); }
        catch (Exception ex) { throw new IllegalStateException("event serialization failed", ex); }
        outbox.save(e);
    }
}