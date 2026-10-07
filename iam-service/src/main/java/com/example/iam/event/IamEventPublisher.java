package com.example.iam.event;

import com.example.iam.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class IamEventPublisher {

    private static final String IAM_EVENTS_TOPIC = "iam-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishEmailVerificationRequested(
            User user,
            String verificationUrl
    ) {
        EmailVerificationRequestedEvent event =
                new EmailVerificationRequestedEvent(
                        UUID.randomUUID().toString(),
                        "USER_EMAIL_VERIFICATION_REQUESTED",
                        1,
                        Instant.now(),
                        "iam-service",
                        user.getId().toString(),
                        null,
                        user.getEmail(),
                        Map.of(
                                "channel", "EMAIL",
                                "firstName", user.getFirstName(),
                                "verificationUrl", verificationUrl
                        )
                );

        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    IAM_EVENTS_TOPIC,
                    user.getId().toString(),
                    message
            ).whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error(
                            "Failed to publish email verification event userId={}",
                            user.getId(),
                            exception
                    );
                    return;
                }

                log.debug(
                        "Published email verification event userId={}",
                        user.getId()
                );
            });

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize email verification event",
                    e
            );
        }
    }
}