package com.example.notification.service;

import com.example.notification.dto.InboxNotificationResponse;
import com.example.notification.entity.Notification;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component @Slf4j
public class InAppNotificationStream {

    private static final long NO_TIMEOUT = 0L;

    private final Map<String, Set<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        if (userId == null || userId.isBlank()) { throw new IllegalArgumentException("Authenticated user ID is required"); }
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        Set<SseEmitter> userEmitters = emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArraySet<>());
        userEmitters.add(emitter);
        Runnable cleanup = () -> removeEmitter(userId, userEmitters, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { cleanup.run(); emitter.complete(); });
        emitter.onError(error -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("connected", true)));
        } catch (IOException | IllegalStateException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void publishAfterCommit(Notification notification) {
        if (notification == null || notification.getChannel() != Notification.Channel.IN_APP || notification.getUserId() == null || notification.getUserId().isBlank() || notification.getId() == null) { return; }
        Runnable publish = () -> {
            InboxNotificationResponse response = InboxNotificationResponse.from(notification);
            publish(notification.getUserId(), response);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { publish.run(); }
            });
        } else {
            publish.run();
        }
    }

    private void publish(String userId, InboxNotificationResponse notification) {
        Set<SseEmitter> userEmitters = emittersByUser.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) { return; }
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().id(notification.id()).name("notification").data(notification));
            } catch (IOException | IllegalStateException exception) {
                removeEmitter(userId, userEmitters, emitter);
                emitter.completeWithError(exception);
                log.debug("Removed disconnected notification stream for userId={}", userId);
            }
        }
    }

    @Scheduled(fixedRateString = "${notification.sse-heartbeat-ms:25000}")
    public void sendHeartbeats() {
        emittersByUser.forEach((userId, userEmitters) -> {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                } catch (IOException | IllegalStateException exception) {
                    removeEmitter(userId, userEmitters, emitter);
                    emitter.completeWithError(exception);
                    log.debug("Removed inactive notification stream for userId={}", userId);
                }
            }
        });
    }

    private void removeEmitter(String userId, Set<SseEmitter> userEmitters, SseEmitter emitter) {
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) { emittersByUser.remove(userId, userEmitters); }
    }

    @PreDestroy
    public void closeStreams() {
        emittersByUser.forEach((userId, userEmitters) -> userEmitters.forEach(SseEmitter::complete));
        emittersByUser.clear();
    }
}