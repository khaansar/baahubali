package com.example.payment.controller.admin;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.AdminProductView;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.service.AdminProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final AdminProductService service;
    private final CurrentUserResolver me;

    @GetMapping
    public ApiResponse<PageResponse<AdminProductView>> list(@RequestParam(required = false) ProductType type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<UUID> ids,
            @RequestParam(required = false) List<UUID> referenceIds,
            @RequestParam(defaultValue = "false") boolean paidOnly,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest req) {
        me.requireAdmin(req);
        return ApiResponse.ok(service.list(type, q, ids, referenceIds, paidOnly, page, size));
    }
}
