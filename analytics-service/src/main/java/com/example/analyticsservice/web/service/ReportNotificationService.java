package com.example.analyticsservice.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.example.analyticsservice.web.event.ReportReadyEvent;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportNotificationService implements MessageListener {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public static final String TOPIC = "analytics:report_ready";

    public SseEmitter subscribe(String attemptId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minutes timeout
        CopyOnWriteArrayList<SseEmitter> group = emitters.computeIfAbsent(attemptId, k -> new CopyOnWriteArrayList<>());
        group.add(emitter);

        Runnable remove = () -> {
            group.remove(emitter);
            if (group.isEmpty()) {
                emitters.remove(attemptId);
            }
        };

        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        return emitter;
    }

    @EventListener
    public void onReportReadyInternal(ReportReadyEvent event) {
        // Publish to Redis so all pods can notify their connected SSE clients
        redisTemplate.convertAndSend(TOPIC, event.attemptId());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String attemptId = new String(message.getBody());
        CopyOnWriteArrayList<SseEmitter> group = emitters.get(attemptId);
        if (group != null) {
            String payload;
            try {
                payload = objectMapper.writeValueAsString(Map.of(
                        "type", "REPORT_READY",
                        "attemptId", attemptId
                ));
            } catch (Exception e) {
                log.error("Failed to serialize REPORT_READY payload", e);
                return;
            }

            for (SseEmitter emitter : group) {
                try {
                    emitter.send(SseEmitter.event().data(payload));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }
        }
    }
}
