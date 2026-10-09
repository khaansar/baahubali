package com.example.payment.controller;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.OrderView;
import com.example.payment.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;
    private final CurrentUserResolver me;

    @GetMapping
    public ApiResponse<PageResponse<OrderView>> list(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size, HttpServletRequest req) {
        return ApiResponse.ok(service.list(me.userId(req), page, size));
    }
    @GetMapping("/{orderId}")
    public ApiResponse<OrderView> get(@PathVariable UUID orderId, HttpServletRequest req) {
        return ApiResponse.ok(service.get(me.userId(req), orderId));
    }
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderView> cancel(@PathVariable UUID orderId, HttpServletRequest req) {
        return ApiResponse.ok(service.cancel(me.userId(req), orderId));
    }
}