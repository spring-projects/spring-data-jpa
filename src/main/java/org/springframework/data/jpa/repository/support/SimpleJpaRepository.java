/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.data.jpa.repository.support;

import static org.springframework.data.jpa.repository.query.QueryUtils.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;


/**
 * Default implementation of the {@link Repository} interface. This will offer
 * you a more sophisticated interface than the plain {@link EntityManager} .
 * 
 * @author Oliver Gierke
 * @author Eberhard Wolff
 * @param <T> the type of the entity to handle
 * @param <ID> the type of the entity's identifier
 */
@org.springframework.stereotype.Repository
@Transactional
public class SimpleJpaRepository<T, ID extends Serializable> extends
        JpaRepositorySupport<T, ID> {

    private final EntityManager em;
    private final PersistenceProvider provider;


    public SimpleJpaRepository(Class<T> domainClass, EntityManager entityManager) {

        super(domainClass);

        Assert.notNull(entityManager);
        this.em = entityManager;
        this.provider = PersistenceProvider.fromEntityManager(entityManager);
    }


    private String getDeleteAllQueryString() {

        return getQueryString(DELETE_ALL_QUERY_STRING, getDomainClass());
    }


    private String getCountQueryString() {

        String countQuery =
                String.format(COUNT_QUERY_STRING,
                        provider.getCountQueryPlaceholder(), "%s");

        return getQueryString(countQuery, getDomainClass());
    }


    /**
     * Factory method to create {@link SimpleJpaRepository} instances.
     * 
     * @param <T> the type of the entity to handle
     * @param <PK> the type of the entity's identifier
     * @param entityManager the {@link EntityManager} backing the repository
     * @param domainClass the domain class to handle
     * @return
     */
    public static <T, PK extends Serializable> Repository<T, PK> create(
            final EntityManager entityManager, final Class<T> domainClass) {

        return new SimpleJpaRepository<T, PK>(domainClass, entityManager);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#delete(java.lang.Object)
     */
    public void delete(final T entity) {

        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#delete(java.lang.Iterable)
     */
    public void delete(final Iterable<? extends T> entities) {

        if (null == entities || !entities.iterator().hasNext()) {
            return;
        }

        applyAndBind(getQueryString(DELETE_ALL_QUERY_STRING, getDomainClass()),
                entities, em).executeUpdate();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#deleteAll()
     */
    public void deleteAll() {

        em.createQuery(getDeleteAllQueryString()).executeUpdate();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#readById(java.io.Serializable
     * )
     */
    @Transactional(readOnly = true)
    public T findById(final ID primaryKey) {

        Assert.notNull(primaryKey, "The given primaryKey must not be null!");

        return em.find(getDomainClass(), primaryKey);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#exists(java.io.Serializable
     * )
     */
    @Transactional(readOnly = true)
    public boolean exists(final ID primaryKey) {

        Assert.notNull(primaryKey, "The given primary key must not be null!");

        return null != findById(primaryKey);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#readAll()
     */
    @Transactional(readOnly = true)
    public List<T> findAll() {

        return getQuery(null, (Sort) null).getResultList();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#readAll(org.springframework
     * .data.domain.Sort)
     */
    @Transactional(readOnly = true)
    public List<T> findAll(final Sort sort) {

        return getQuery(null, sort).getResultList();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#readAll(org.
     * springframework.data.domain.Pageable)
     */
    @Transactional(readOnly = true)
    public Page<T> findAll(final Pageable pageable) {

        if (null == pageable) {
            return new PageImpl<T>(findAll());
        }

        return findAll(null, pageable);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.JpaRepository#findOneBy(org.
     * springframework.data.jpa.domain.Specification)
     */
    public T findOne(Specification<T> spec) {

        return getQuery(spec, (Sort) null).getSingleResult();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.JpaRepository#readAll(org.
     * springframework.data.jpa.domain.Specification)
     */
    @Transactional(readOnly = true)
    public List<T> findAll(Specification<T> spec) {

        return getQuery(spec, (Sort) null).getResultList();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.JpaRepository#readAll(org.
     * springframework.data.jpa.domain.Specification,
     * org.springframework.data.domain.Pageable)
     */
    @Transactional(readOnly = true)
    public Page<T> findAll(Specification<T> spec, Pageable pageable) {

        TypedQuery<T> query = getQuery(spec, pageable);

        return pageable == null ? new PageImpl<T>(query.getResultList())
                : readPage(query, pageable, spec);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#count()
     */
    @Transactional(readOnly = true)
    public Long count() {

        return em.createQuery(getCountQueryString(), Long.class)
                .getSingleResult();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#save(java.lang.Object)
     */
    public T save(final T entity) {

        if (getIsNewStrategy().isNew(entity)) {
            em.persist(entity);
            return entity;
        } else {
            return em.merge(entity);
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.JpaRepository#saveAndFlush(java
     * .lang.Object)
     */
    public T saveAndFlush(final T entity) {

        T result = save(entity);
        flush();

        return result;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#save(java.lang.Iterable)
     */
    public List<T> save(Iterable<? extends T> entities) {

        List<T> result = new ArrayList<T>();

        if (entities == null) {
            return result;
        }

        for (T entity : entities) {
            result.add(save(entity));
        }

        return result;
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.JpaRepository#flush()
     */
    public void flush() {

        em.flush();
    }


    /**
     * Reads a page of entities for the given JPQL query.
     * 
     * @param pageable
     * @param query
     * @return a page of entities for the given JPQL query
     */
    protected Page<T> readPage(final Pageable pageable, final String query) {

        String queryString = QueryUtils.applySorting(query, pageable.getSort());
        TypedQuery<T> jpaQuery = em.createQuery(queryString, getDomainClass());

        return readPage(jpaQuery, pageable, null);
    }


    /**
     * @param query
     * @param spec
     * @param pageable
     * @return
     */
    private Page<T> readPage(final TypedQuery<T> query,
            final Pageable pageable, final Specification<T> spec) {

        query.setFirstResult(pageable.getFirstItem());
        query.setMaxResults(pageable.getPageSize());

        Long total = getCountQuery(spec).getSingleResult();

        return new PageImpl<T>(query.getResultList(), pageable, total);
    }


    /**
     * Creates a new {@link TypedQuery} from the given {@link Specification}.
     * 
     * @param spec can be {@literal null}
     * @param pageable can be {@literal null}
     * @return
     */
    private TypedQuery<T> getQuery(Specification<T> spec, Pageable pageable) {

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(getDomainClass());

        Root<T> root = applySpecificationToCriteria(spec, query);
        query.select(root);

        if (pageable != null) {
            query.orderBy(toOrders(pageable.getSort(), root, builder));
        }

        return em.createQuery(query);
    }


    /**
     * Creates a {@link TypedQuery} for the given {@link Specification} and
     * {@link Sort}.
     * 
     * @param spec
     * @param sort
     * @return
     */
    private TypedQuery<T> getQuery(Specification<T> spec, Sort sort) {

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(getDomainClass());

        Root<T> root = applySpecificationToCriteria(spec, query);
        query.select(root);

        if (sort != null) {
            query.orderBy(toOrders(sort, root, builder));
        }

        return em.createQuery(query);
    }


    /**
     * Creates a new count query for the given {@link Specification}.
     * 
     * @param spec can be {@literal null}.
     * @return
     */
    private TypedQuery<Long> getCountQuery(Specification<T> spec) {

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);

        Root<T> root = applySpecificationToCriteria(spec, query);
        query.select(builder.count(root)).distinct(true);

        return em.createQuery(query);
    }


    /**
     * Applies the given {@link Specification} to the given
     * {@link CriteriaQuery}.
     * 
     * @param spec can be {@literal null}
     * @param query
     * @return
     */
    private <S> Root<T> applySpecificationToCriteria(Specification<T> spec,
            CriteriaQuery<S> query) {

        Assert.notNull(query);
        Root<T> root = query.from(getDomainClass());

        if (spec == null) {
            return root;
        }

        CriteriaBuilder builder = em.getCriteriaBuilder();
        Predicate predicate = spec.toPredicate(root, query, builder);

        if (predicate != null) {
            query.where(predicate);
        }

        return root;
    }


    private List<Order> toOrders(Sort sort, Root<T> root, CriteriaBuilder cb) {

        List<Order> orders = new ArrayList<Order>();

        if (sort == null) {
            return orders;
        }

        for (org.springframework.data.domain.Sort.Order order : sort) {
            orders.add(toJpaOrder(order, root, cb));
        }

        return orders;
    }


    private Order toJpaOrder(org.springframework.data.domain.Sort.Order order,
            Root<T> root, CriteriaBuilder cb) {

        Expression<?> expression = root.get(order.getProperty());
        return order.isAscending() ? cb.asc(expression) : cb.desc(expression);
    }
}
