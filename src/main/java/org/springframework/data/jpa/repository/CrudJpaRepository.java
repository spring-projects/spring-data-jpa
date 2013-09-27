/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * JPA specific extension of {@link org.springframework.data.repository.Repository} that supports all CRUD operations
 * from {@link CrudRepository}. You will probably want to use {@link JpaRepository}, which also supports
 * pagination with {@link org.springframework.data.repository.PagingAndSortingRepository}. This simply provides a
 * cleaner interface for when you do not want to expose the pagination methods.
 * <p>
 * All current implementations of this interface also implement {@link JpaRepository}.
 *
 * @author Oliver Gierke
 * @author Nick Williams
 */
@NoRepositoryBean
public interface CrudJpaRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAll()
     */
    List<T> findAll();

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAll(java.lang.Iterable)
     */
    List<T> findAll(Iterable<ID> ids);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#save(java.lang.Iterable)
     */
    <S extends T> List<S> save(Iterable<S> entities);

    /**
     * Flushes all pending changes to the database.
     */
    void flush();

    /**
     * Saves an entity and flushes changes instantly.
     *
     * @param entity The entity to save
     * @return the saved entity.
     */
    T saveAndFlush(T entity);

    /**
     * Deletes the given entities in a batch which means it will create a single {@link Query}. Assume that we will clear
     * the {@link javax.persistence.EntityManager} after the call.
     *
     * @param entities The entities to delete
     */
    void deleteInBatch(Iterable<T> entities);

    /**
     * Deletes all entities in a batch call.
     */
    void deleteAllInBatch();
}
