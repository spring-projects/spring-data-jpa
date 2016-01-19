/*
 * Copyright 2008-2016 the original author or authors.
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

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * JPA specific extension of {@link org.springframework.data.repository.Repository}.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@NoRepositoryBean
public interface JpaRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findAll()
	 */
	List<T> findAll();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.PagingAndSortingRepository#findAll(org.springframework.data.domain.Sort)
	 */
	List<T> findAll(Sort sort);

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
	 * @param entity
	 * @return the saved entity
	 */
	<S extends T> S saveAndFlush(S entity);

	/**
	 * Deletes the given entities in a batch which means it will create a single {@link Query}. Assume that we will clear
	 * the {@link javax.persistence.EntityManager} after the call.
	 * 
	 * @param entities
	 */
	void deleteInBatch(Iterable<T> entities);

	/**
	 * Deletes all entities in a batch call.
	 */
	void deleteAllInBatch();

	/**
	 * Returns a reference to the entity with the given identifier.
	 * 
	 * @param id must not be {@literal null}.
	 * @return a reference to the entity with the given identifier.
	 * @see EntityManager#getReference(Class, Object)
	 */
	T getOne(ID id);

	/**
	 * Returns all instances of the type specified by the given {@link Example}.
	 * 
	 * @param example must not be {@literal null}.
	 * @return
	 * @since 1.10
	 */
	List<T> findAllByExample(Example<T> example);

	/**
	 * Returns all instances of the type specified by the given {@link Example}.
	 * 
	 * @param example must not be {@literal null}.
	 * @param sort can be {@literal null}.
	 * @return all entities sorted by the given options
	 * @since 1.10
	 */
	List<T> findAllByExample(Example<T> example, Sort sort);

	/**
	 * Returns a {@link Page} of entities meeting the paging restriction specified by the given {@link Example} limited to
	 * criteria provided in the {@code Pageable} object.
	 * 
	 * @param example must not be {@literal null}.
	 * @param pageable can be {@literal null}.
	 * @return a {@link Page} of entities
	 * @since 1.10
	 */
	Page<T> findAllByExample(Example<T> example, Pageable pageable);
}
