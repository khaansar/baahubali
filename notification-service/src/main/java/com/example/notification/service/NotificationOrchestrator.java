package com.example.notification.service;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationAttempt;
import com.example.notification.config.NotificationProperties;
import com.example.notification.event.DomainEvent;
import com.example.notification.provider.NotificationProvider;
import com.example.notification.repository.NotificationAttemptRepository;
import com.example.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOrchestrator {

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository notificationAttemptRepository;
    private final TemplateService templateService;
    private final List<NotificationProvider> providers;
    private final ObjectMapper objectMapper;
    private final NotificationProperties notificationProperties;

    @Transactional
    public void process(DomainEvent event) {
        if (event.eventId() == null || event.eventType() == null) {
            log.warn("Ignoring invalid notification event");
            return;
        }

        if (event.recipient() == null || event.recipient().isBlank()) {
            log.warn(
                    "Ignoring notification event without recipient eventId={}, eventType={}",
                    event.eventId(),
                    event.eventType()
            );
            return;
        }

        Notification.Channel channel = resolveChannel(event);

        if (notificationRepository
                .findByEventIdAndChannel(event.eventId(), channel)
                .isPresent()) {
            log.debug(
                    "Notification already exists for eventId={}, channel={}",
                    event.eventId(),
                    channel
            );
            return;
        }

        Notification notification = Notification.builder()
                .eventId(event.eventId())
                .userId(event.userId())
                .eventType(event.eventType())
                .channel(channel)
                .recipient(event.recipient())
                .templateCode(resolveTemplate(event.eventType(), channel))
                .status(Notification.Status.PENDING)
                .payload(writePayload(event.payload()))
                .attemptCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        notificationRepository.save(notification);
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
                .orElseThrow(() -> new IllegalStateException(
                        "No notification provider for " + notification.getChannel()
                ));

        notification.setStatus(Notification.Status.PROCESSING);
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        notificationRepository.save(notification);

        try {
            TemplateService.RenderedTemplate template = templateService.render(
                    notification.getTemplateCode(),
                    notification.getChannel(),
                    notification.getPayload()
            );

            NotificationProvider.DeliveryResult result = provider.send(
                    notification,
                    template.subject(),
                    template.body()
            );

            int attemptNumber = notification.getAttemptCount();

            notificationAttemptRepository.save(
                    NotificationAttempt.builder()
                            .notificationId(notification.getId())
                            .provider(result.provider())
                            .attemptNumber(attemptNumber)
                            .status(result.successful()
                                    ? NotificationAttempt.Status.SUCCESS
                                    : NotificationAttempt.Status.FAILED)
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
            notification.setLastError(e.getMessage());
            notificationRepository.save(notification);

            log.error(
                    "Notification delivery failed notificationId={}",
                    notification.getId(),
                    e
            );
        }
    }

    private Notification.Channel resolveChannel(DomainEvent event) {
        Object channel = event.payload() == null
                ? null
                : event.payload().get("channel");

        if (channel == null) {
            return Notification.Channel.EMAIL;
        }

        return Notification.Channel.valueOf(channel.toString().toUpperCase());
    }

    private String resolveTemplate(
            String eventType,
            Notification.Channel channel
    ) {
        return eventType + "_" + channel.name() + "_V1";
    }

    private String writePayload(Object payload) {
        try {
            if (payload == null) {
                return "{}";
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize notification payload",
                    e
            );
        }
    }
}
