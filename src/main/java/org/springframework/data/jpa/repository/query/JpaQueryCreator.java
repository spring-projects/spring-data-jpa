/*
 * Copyright 2008-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.SimpleParameterAccessor;
import org.springframework.data.repository.query.SimpleParameterAccessor.BindableParameterIterator;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;


/**
 * Query creator to create a {@link CriteriaQuery} from a {@link PartTree}.
 * 
 * @author Oliver Gierke
 */
public class JpaQueryCreator extends
        AbstractQueryCreator<CriteriaQuery<Object>, Predicate> {

    private final CriteriaBuilder builder;
    private final Root<?> root;
    private final CriteriaQuery<Object> query;


    /**
     * Create a new {@link JpaQueryCreator}.
     * 
     * @param tree
     * @param parameters
     * @param domainClass
     * @param em
     */
    public JpaQueryCreator(PartTree tree, SimpleParameterAccessor parameters,
            Class<?> domainClass, EntityManager em) {

        super(tree, parameters);

        this.builder = em.getCriteriaBuilder();
        this.query = builder.createQuery();
        this.root = query.from(domainClass);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.query.parser.AbstractQueryCreator
     * #create(org.springframework.data.repository.query.parser.Part,
     * org.springframework
     * .data.repository.query.SimpleParameterAccessor.BindableParameterIterator)
     */
    @Override
    protected Predicate create(Part part, BindableParameterIterator iterator) {

        return toPredicate(part, root, iterator);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.query.parser.AbstractQueryCreator
     * #and(org.springframework.data.repository.query.parser.Part,
     * java.lang.Object,
     * org.springframework.data.repository.query.SimpleParameterAccessor
     * .BindableParameterIterator)
     */
    @Override
    protected Predicate and(Part part, Predicate base,
            BindableParameterIterator iterator) {

        return builder.and(base, toPredicate(part, root, iterator));
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.query.parser.AbstractQueryCreator
     * #or(java.lang.Object, java.lang.Object)
     */
    @Override
    protected Predicate or(Predicate base, Predicate predicate) {

        return builder.or(base, predicate);
    }


    /**
     * Finalizes the given {@link Predicate} and applies the given sort.
     * Delegates to
     * {@link #complete(Predicate, Sort, CriteriaQuery, CriteriaBuilder)} and
     * hands it the current {@link CriteriaQuery} and {@link CriteriaBuilder}.
     */
    @Override
    protected final CriteriaQuery<Object> complete(Predicate predicate,
            Sort sort) {

        return complete(predicate, sort, query, builder);
    }


    /**
     * Template method to finalize the given {@link Predicate} using the given
     * {@link CriteriaQuery} and {@link CriteriaBuilder}.
     * 
     * @param predicate
     * @param sort
     * @param query
     * @param builder
     * @return
     */
    protected CriteriaQuery<Object> complete(Predicate predicate, Sort sort,
            CriteriaQuery<Object> query, CriteriaBuilder builder) {

        return this.query.select(root).where(predicate)
                .orderBy(QueryUtils.toOrders(sort, root, builder));
    }


    /**
     * Creates a {@link Predicate} from the given {@link Part}.
     * 
     * @param part
     * @param root
     * @param iterator
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate toPredicate(Part part, Root<?> root,
            BindableParameterIterator iterator) {

        Expression<Object> path = root.get(part.getProperty());

        switch (part.getType()) {

        case BETWEEN:
            return builder.between(root.<Comparable> get(part.getProperty()),
                    nextAsComparable(iterator), nextAsComparable(iterator));
        case GREATER_THAN:
            return builder.greaterThan(getComparablePath(root, part),
                    nextAsComparable(iterator));
        case LESS_THAN:
            return builder.lessThan(getComparablePath(root, part),
                    nextAsComparable(iterator));
        case IS_NULL:
            return root.isNull();
        case IS_NOT_NULL:
            return root.isNotNull();
        case LIKE:
            return builder.like(root.<String> get(part.getProperty()), iterator
                    .next().toString());
        case NOT_LIKE:
            return builder.not(builder.like(root.<String> get(part
                    .getProperty()), iterator.next().toString()));
        case SIMPLE_PROPERTY:
            return builder.equal(path, iterator.next());
        case NEGATING_SIMPLE_PROPERTY:
            return builder.notEqual(path, iterator.next());
        default:
            throw new IllegalArgumentException("Unsupported keyword + "
                    + part.getType());
        }
    }


    /**
     * Returns a path to a {@link Comparable}.
     * 
     * @param root
     * @param part
     * @return
     */
    @SuppressWarnings("rawtypes")
    private Expression<? extends Comparable> getComparablePath(Root<?> root,
            Part part) {

        return root.get(part.getProperty());
    }


    /**
     * Returns the next parameter from the given
     * {@link BindableParameterIterator} and expects it to be a
     * {@link Comparable}.
     * 
     * @param iterator
     * @return
     */
    @SuppressWarnings("rawtypes")
    private Comparable nextAsComparable(BindableParameterIterator iterator) {

        Object next = iterator.next();
        Assert.isInstanceOf(Comparable.class, next,
                "Parameter has to implement Comparable to be bound correctly!");

        return (Comparable<?>) next;
    }
}
