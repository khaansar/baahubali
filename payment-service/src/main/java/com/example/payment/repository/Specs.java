package com.example.payment.repository;

import org.springframework.data.jpa.domain.Specification;

public final class Specs {
    private Specs() {}
    /** null value => null spec => ignored by Specification.where(...).and(...) */
    public static <T> Specification<T> eq(String field, Object value) {
        return value == null ? null : (r, q, cb) -> cb.equal(r.get(field), value);
    }
}