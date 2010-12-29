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
package org.springframework.data.jpa.repository.support;

import static org.springframework.data.jpa.repository.utils.JpaClassUtils.*;
import static org.springframework.util.StringUtils.*;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.QueryDslPredicateExecutor;
import org.springframework.util.ClassUtils;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.PathBuilder;


/**
 * QueryDsl specific extension of {@link SimpleJpaRepository} which adds
 * implementation for {@link QueryDslPredicateExecutor}.
 * 
 * @author Oliver Gierke
 */
public class QueryDslJpaRepository<T, ID extends Serializable> extends
        SimpleJpaRepository<T, ID> implements QueryDslPredicateExecutor<T> {

    private final EntityManager em;
    private final EntityPath<T> path;


    /**
     * Creates a new {@link QueryDslJpaRepository}.
     * 
     * @param domainClass
     * @param entityManager
     */
    public QueryDslJpaRepository(Class<T> domainClass,
            EntityManager entityManager) {

        super(domainClass, entityManager);
        this.em = entityManager;
        this.path = createPath(domainClass);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#findOne(com.mysema.query.types.Predicate)
     */
    public T findOne(Predicate predicate) {

        return createQuery(predicate).uniqueResult(path);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#findAll(com.mysema.query.types.Predicate)
     */
    public List<T> findAll(Predicate predicate) {

        return createQuery(predicate).list(path);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#findAll(com.mysema.query.types.Predicate,
     * com.mysema.query.types.OrderSpecifier<?>[])
     */
    public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

        return createQuery(predicate).orderBy(orders).list(path);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#findAll(com.mysema.query.types.Predicate,
     * org.springframework.data.domain.Pageable)
     */
    public Page<T> findAll(Predicate predicate, Pageable pageable) {

        JPQLQuery countQuery = createQuery(predicate);
        JPQLQuery query = applyPagination(createQuery(predicate), pageable);

        return new PageImpl<T>(query.list(path), pageable, countQuery.count());
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.querydsl.
     * QueryDslSpecificationExecutor#count(com.mysema.query.types.Predicate)
     */
    public Long count(Predicate spec) {

        return createQuery(spec).count();
    }


    /**
     * Creates an {@link EntityPath} instance for the given domain class. Tries
     * to lookup a class matching the naming convention (prepend Q to the simple
     * name of the class, same package) and instantiates it using the
     * constructor taking a {@link String} and hands the entity name to it.
     * 
     * @param domainClass
     * @return
     */
    @SuppressWarnings("unchecked")
    private EntityPath<T> createPath(Class<T> domainClass) {

        String pathClassName =
                String.format("%s.Q%s", domainClass.getPackage().getName(),
                        domainClass.getSimpleName());

        try {

            Class<?> pathClass =
                    ClassUtils.forName(pathClassName,
                            QueryDslJpaRepository.class.getClassLoader());
            return (EntityPath<T>) BeanUtils.instantiateClass(
                    pathClass.getConstructor(String.class),
                    uncapitalize(getEntityName(domainClass)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Creates a new {@link JPQLQuery} for the given {@link Predicate}.
     * 
     * @param predicate
     * @return
     */
    private JPQLQuery createQuery(Predicate... predicate) {

        return new JPAQuery(em).from(path).where(predicate);
    }


    /**
     * Applies the given {@link Pageable} to the given {@link JPQLQuery}.
     * 
     * @param query
     * @param pageable
     * @return
     */
    private JPQLQuery applyPagination(JPQLQuery query, Pageable pageable) {

        if (pageable == null) {
            return query;
        }

        query.offset(pageable.getFirstItem());
        query.limit(pageable.getPageSize());

        return applySorting(query, pageable.getSort());
    }


    /**
     * Applies sorting to the given {@link JPQLQuery}.
     * 
     * @param query
     * @param sort
     * @return
     */
    private JPQLQuery applySorting(JPQLQuery query, Sort sort) {

        if (sort == null) {
            return query;
        }

        for (Order order : sort) {
            query.orderBy(toOrder(order));
        }

        return query;
    }


    /**
     * Transforms a plain {@link Order} into a QueryDsl specific
     * {@link OrderSpecifier}.
     * 
     * @param order
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private OrderSpecifier<?> toOrder(Order order) {

        PathBuilder<T> path =
                new PathBuilder<T>(this.path.getType(), this.path.getMetadata());
        PathBuilder<Object> builder = path.get(order.getProperty());

        return new OrderSpecifier(
                order.isAscending() ? com.mysema.query.types.Order.ASC
                        : com.mysema.query.types.Order.DESC, builder);
    }
}
