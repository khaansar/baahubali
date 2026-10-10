package com.example.payment.repository;

import org.springframework.data.jpa.domain.Specification;

public final class Specs {

    private Specs() {
    }

    /**
     * Returns an unrestricted specification when the filter value is null.
     * Otherwise, applies an equality condition to the specified entity field.
     */
    public static <T> Specification<T> eq(String field, Object value) {
        if (value == null) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(field), value);
    }
}