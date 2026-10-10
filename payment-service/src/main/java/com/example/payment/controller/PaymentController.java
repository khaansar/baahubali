package com.example.payment.controller;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.VerifyPaymentRequest;
import com.example.payment.dto.Views.PaymentStatusView;
import com.example.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    private final CurrentUserResolver me;

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentStatusView> get(@PathVariable UUID paymentId, HttpServletRequest req) {
        return ApiResponse.ok(service.get(me.userId(req), paymentId));
    }
    @PostMapping("/{paymentId}/verify")
    public ApiResponse<PaymentStatusView> verify(@PathVariable UUID paymentId, @Valid @RequestBody VerifyPaymentRequest body,
                                                 HttpServletRequest req) {
        return ApiResponse.ok(service.verify(me.userId(req), paymentId, body));
    }
}