/*
 * Copyright 2008-2011 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.QueryUtils.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;
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
public class SimpleJpaRepository<T, ID extends Serializable> implements
        JpaRepository<T, ID> {

    private final JpaEntityInformation<T, ID> entityInformation;
    private final EntityManager em;
    private final PersistenceProvider provider;


    /**
     * Creates a new {@link SimpleJpaRepository} to manage objects of the given
     * domain type.
     * 
     * @param entityMetadata
     * @param entityManager
     */
    public SimpleJpaRepository(JpaEntityInformation<T, ID> entityMetadata,
            EntityManager entityManager) {

        Assert.notNull(entityMetadata);
        Assert.notNull(entityManager);
        this.entityInformation = entityMetadata;
        this.em = entityManager;
        this.provider = PersistenceProvider.fromEntityManager(entityManager);
    }


    private Class<T> getDomainClass() {

        return entityInformation.getJavaType();
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


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#delete(java.lang.Object)
     */
    public void delete(T entity) {

        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#delete(java.lang.Iterable)
     */
    public void delete(Iterable<? extends T> entities) {

        if (entities == null) {
            return;
        }

        for (T entity : entities) {
            delete(entity);
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.JpaRepository#deleteInBatch(java
     * .lang.Iterable)
     */
    public void deleteInBatch(Iterable<T> entities) {

        if (null == entities || !entities.iterator().hasNext()) {
            return;
        }

        applyAndBind(getQueryString(DELETE_ALL_QUERY_STRING, getDomainClass()),
                entities, em).executeUpdate();
        em.clear();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#deleteAll()
     */
    public void deleteAll() {

        em.createQuery(getDeleteAllQueryString()).executeUpdate();
        em.clear();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#readById(java.io.Serializable
     * )
     */
    public T findById(ID id) {

        Assert.notNull(id, "The given id must not be null!");
        return em.find(getDomainClass(), id);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#exists(java.io.Serializable
     * )
     */
    public boolean exists(ID id) {

        Assert.notNull(id, "The given id must not be null!");
        return null != findById(id);
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#readAll()
     */
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
    public List<T> findAll(Sort sort) {

        return getQuery(null, sort).getResultList();
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#readAll(org.
     * springframework.data.domain.Pageable)
     */
    public Page<T> findAll(Pageable pageable) {

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

        try {
            return getQuery(spec, (Sort) null).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.jpa.repository.JpaRepository#readAll(org.
     * springframework.data.jpa.domain.Specification)
     */
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
    public T save(T entity) {

        if (entityInformation.isNew(entity)) {
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
    public T saveAndFlush(T entity) {

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
     * Reads the given {@link TypedQuery} into a {@link Page} applying the given
     * {@link Pageable} and {@link Specification}.
     * 
     * @param query
     * @param spec
     * @param pageable
     * @return
     */
    private Page<T> readPage(TypedQuery<T> query, Pageable pageable,
            Specification<T> spec) {

        query.setFirstResult(pageable.getOffset());
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
}
