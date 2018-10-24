package org.springframework.data.jpa.domain;

import org.springframework.lang.Nullable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;

/**
 * Helper class to support specification compositions
 *
 * @author Sebastian Staudt
 * @see Specification
 */
class SpecificationComposition {

    /**
     * Enum for the composition types for {@link Predicate}s. Can not be turned into lambdas as we need to be
     * serializable.
     *
     * @author Thomas Darimont
     */
    enum CompositionType {

        AND {
            @Override
            public Predicate combine(CriteriaBuilder builder, Predicate lhs, Predicate rhs) {
                return builder.and(lhs, rhs);
            }
        },

        OR {
            @Override
            public Predicate combine(CriteriaBuilder builder, Predicate lhs, Predicate rhs) {
                return builder.or(lhs, rhs);
            }
        };

        abstract Predicate combine(CriteriaBuilder builder, Predicate lhs, Predicate rhs);
    }

    static <T> Specification<T> negated(@Nullable Specification<T> spec) {
        return (root, query, builder) -> spec == null ? null : builder.not(spec.toPredicate(root, query, builder));
    }

    static <T> Specification<T> composed(@Nullable Specification<T> lhs, @Nullable Specification<T> rhs, CompositionType compositionType) {

        return (root, query, builder) -> {

            Predicate otherPredicate = rhs == null ? null : rhs.toPredicate(root, query, builder);
            Predicate thisPredicate = lhs == null ? null : lhs.toPredicate(root, query, builder);

            return thisPredicate == null ? otherPredicate
                    : otherPredicate == null ? thisPredicate : compositionType.combine(builder, thisPredicate, otherPredicate);
        };
    }
}
