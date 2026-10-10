package com.example.payment.service;

import com.example.payment.dto.Views.ProductView;
import com.example.payment.entity.Product;
import com.example.payment.entity.ProductPrice;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.exception.ErrorCode;
import com.example.payment.exception.PaymentException;
import com.example.payment.repository.ProductPriceRepository;
import com.example.payment.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    public record ProductSyncRequest(ProductType productType, UUID referenceId, String name, Boolean active,
                                     Long amountMinor, String currency) {}

    private final ProductRepository products;
    private final ProductPriceRepository prices;

    /** Idempotent upsert called by Test Service. A price change SUPERSEDES the old row: history is preserved. */
    @Transactional
    public Product sync(ProductSyncRequest r) {
        Product p = products.findByProductTypeAndReferenceId(r.productType(), r.referenceId()).orElseGet(() -> {
            Product n = new Product(); n.setProductType(r.productType()); n.setReferenceId(r.referenceId()); return n; });
        p.setName(r.name());
        p.setStatus(Boolean.TRUE.equals(r.active()) ? "ACTIVE" : "INACTIVE");
        products.save(p);
        if (r.amountMinor() != null) {
            if (r.amountMinor() < 0) throw new PaymentException(ErrorCode.VALIDATION_FAILED, "Negative price");
            String cur = r.currency() == null ? "INR" : r.currency();
            Instant now = Instant.now();
            var current = prices.findActive(p.getId(), cur, now);
            if (current.isEmpty() || current.get().getAmountMinor() != r.amountMinor()) {
                current.ifPresent(c -> { c.setEffectiveUntil(now); c.setStatus("SUPERSEDED"); prices.save(c); });
                ProductPrice np = new ProductPrice();
                np.setProductId(p.getId()); np.setCurrency(cur); np.setAmountMinor(r.amountMinor()); np.setEffectiveFrom(now);
                prices.save(np);
            }
        }
        return p;
    }

    @Transactional(readOnly = true)
    public ProductView byReference(ProductType type, UUID referenceId, String currency) {
        Product p = products.findByProductTypeAndReferenceId(type, referenceId).filter(Product::isActive)
            .orElseThrow(() -> new PaymentException(ErrorCode.NOT_FOUND, "Product not found"));
        Long price = prices.findActive(p.getId(), currency, Instant.now()).map(ProductPrice::getAmountMinor).orElse(null);
        return new ProductView(p.getId(), p.getProductType(), p.getReferenceId(), p.getName(), price, currency);
    }
}