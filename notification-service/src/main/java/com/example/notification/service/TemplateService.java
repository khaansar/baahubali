package com.example.notification.service;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationTemplate;
import com.example.notification.repository.NotificationTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public RenderedTemplate render(
            String templateCode,
            Notification.Channel channel,
            String payload
    ) {
        NotificationTemplate template =
                templateRepository
                        .findFirstByTemplateCodeAndChannelAndActiveTrueOrderByVersionDesc(
                                templateCode,
                                channel
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Notification template not found: "
                                                + templateCode
                                                + " / "
                                                + channel
                                )
                        );

        try {
            Map<String, Object> values = objectMapper.readValue(
                    payload,
                    new TypeReference<>() {
                    }
            );

            return new RenderedTemplate(
                    replace(template.getSubject(), values),
                    replace(template.getBody(), values)
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to render notification template " + templateCode,
                    e
            );
        }
    }

    private String replace(
            String template,
            Map<String, Object> values
    ) {
        if (template == null) {
            return null;
        }

        String result = template;

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            result = result.replace(
                    "{{" + entry.getKey() + "}}",
                    String.valueOf(entry.getValue())
            );
        }

        return result;
    }

    public record RenderedTemplate(
            String subject,
            String body
    ) {
    }
}
