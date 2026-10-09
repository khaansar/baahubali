package com.example.payment.controller.admin;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.OrderDetailView;
import com.example.payment.dto.Views.OrderView;
import com.example.payment.dto.Views.TimelineEntry;
import com.example.payment.entity.enums.OrderStatus;
import com.example.payment.service.AdminQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
    private final AdminQueryService query;
    private final CurrentUserResolver me;

    @GetMapping
    public ApiResponse<PageResponse<OrderView>> list(@RequestParam(required = false) UUID userId,
            @RequestParam(required = false) OrderStatus status, @RequestParam(required = false) String orderNumber,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(query.orders(userId, status, orderNumber, page, size));
    }
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailView> detail(@PathVariable UUID id, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(query.orderDetail(id));
    }
    @GetMapping("/{id}/timeline")
    public ApiResponse<List<TimelineEntry>> timeline(@PathVariable UUID id, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(query.timeline(id));
    }
}