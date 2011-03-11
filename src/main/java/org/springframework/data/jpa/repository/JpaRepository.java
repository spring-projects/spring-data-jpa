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
package org.springframework.data.jpa.repository;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;


/**
 * JPA specific extension of {@link Repository}. Redeclares methods from
 * {@link Repository} and {@link PagingAndSortingRepository} to apply
 * transaction configuration to those. We need to do this to allow specific
 * extensions of the interface to override transaction configuration. If we'd
 * annotated the implementation this configuration would always enjoy precedence
 * over the configuration applied on an interface.
 * 
 * @author Oliver Gierke
 */
@Transactional(readOnly = true)
public interface JpaRepository<T, ID extends Serializable> extends
        PagingAndSortingRepository<T, ID> {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#save(java.lang.Object)
     */
    @Transactional
    T save(T entity);


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#save(java.lang.Iterable)
     */
    @Transactional
    List<T> save(Iterable<? extends T> entities);


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#findById(java.io.Serializable
     * )
     */
    T findOne(ID id);


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#exists(java.io.Serializable
     * )
     */
    boolean exists(ID id);


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#findAll()
     */
    List<T> findAll();


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#count()
     */
    Long count();


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#delete(java.lang.Object)
     */
    @Transactional
    void delete(T entity);


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.Repository#delete(java.lang.Iterable)
     */
    @Transactional
    void delete(Iterable<? extends T> entities);


    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.data.repository.Repository#deleteAll()
     */
    @Transactional
    void deleteAll();


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.PagingAndSortingRepository#findAll
     * (org.springframework.data.domain.Sort)
     */
    List<T> findAll(Sort sort);


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.PagingAndSortingRepository#findAll
     * (org.springframework.data.domain.Pageable)
     */
    Page<T> findAll(Pageable pageable);


    /**
     * Returns a single entity matching the given {@link Specification}.
     * 
     * @param spec
     * @return
     */
    T findOne(Specification<T> spec);


    /**
     * Returns all entities matching the given {@link Specification}.
     * 
     * @param spec
     * @return
     */
    List<T> findAll(Specification<T> spec);


    /**
     * Returns a {@link Page} of entities matching the given
     * {@link Specification}.
     * 
     * @param spec
     * @param pageable
     * @return
     */
    Page<T> findAll(Specification<T> spec, Pageable pageable);


    /**
     * Flushes all pending changes to the database.
     */
    @Transactional
    void flush();


    /**
     * Saves an entity and flushes changes instantly.
     * 
     * @param entity
     * @return the saved entity
     */
    @Transactional
    T saveAndFlush(T entity);


    /**
     * Deletes the given entities in a batch which means it will create a single
     * {@link Query}. Assume that we will clear the {@link EntityManager} after
     * the call.
     * 
     * @param entities
     */
    @Transactional
    void deleteInBatch(Iterable<T> entities);
}
