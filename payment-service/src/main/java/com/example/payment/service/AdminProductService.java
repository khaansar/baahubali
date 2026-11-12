package com.example.payment.service;

import com.example.payment.config.PaymentProperties;
import com.example.payment.dto.PageResponse;
import com.example.payment.dto.Views.AdminProductView;
import com.example.payment.entity.Product;
import com.example.payment.entity.ProductPrice;
import com.example.payment.entity.enums.ProductType;
import com.example.payment.repository.Paging;
import com.example.payment.repository.ProductPriceRepository;
import com.example.payment.repository.ProductRepository;
import com.example.payment.repository.Specs;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Read-only product catalog for admin pickers (coupon applicability, manual access grants). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {
    private final ProductRepository products;
    private final ProductPriceRepository prices;
    private final PaymentProperties props;

    public PageResponse<AdminProductView> list(ProductType type, String q, Collection<UUID> ids, Collection<UUID> referenceIds,
                                               boolean paidOnly, int page, int size) {
        String currency = props.getCurrencyDefault();
        Instant now = Instant.now();
        Specification<Product> spec = Specification.<Product>unrestricted()
            .and((root, query, cb) -> cb.isNull(root.get("deletedAt")))
            .and(Specs.eq("productType", type))
            .and(nameContains(q))
            .and(in("id", ids))
            .and(in("referenceId", referenceIds))
            .and(paidOnly ? hasPositivePrice(currency, now) : Specification.unrestricted());

        Page<Product> result = products.findAll(spec, Paging.of(page, size));
        Map<UUID, Long> priceByProduct = new HashMap<>();
        if (!result.isEmpty()) {
            // Rows are ordered newest-first, so the first row seen per product is its current price.
            for (ProductPrice p : prices.findActiveFor(result.map(Product::getId).toList(), currency, now))
                priceByProduct.putIfAbsent(p.getProductId(), p.getAmountMinor());
        }
        return PageResponse.of(result.map(p -> new AdminProductView(p.getId(), p.getProductType(), p.getReferenceId(),
            p.getName(), p.getStatus(), priceByProduct.get(p.getId()), currency)));
    }

    private static Specification<Product> nameContains(String q) {
        if (q == null || q.isBlank()) return Specification.unrestricted();
        String pattern = "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    private static Specification<Product> in(String field, Collection<UUID> values) {
        if (values == null || values.isEmpty()) return Specification.unrestricted();
        return (root, query, cb) -> root.get(field).in(values);
    }

    private static Specification<Product> hasPositivePrice(String currency, Instant now) {
        return (root, query, cb) -> {
            Subquery<UUID> sq = query.subquery(UUID.class);
            Root<ProductPrice> price = sq.from(ProductPrice.class);
            sq.select(price.get("productId")).where(
                cb.equal(price.get("productId"), root.get("id")),
                cb.equal(price.get("currency"), currency),
                cb.equal(price.get("status"), "ACTIVE"),
                cb.lessThanOrEqualTo(price.get("effectiveFrom"), now),
                cb.or(cb.isNull(price.get("effectiveUntil")), cb.greaterThan(price.get("effectiveUntil"), now)),
                cb.greaterThan(price.get("amountMinor"), 0L));
            return cb.exists(sq);
        };
    }
}
