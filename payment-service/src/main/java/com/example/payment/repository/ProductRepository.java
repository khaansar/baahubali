package com.example.payment.repository;

import com.example.payment.entity.Product;
import com.example.payment.entity.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByProductTypeAndReferenceId(ProductType type, UUID referenceId);
}