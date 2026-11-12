package com.example.payment.service;

import com.example.payment.config.PaymentProperties;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.AdminProductView;
import com.example.payment.entity.Product;
import com.example.payment.entity.ProductPrice;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.repository.ProductPriceRepository;
import com.example.payment.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {
    @Mock ProductRepository products;
    @Mock ProductPriceRepository prices;

    AdminProductService service() { return new AdminProductService(products, prices, new PaymentProperties()); }

    static Product product(String name) {
        Product p = new Product();
        p.setId(UUID.randomUUID()); p.setProductType(ProductType.TEST_SERIES); p.setReferenceId(UUID.randomUUID()); p.setName(name);
        return p;
    }

    static ProductPrice price(UUID productId, long amount) {
        ProductPrice pp = new ProductPrice(); pp.setProductId(productId); pp.setAmountMinor(amount); return pp;
    }

    @SuppressWarnings("unchecked")
    @Test void attachesNewestActivePriceAndLeavesUnpricedProductsNull() {
        Product priced = product("Priced"), unpriced = product("Unpriced");
        when(products.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(priced, unpriced)));
        when(prices.findActiveFor(anyCollection(), eq("INR"), any()))
            .thenReturn(List.of(price(priced.getId(), 49_900), price(priced.getId(), 39_900)));

        PageResponse<AdminProductView> page = service().list(ProductType.TEST_SERIES, null, null, null, false, 0, 20);

        assertEquals(49_900L, page.content().get(0).currentPriceMinor());
        assertEquals(priced.getReferenceId(), page.content().get(0).referenceId());
        assertNull(page.content().get(1).currentPriceMinor());
    }

    @SuppressWarnings("unchecked")
    @Test void skipsPriceLookupForEmptyPage() {
        when(products.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, service().list(null, "x", null, null, true, 0, 20).content().size());
        verify(prices, never()).findActiveFor(anyCollection(), any(), any());
    }
}
