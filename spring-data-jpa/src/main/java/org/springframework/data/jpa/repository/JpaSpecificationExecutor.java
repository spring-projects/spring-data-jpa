/*
 * Copyright 2008-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.FluentQuery;

/**
 * Interface to allow execution of {@link Specification}s based on the JPA criteria API.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Diego Krupitza
 * @author Mark Paluch
 */
public interface JpaSpecificationExecutor<T> {

	/**
	 * Returns a single entity matching the given {@link Specification} or {@link Optional#empty()} if none found.
	 *
	 * @param spec must not be {@literal null}.
	 * @return never {@literal null}.
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one entity found.
	 */
	Optional<T> findOne(Specification<T> spec);

	/**
	 * Returns all entities matching the given {@link Specification}.
	 *
	 * @param spec must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	List<T> findAll(Specification<T> spec);

	/**
	 * Returns a {@link Page} of entities matching the given {@link Specification}.
	 *
	 * @param spec must not be {@literal null}.
	 * @param pageable must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	Page<T> findAll(Specification<T> spec, Pageable pageable);

	/**
	 * Returns all entities matching the given {@link Specification} and {@link Sort}.
	 *
	 * @param spec must not be {@literal null}.
	 * @param sort must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	List<T> findAll(Specification<T> spec, Sort sort);

	/**
	 * Returns the number of instances that the given {@link Specification} will return.
	 *
	 * @param spec the {@link Specification} to count instances for, must not be {@literal null}.
	 * @return the number of instances.
	 */
	long count(Specification<T> spec);

	/**
	 * Checks whether the data store contains elements that match the given {@link Specification}.
	 *
	 * @param spec the {@link Specification} to use for the existence check, ust not be {@literal null}.
	 * @return {@code true} if the data store contains elements that match the given {@link Specification} otherwise
	 *         {@code false}.
	 */
	boolean exists(Specification<T> spec);

	/**
	 * Deletes by the {@link Specification} and returns the number of rows deleted.
	 * <p>
	 * This method uses {@link jakarta.persistence.criteria.CriteriaDelete Criteria API bulk delete} that maps directly to
	 * database delete operations. The persistence context is not synchronized with the result of the bulk delete.
	 * <p>
	 * Please note that {@link jakarta.persistence.criteria.CriteriaQuery} in,
	 * {@link Specification#toPredicate(Root, CriteriaQuery, CriteriaBuilder)} will be {@literal null} because
	 * {@link jakarta.persistence.criteria.CriteriaBuilder#createCriteriaDelete(Class)} does not implement
	 * {@code CriteriaQuery}.
	 *
	 * @param spec the {@link Specification} to use for the existence check, must not be {@literal null}.
	 * @return the number of entities deleted.
	 * @since 3.0
	 */
	long delete(Specification<T> spec);

	/**
	 * Returns entities matching the given {@link Specification} applying the {@code queryFunction} that defines the query
	 * and its result type.
	 *
	 * @param spec must not be null.
	 * @param queryFunction the query function defining projection, sorting, and the result type
	 * @return all entities matching the given Example.
	 * @since 3.0
	 */
	<S extends T, R> R findBy(Specification<T> spec, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction);

}
