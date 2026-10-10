package com.example.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter
@Entity @Table(name = "payment_webhook_events")
public class WebhookEvent extends AssignedIdEntity {
    @Column(nullable = false) private String provider;
    @Column(nullable = false) private String providerEventId;
    @Column(nullable = false) private String eventType;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT") private String payload;
    private boolean signatureValid;
    private boolean processed;
    private Instant processedAt;
    private int attempts;
    private String lastError;
    @Column(nullable = false) private Instant receivedAt = Instant.now();
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public static WebhookEvent of(String provider, String eventId, String type, String payload) {
        WebhookEvent w = new WebhookEvent();
        w.provider = provider; w.providerEventId = eventId; w.eventType = type;
        w.payload = payload; w.signatureValid = true;     // only signature-valid events are stored
        return w;
    }
}