package com.example.notification.service;

import com.example.notification.config.NotificationProperties;
import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationAttempt;
import com.example.notification.event.DomainEvent;
import com.example.notification.provider.NotificationProvider;
import com.example.notification.repository.NotificationAttemptRepository;
import com.example.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestrator {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;
    private final TemplateService templateService;
    private final NotificationRecipientResolver recipientResolver;
    private final List<NotificationProvider> providers;
    private final ObjectMapper objectMapper;
    private final NotificationProperties notificationProperties;

    @Transactional
    public void process(DomainEvent event) {
        if (event == null || isBlank(event.eventId()) || isBlank(event.eventType())) {
            log.warn("Ignoring invalid notification event");
            return;
        }

        if (isBlank(event.userId())) {
            log.warn("Ignoring notification event without userId: eventId={}, eventType={}", event.eventId(), event.eventType());
            return;
        }

        for (Notification.Channel channel : resolveChannels(event)) {
            processChannel(event, channel);
        }
    }

    private void processChannel(DomainEvent event, Notification.Channel channel) {
        if (notificationRepository.findByEventIdAndChannel(event.eventId(), channel).isPresent()) {
            log.debug("Notification already exists: eventId={}, channel={}", event.eventId(), channel);
            return;
        }

        String recipient = event.recipient();

        if (channel == Notification.Channel.EMAIL && isBlank(recipient)) {
            NotificationRecipientResolver.Contact contact = recipientResolver.resolve(event.userId());

            if (contact == null) {
                log.warn("Skipping EMAIL notification because no verified email was resolved: eventId={}, userId={}", event.eventId(), event.userId());
                return;
            }

            recipient = contact.email();
        }

        if (channel == Notification.Channel.EMAIL && isBlank(recipient)) {
            return;
        }

        Map<String, Object> payload = mutablePayload(event.payload());

        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .userId(event.userId())
                .eventType(event.eventType())
                .channel(channel)
                .recipient(channel == Notification.Channel.EMAIL ? recipient : null)
                .templateCode(resolveTemplate(event.eventType(), channel))
                .status(Notification.Status.PENDING)
                .payload(writePayload(payload))
                .attemptCount(0)
                .title(resolveTitle(event.eventType(), payload))
                .message(resolveMessage(event.eventType(), payload))
                .actionUrl(resolveActionUrl(event.eventType(), payload))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        /*
         * The unique (event_id, channel) constraint is the final
         * idempotency guard if two consumers process the same event
         * concurrently. Kafka delivery can be at least once.
         */
        try {
            notificationRepository.saveAndFlush(notification);
        } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
            log.debug("Duplicate notification suppressed: eventId={}, channel={}", event.eventId(), channel);
            return;
        }

        deliver(notification);
    }

    @Transactional
    public void deliver(Notification notification) {
        if (notification.getStatus() == Notification.Status.SENT) {
            return;
        }

        if (notification.getAttemptCount() >= notificationProperties.getMaxAttempts()) {
            notification.setStatus(Notification.Status.FAILED);
            notification.setLastError("Maximum delivery attempts exceeded");
            notificationRepository.save(notification);
            return;
        }

        NotificationProvider provider = providers.stream()
                .filter(candidate -> candidate.getChannel() == notification.getChannel())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No notification provider for " + notification.getChannel()));

        notification.setStatus(Notification.Status.PROCESSING);
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        notificationRepository.save(notification);

        try {
            TemplateService.RenderedTemplate rendered = templateService.render(
                    notification.getTemplateCode(),
                    notification.getChannel(),
                    notification.getPayload()
            );

            NotificationProvider.DeliveryResult result = provider.send(
                    notification,
                    rendered.subject(),
                    rendered.body()
            );

            notificationAttemptRepository.save(
                    NotificationAttempt.builder()
                            .notificationId(notification.getId())
                            .provider(result.provider())
                            .attemptNumber(notification.getAttemptCount())
                            .status(result.successful() ? NotificationAttempt.Status.SUCCESS : NotificationAttempt.Status.FAILED)
                            .providerMessageId(result.providerMessageId())
                            .errorCode(result.errorCode())
                            .errorMessage(result.errorMessage())
                            .createdAt(Instant.now())
                            .build()
            );

            if (result.successful()) {
                notification.setStatus(Notification.Status.SENT);
                notification.setSentAt(Instant.now());
                notification.setLastError(null);
            } else {
                notification.setStatus(Notification.Status.FAILED);
                notification.setLastError(result.errorMessage());
            }

            notificationRepository.save(notification);

        } catch (Exception e) {
            notification.setStatus(Notification.Status.FAILED);
            notification.setLastError(truncate(e.getMessage()));
            notificationRepository.save(notification);

            log.error("Notification delivery failed: notificationId={}, channel={}", notification.getId(), notification.getChannel(), e);
        }
    }

    private List<Notification.Channel> resolveChannels(DomainEvent event) {
        return switch (event.eventType()) {
            case "ATTEMPT_SUBMITTED" -> List.of(Notification.Channel.IN_APP);
            case "PAYMENT_CAPTURED" -> List.of(Notification.Channel.IN_APP, Notification.Channel.EMAIL);
            case "STREAK_AT_RISK" -> List.of(Notification.Channel.EMAIL);
            default -> {
                Object configured = event.payload() == null ? null : event.payload().get("channel");

                if (configured == null) {
                    yield List.of(Notification.Channel.EMAIL);
                }

                yield List.of(Notification.Channel.valueOf(configured.toString().toUpperCase()));
            }
        };
    }

    private String resolveTemplate(String eventType, Notification.Channel channel) {
        return eventType + "_" + channel.name() + "_V1";
    }

    private String resolveTitle(String eventType, Map<String, Object> payload) {
        return switch (eventType) {
            case "ATTEMPT_SUBMITTED" -> "Test submitted";
            case "PAYMENT_CAPTURED" -> "Payment successful";
            case "STREAK_AT_RISK" -> "Keep your streak alive";
            default -> eventType.replace('_', ' ');
        };
    }

    private String resolveMessage(String eventType, Map<String, Object> payload) {
        return switch (eventType) {
            case "ATTEMPT_SUBMITTED" -> "Your test has been submitted successfully.";
            case "PAYMENT_CAPTURED" -> "Your payment was successful. Your purchased access is ready.";
            case "STREAK_AT_RISK" -> "Submit a test today to keep your current streak alive.";
            default -> "You have a new notification.";
        };
    }

    private String resolveActionUrl(String eventType, Map<String, Object> payload) {
        return switch (eventType) {
            case "ATTEMPT_SUBMITTED" -> {
                String attemptId = stringValue(payload.get("attemptId"));
                yield isBlank(attemptId) ? null : "/attempt/" + attemptId + "/result";
            }
            case "PAYMENT_CAPTURED" -> {
                String slug = stringValue(payload.get("slug"));
                yield isBlank(slug) ? null : "/test-series/" + slug;
            }
            case "STREAK_AT_RISK" -> "/tests";
            default -> null;
        };
    }

    private Map<String, Object> mutablePayload(Object value) {
        if (value == null) {
            return new java.util.HashMap<>();
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new java.util.HashMap<>();
            map.forEach((key, item) -> {
                if (key != null) {
                    result.put(key.toString(), item);
                }
            });
            return result;
        }

        return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize notification payload", e);
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.substring(0, Math.min(value.length(), 1000));
    }
}