package com.example.payment.controller;

import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.Views.OrderDetailView;
import com.example.payment.service.AdminQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

/** Service-to-service only. Guarded by InternalAuthFilter (X-Internal-Token). Never routed by the gateway. */
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {
    private final AdminQueryService query;

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailView> get(@PathVariable UUID id) { return ApiResponse.ok(query.orderDetail(id)); }
}