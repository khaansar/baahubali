package com.example.payment.controller.admin;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.AdminRequests.RefundRequest;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.RefundView;
import com.example.payment.dto.Views.RefundableView;
import com.example.payment.entity.enums.RefundStatus;
import com.example.payment.service.AdminQueryService;
import com.example.payment.service.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRefundController {
    private final RefundService refunds;
    private final AdminQueryService query;
    private final CurrentUserResolver me;

    @GetMapping("/refunds")
    public ApiResponse<PageResponse<RefundView>> list(@RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(query.refunds(orderId, status, page, size));
    }
    @GetMapping("/payments/{paymentId}/refundable")
    public ApiResponse<RefundableView> refundable(@PathVariable UUID paymentId, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(query.refundable(paymentId));
    }
    /** Idempotency-Key is mandatory; the key + paymentId uniquely identifies a refund intent (DB unique constraint). */
    @PostMapping("/payments/{paymentId}/refunds")
    public ApiResponse<RefundView> request(@PathVariable UUID paymentId, @Valid @RequestBody RefundRequest body,
            @RequestHeader("Idempotency-Key") String idemKey, HttpServletRequest req) {
        UUID adminId = me.requireRefundAdmin(req);
        return ApiResponse.ok(RefundView.of(refunds.request(adminId, paymentId, body.amount(), body.reason(), idemKey)));
    }
}