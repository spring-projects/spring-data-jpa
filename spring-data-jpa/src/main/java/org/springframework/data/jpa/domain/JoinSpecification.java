package org.springframework.data.jpa.domain;

import jakarta.persistence.criteria.*;

/**
 * Abstract base class for specifications which apply specifications on another joined entity.
 *
 * @param <T>
 * @param <S>
 */
public abstract class JoinSpecification<T, S> implements NestableSpecification<T> {

    private NestableSpecification<S> specification;

    protected JoinSpecification(Specification<S> specification) {
        if (!(specification instanceof NestableSpecification<S>)) {
            throw new IllegalArgumentException("specification is non-nestable");
        }
        this.specification = (NestableSpecification<S>) specification;
    }

    @Override
    public final Predicate toPredicate(From<T, T> from, CriteriaQuery<?> query, CriteriaBuilder builder) {
        var joined = join(from);

        return specification.toPredicate(joined, query, builder);
    }

    /**
     * Join another entity.
     * @param from entity to join from
     * @return join
     */
    protected abstract Join<S, S> join(From<T, T> from);
}
