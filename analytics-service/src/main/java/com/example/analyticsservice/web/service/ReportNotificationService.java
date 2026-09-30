package com.example.analyticsservice.web.service;

import com.example.analyticsservice.web.event.ReportReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportNotificationService implements MessageListener {

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    public static final String TOPIC = "analytics:report_ready";

    public SseEmitter subscribe(String attemptId) {
        SseEmitter emitter = new SseEmitter(300_000L);

        CopyOnWriteArrayList<SseEmitter> group =
                emitters.computeIfAbsent(attemptId, k -> new CopyOnWriteArrayList<>());

        group.add(emitter);

        Runnable remove = () -> {
            group.remove(emitter);

            if (group.isEmpty()) {
                emitters.remove(attemptId, group);
            }
        };

        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        return emitter;
    }

    @EventListener
    public void onReportReadyInternal(ReportReadyEvent event) {
        redisTemplate.convertAndSend(TOPIC, event.attemptId());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String attemptId = new String(message.getBody(), StandardCharsets.UTF_8);

        CopyOnWriteArrayList<SseEmitter> group = emitters.get(attemptId);

        if (group == null) {
            return;
        }

        String payload;

        try {
            payload = jsonMapper.writeValueAsString(
                    Map.of(
                            "type", "REPORT_READY",
                            "attemptId", attemptId
                    )
            );
        } catch (Exception e) {
            log.error("Failed to serialize REPORT_READY payload", e);
            return;
        }

        for (SseEmitter emitter : group) {
            try {
                emitter.send(SseEmitter.event().name("REPORT_READY").data(payload));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }
}