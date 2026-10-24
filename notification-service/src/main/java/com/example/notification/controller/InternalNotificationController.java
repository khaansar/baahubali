
package com.example.notification.controller;

import com.example.notification.dto.ApiResponse;
import com.example.notification.entity.Notification;
import com.example.notification.provider.EmailProvider;
import com.example.notification.provider.NotificationProvider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final EmailProvider emailProvider;

    @Value("${app.api.version}")
    private String apiVersion;

    @PostMapping("/email/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestEmail(
            @Valid @RequestBody TestEmailRequest request
    ) {
        Notification notification = Notification.builder()
                .id("smtp-test")
                .eventId("smtp-test")
                .eventType("SMTP_TEST")
                .channel(Notification.Channel.EMAIL)
                .recipient(request.recipient())
                .templateCode("SMTP_TEST_EMAIL_V1")
                .payload("{}")
                .status(Notification.Status.PROCESSING)
                .attemptCount(1)
                .build();

        NotificationProvider.DeliveryResult result =
                emailProvider.send(
                        notification,
                        request.subject(),
                        request.body()
                );

        if (!result.successful()) {
            return ResponseEntity.internalServerError().body(ApiResponse.failure(500,
                    "Test email could not be sent", "EMAIL_DELIVERY_FAILED", null, apiVersion));
        }

        return ResponseEntity.accepted().body(ApiResponse.success(202,
                "Test email accepted for delivery", Map.<String, Object>of(), apiVersion));
    }

    public record TestEmailRequest(
            @NotBlank
            @Email
            String recipient,

            @NotBlank
            String subject,

            @NotBlank
            String body
    ) {
    }
}
