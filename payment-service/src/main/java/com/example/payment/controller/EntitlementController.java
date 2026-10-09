package com.example.payment.controller;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.EntitlementView;
import com.example.payment.repository.EntitlementRepository;
import com.example.payment.repository.Paging;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/entitlements")
@RequiredArgsConstructor
public class EntitlementController {
    private final EntitlementRepository repo;
    private final CurrentUserResolver me;

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<EntitlementView>> mine(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size, HttpServletRequest req) {
        return ApiResponse.ok(PageResponse.of(repo.findByUserId(me.userId(req), Paging.of(page, size)).map(EntitlementView::of)));
    }
}