package com.example.payment.controller;

import com.example.payment.config.CurrentUserResolver;
import com.example.payment.config.PaymentProperties;
import com.example.payment.dto.ApiResponse;
import com.example.payment.dto.Views.ProductView;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductLookupController {
    private final ProductService products;
    private final CurrentUserResolver me;
    private final PaymentProperties props;

    @GetMapping("/by-reference")
    public ApiResponse<ProductView> byReference(@RequestParam ProductType type, @RequestParam UUID referenceId, HttpServletRequest req) {
        me.userId(req);   // must be authenticated
        return ApiResponse.ok(products.byReference(type, referenceId, props.getCurrencyDefault()));
    }
}