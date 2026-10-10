package com.example.payment.controller;

import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.Views.ProductView;
import com.example.payment.service.ProductService;
import com.example.payment.service.ProductService.ProductSyncRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {
    private final ProductService products;

    @PutMapping("/sync")
    public ApiResponse<ProductView> sync(@RequestBody @NotNull ProductSyncRequest r) {
        var p = products.sync(r);
        return ApiResponse.ok(new ProductView(p.getId(), p.getProductType(), p.getReferenceId(), p.getName(), r.amountMinor(), r.currency()));
    }
}