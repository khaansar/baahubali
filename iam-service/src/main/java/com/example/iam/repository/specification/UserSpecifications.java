package com.example.iam.repository.specification;

import com.example.iam.entity.Role;
import com.example.iam.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class UserSpecifications {

    private UserSpecifications() { }

    public static Specification<User> buildFilter(
            String search, String firstName, String lastName, String email,
            Role role, Boolean isActive, Boolean isDeleted,
            Integer minTestsAttemptedCount, Integer maxTestsAttemptedCount,
            Instant createdAfter, Instant createdBefore,
            Instant updatedAfter, Instant updatedBefore,
            Instant deletedAfter, Instant deletedBefore) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (hasText(search)) {
                String pattern = prefixPattern(search);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("firstName"), pattern, '\\'),
                        criteriaBuilder.like(root.get("lastName"), pattern, '\\'),
                        criteriaBuilder.like(root.get("email"), pattern, '\\')
                ));
            }
            addContains(predicates, criteriaBuilder, root.get("firstName"), firstName);
            addContains(predicates, criteriaBuilder, root.get("lastName"), lastName);
            addContains(predicates, criteriaBuilder, root.get("email"), email);
            if (role != null) predicates.add(criteriaBuilder.equal(root.get("role"), role));
            if (isActive != null) predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            if (isDeleted != null) {
                predicates.add(isDeleted
                        ? criteriaBuilder.isNotNull(root.get("deletedAt"))
                        : criteriaBuilder.isNull(root.get("deletedAt")));
            }
            if (minTestsAttemptedCount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("testsAttemptedCount"), minTestsAttemptedCount));
            }
            if (maxTestsAttemptedCount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("testsAttemptedCount"), maxTestsAttemptedCount));
            }
            addRange(predicates, criteriaBuilder, root.get("createdAt"), createdAfter, createdBefore);
            addRange(predicates, criteriaBuilder, root.get("updatedAt"), updatedAfter, updatedBefore);
            addRange(predicates, criteriaBuilder, root.get("deletedAt"), deletedAfter, deletedBefore);

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static void addContains(List<jakarta.persistence.criteria.Predicate> predicates,
                                    jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
                                    jakarta.persistence.criteria.Path<String> path,
                                    String value) {
        if (hasText(value)) {
            predicates.add(criteriaBuilder.like(path, prefixPattern(value), '\\'));
        }
    }

    private static void addRange(List<jakarta.persistence.criteria.Predicate> predicates,
                                 jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
                                 jakarta.persistence.criteria.Path<Instant> path,
                                 Instant after, Instant before) {
        if (after != null) predicates.add(criteriaBuilder.greaterThanOrEqualTo(path, after));
        if (before != null) predicates.add(criteriaBuilder.lessThanOrEqualTo(path, before));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * A trailing wildcard allows MySQL/TiDB to use a B-tree index. Leading wildcards and
     * REGEXP would force a scan, so user-directory search deliberately matches prefixes.
     */
    private static String prefixPattern(String value) {
        return value.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
    }
}
