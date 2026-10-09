package com.example.payment.controller;

import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.service.WebhookProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookProcessor processor;

    @PostMapping("/webhooks/razorpay")
    public ResponseEntity<Void> razorpay(@RequestBody String rawBody, @RequestHeader(value = "X-Razorpay-Signature", required = false) String sig, @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        try {
            processor.receive(rawBody, sig, eventId);

            return ResponseEntity.ok().build();

        } catch (PaymentException e) {
            if (e.getCode() == ErrorCode.WEBHOOK_SIGNATURE_INVALID) {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.status(500).build();
        }
    }
}