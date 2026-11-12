package com.example.notification.service;

import com.example.notification.config.NotificationProperties;
import com.example.notification.entity.Notification;
import com.example.notification.event.DomainEvent;
import com.example.notification.provider.NotificationProvider;
import com.example.notification.repository.NotificationAttemptRepository;
import com.example.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessGrantedNotificationTest {
    @Mock NotificationRepository notifications;
    @Mock NotificationAttemptRepository attempts;
    @Mock TemplateService templates;
    @Mock NotificationRecipientResolver recipients;
    @Mock InAppNotificationStream stream;
    final ObjectMapper mapper = new ObjectMapper();
    final List<Notification> saved = new ArrayList<>();
    NotificationOrchestrator orchestrator;

    @BeforeEach void setUp() {
        NotificationProperties props = new NotificationProperties();
        props.setFrontendBaseUrl("https://app.example.com/");
        props.setDisplayTimeZone(ZoneId.of("Asia/Kolkata"));
        List<NotificationProvider> providers = new ArrayList<>();
        for (Notification.Channel c : Notification.Channel.values()) {
            NotificationProvider p = mock(NotificationProvider.class);
            when(p.getChannel()).thenReturn(c);
            when(p.send(any(), any(), any())).thenReturn(NotificationProvider.DeliveryResult.success("test", "id"));
            providers.add(p);
        }
        when(notifications.findByEventIdAndChannel(any(), any())).thenReturn(Optional.empty());
        when(notifications.saveAndFlush(any())).thenAnswer(inv -> { saved.add(inv.getArgument(0)); return inv.getArgument(0); });
        when(recipients.resolve(any())).thenReturn(new NotificationRecipientResolver.Contact("user@example.com", "Asha"));
        when(templates.render(any(), any(), any())).thenReturn(new TemplateService.RenderedTemplate("s", "b"));
        orchestrator = new NotificationOrchestrator(notifications, attempts, templates, recipients, providers, mapper, props, stream);
    }

    DomainEvent event(Map<String, Object> payload) {
        return new DomainEvent(UUID.randomUUID().toString(), "ACCESS_GRANTED", 1, Instant.now(), "payment-service",
            UUID.randomUUID().toString(), null, null, payload);
    }

    Map<String, Object> emailPayload() throws Exception {
        Notification email = saved.stream().filter(n -> n.getChannel() == Notification.Channel.EMAIL).findFirst().orElseThrow();
        assertEquals("ACCESS_GRANTED_EMAIL_V1", email.getTemplateCode());
        return mapper.readValue(email.getPayload(), new TypeReference<>() {});
    }

    @Test void sendsEmailAndInAppWithEscapedReasonExpiryAndSeriesLink() throws Exception {
        Map<String, Object> payload = new HashMap<>(Map.of("seriesTitle", "SSC CGL Mocks", "slug", "ssc-cgl",
            "reason", "Scholarship <winner> & topper", "expiresAt", "2026-12-31T18:29:59Z"));
        orchestrator.process(event(payload));

        assertEquals(2, saved.size());
        Notification inApp = saved.stream().filter(n -> n.getChannel() == Notification.Channel.IN_APP).findFirst().orElseThrow();
        assertEquals("/test-series/ssc-cgl", inApp.getActionUrl());
        assertEquals("You now have access to SSC CGL Mocks.", inApp.getMessage());

        Map<String, Object> email = emailPayload();
        assertEquals("https://app.example.com/test-series/ssc-cgl", email.get("seriesUrl"));
        assertEquals("Asha", email.get("firstName"));
        String reasonBlock = (String) email.get("reasonBlock");
        assertTrue(reasonBlock.contains("Scholarship &lt;winner&gt; &amp; topper"), reasonBlock);
        assertFalse(reasonBlock.contains("<winner>"));
        assertEquals("Your access is valid until <strong>31 December 2026</strong>.", email.get("accessPeriod"));
    }

    @Test void omitsReasonAndFallsBackWhenOptionalFieldsMissing() throws Exception {
        orchestrator.process(event(new HashMap<>()));
        Map<String, Object> email = emailPayload();
        assertEquals("", email.get("reasonBlock"));
        assertEquals("Your access does not expire.", email.get("accessPeriod"));
        assertEquals("https://app.example.com/my-learning", email.get("seriesUrl"));
        assertEquals("a new test series", email.get("seriesTitle"));
    }
}
