package com.example.testservice.repository.specification;

import com.example.testservice.entity.Question;
import com.example.testservice.entity.SectionQuestion;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public class QuestionSpecifications {

    public static Specification<Question> buildFilter(String search, String type, String difficulty, Boolean isLocked, Boolean unusedOnly) {
        return Specification.where(searchLike(search))
                .and(typeEquals(type))
                .and(difficultyEquals(difficulty))
                .and(lockedEquals(isLocked))
                .and(isUnused(unusedOnly));
    }

    private static Specification<Question> searchLike(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;
            query.distinct(true);
            return cb.like(cb.lower(root.join("translations").get("questionText")), "%" + search.toLowerCase() + "%");
        };
    }

    private static Specification<Question> typeEquals(String type) {
        return (root, query, cb) -> type == null || type.isBlank() ? null :
                cb.equal(root.get("questionType"), type);
    }

    private static Specification<Question> difficultyEquals(String difficulty) {
        return (root, query, cb) -> difficulty == null || difficulty.isBlank() ? null :
                cb.equal(root.get("difficulty"), difficulty);
    }

    private static Specification<Question> lockedEquals(Boolean isLocked) {
        return (root, query, cb) -> isLocked == null ? null :
                cb.equal(root.get("isLocked"), isLocked);
    }

    private static Specification<Question> isUnused(Boolean unusedOnly) {
        return (root, query, cb) -> {
            if (unusedOnly == null || !unusedOnly) return null;
            Subquery<Long> subquery = query.subquery(Long.class);
            var sqRoot = subquery.from(SectionQuestion.class);
            subquery.select(cb.literal(1L))
                    .where(cb.equal(sqRoot.get("question").get("id"), root.get("id")));
            return cb.not(cb.exists(subquery));
        };
    }
}