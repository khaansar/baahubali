package com.example.payment.controller.admin;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.PaymentView;
import com.example.payment.entity.enums.PaymentStatus;
import com.example.payment.service.AdminQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {
    private final AdminQueryService query;
    private final CurrentUserResolver me;

    @GetMapping
    public ApiResponse<PageResponse<PaymentView>> list(@RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) UUID userId, @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String providerPaymentId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(query.payments(orderId, userId, status, providerPaymentId, page, size));
    }
}