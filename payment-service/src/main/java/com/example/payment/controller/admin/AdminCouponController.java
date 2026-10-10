package com.example.payment.controller.admin;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.AdminRequests.CouponRequest;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.CouponView;
import com.example.payment.dto.Views.RedemptionView;
import com.example.payment.service.AdminCouponService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {
    private final AdminCouponService service;
    private final CurrentUserResolver me;

    @GetMapping
    public ApiResponse<PageResponse<CouponView>> list(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(service.list(q, page, size));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<CouponView>> create(@Valid @RequestBody CouponRequest body, HttpServletRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.ok(service.create(me.requireAdmin(req), body)));
    }
    @PutMapping("/{id}")
    public ApiResponse<CouponView> update(@PathVariable UUID id, @Valid @RequestBody CouponRequest body, HttpServletRequest req) {
        return ApiResponse.ok(service.update(me.requireAdmin(req), id, body));
    }
    @PostMapping("/{id}/enable")
    public ApiResponse<CouponView> enable(@PathVariable UUID id, HttpServletRequest req) {
        return ApiResponse.ok(service.setEnabled(me.requireAdmin(req), id, true));
    }
    @PostMapping("/{id}/disable")
    public ApiResponse<CouponView> disable(@PathVariable UUID id, HttpServletRequest req) {
        return ApiResponse.ok(service.setEnabled(me.requireAdmin(req), id, false));
    }
    @GetMapping("/{id}/redemptions")
    public ApiResponse<PageResponse<RedemptionView>> redemptions(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(service.redemptions(id, page, size));
    }
}