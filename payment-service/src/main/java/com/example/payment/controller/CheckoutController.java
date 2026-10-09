package com.example.payment.controller;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.CreateOrderRequest;
import com.example.payment.dto.CreateOrderResponse;
import com.example.payment.dto.QuoteRequest;
import com.example.payment.dto.QuoteResponse;
import com.example.payment.service.CheckoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkout;
    private final CurrentUserResolver me;

    @PostMapping("/checkout/quote")
    public ApiResponse<QuoteResponse> quote(@Valid @RequestBody QuoteRequest body, HttpServletRequest request) {
        return ApiResponse.ok(checkout.quote(me.userId(request), body));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> create(
        @Valid @RequestBody CreateOrderRequest body,
        @RequestHeader(value = "Idempotency-Key", required = false) String key,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(201).body(ApiResponse.ok(checkout.createOrder(me.userId(request), key, body)));
    }
}