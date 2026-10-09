package com.example.payment.controller.admin;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.AdminRequests.GrantRequest;
import com.example.payment.dto.AdminRequests.RevokeRequest;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.EntitlementView;
import com.example.payment.entity.enums.EntitlementStatus;
import com.example.payment.service.AdminQueryService;
import com.example.payment.service.EntitlementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/admin/entitlements")
@RequiredArgsConstructor
public class AdminEntitlementController {
    private final EntitlementService service;
    private final AdminQueryService query;
    private final CurrentUserResolver me;

    @GetMapping
    public ApiResponse<PageResponse<EntitlementView>> list(@RequestParam(required = false) UUID userId,
            @RequestParam(required = false) EntitlementStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(query.entitlements(userId, status, page, size));
    }
    @PostMapping("/grant")
    public ApiResponse<EntitlementView> grant(@Valid @RequestBody GrantRequest b, HttpServletRequest req) {
        UUID admin = me.requireRefundAdmin(req);      // high-risk: same role set as refunds
        return ApiResponse.ok(EntitlementView.of(
            service.grantManual(admin, b.userId(), b.productType(), b.productReferenceId(), b.source(), b.reason(), b.expiresAt())));
    }
    @PostMapping("/{id}/revoke")
    public ApiResponse<Void> revoke(@PathVariable UUID id, @Valid @RequestBody RevokeRequest b, HttpServletRequest req) {
        service.revokeById(me.requireRefundAdmin(req), id, b.reason());
        return ApiResponse.ok(null);
    }
}