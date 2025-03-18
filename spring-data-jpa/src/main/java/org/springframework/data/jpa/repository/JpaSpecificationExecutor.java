/*
 * Copyright 2008-2025 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.DeleteSpecification;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.UpdateSpecification;
import org.springframework.data.repository.query.FluentQuery;

/**
 * Interface to allow execution of {@link Specification}s based on the JPA criteria API.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Diego Krupitza
 * @author Mark Paluch
 * @author Joshua Chen
 * @see Specification
 * @see org.springframework.data.jpa.domain.UpdateSpecification
 * @see DeleteSpecification
 * @see PredicateSpecification
 */
public interface JpaSpecificationExecutor<T> {

	/**
	 * Returns a single entity matching the given {@link PredicateSpecification} or {@link Optional#empty()} if none
	 * found.
	 *
	 * @param spec must not be {@literal null}.
	 * @return never {@literal null}.
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one entity found.
	 * @see Specification#unrestricted()
	 */
	default Optional<T> findOne(PredicateSpecification<T> spec) {
		return findOne(Specification.where(spec));
	}

	/**
	 * Returns a single entity matching the given {@link Specification} or {@link Optional#empty()} if none found.
	 *
	 * @param spec must not be {@literal null}.
	 * @return never {@literal null}.
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one entity found.
	 * @see Specification#unrestricted()
	 */
	Optional<T> findOne(Specification<T> spec);

	/**
	 * Returns all entities matching the given {@link PredicateSpecification}.
	 *
	 * @param spec must not be {@literal null}.
	 * @return never {@literal null}.
	 * @see Specification#unrestricted()
	 */
	default List<T> findAll(PredicateSpecification<T> spec) {
		return findAll(Specification.where(spec));
	}

	/**
	 * Returns all entities matching the given {@link Specification}.
	 *
	 * @param spec must not be {@literal null}.
	 * @return never {@literal null}.
	 * @see Specification#unrestricted()
	 */
	List<T> findAll(Specification<T> spec);

	/**
	 * Returns a {@link Page} of entities matching the given {@link Specification}.
	 *
	 * @param spec must not be {@literal null}.
	 * @param pageable must not be {@literal null}.
	 * @return never {@literal null}.
	 * @see Specification#unrestricted()
	 */
	Page<T> findAll(Specification<T> spec, Pageable pageable);

	/**
	 * Returns a {@link Page} of entities matching the given {@link Specification}.
	 * <p>
	 * Supports counting the total number of entities matching the {@link Specification}.
	 *
	 * @param spec can be {@literal null}, if no {@link Specification} is given all entities matching {@code <T>} will be
	 *          selected.
	 * @param countSpec can be {@literal null}，if no {@link Specification} is given all entities matching {@code <T>} will
	 *          be counted.
	 * @param pageable must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.5
	 */
	Page<T> findAll(@Nullable Specification<T> spec, @Nullable Specification<T> countSpec, Pageable pageable);

	/**
	 * Returns all entities matching the given {@link Specification} and {@link Sort}.
	 *
	 * @param spec must not be {@literal null}.
	 * @param sort must not be {@literal null}.
	 * @return never {@literal null}.
	 * @see Specification#unrestricted()
	 */
	List<T> findAll(Specification<T> spec, Sort sort);

	/**
	 * Returns the number of instances that the given {@link PredicateSpecification} will return.
	 *
	 * @param spec the {@link PredicateSpecification} to count instances for, must not be {@literal null}.
	 * @return the number of instances.
	 * @see Specification#unrestricted()
	 */
	default long count(PredicateSpecification<T> spec) {
		return count(Specification.where(spec));
	}

	/**
	 * Returns the number of instances that the given {@link Specification} will return.
	 *
	 * @param spec the {@link Specification} to count instances for, must not be {@literal null}.
	 * @return the number of instances.
	 * @see Specification#unrestricted()
	 */
	long count(Specification<T> spec);

	/**
	 * Checks whether the data store contains elements that match the given {@link PredicateSpecification}.
	 *
	 * @param spec the {@link PredicateSpecification} to use for the existence check, must not be {@literal null}.
	 * @return {@code true} if the data store contains elements that match the given {@link PredicateSpecification}
	 *         otherwise {@code false}.
	 * @see Specification#unrestricted()
	 */
	default boolean exists(PredicateSpecification<T> spec) {
		return exists(Specification.where(spec));
	}

	/**
	 * Checks whether the data store contains elements that match the given {@link Specification}.
	 *
	 * @param spec the {@link Specification} to use for the existence check, must not be {@literal null}.
	 * @return {@code true} if the data store contains elements that match the given {@link Specification} otherwise
	 *         {@code false}.
	 * @see Specification#unrestricted()
	 */
	boolean exists(Specification<T> spec);

	/**
	 * Updates entities by the {@link UpdateSpecification} and returns the number of rows updated.
	 * <p>
	 * This method uses {@link jakarta.persistence.criteria.CriteriaUpdate Criteria API bulk update} that maps directly to
	 * database update operations. The persistence context is not synchronized with the result of the bulk update.
	 *
	 * @param spec the {@link UpdateSpecification} to use for the update query must not be {@literal null}.
	 * @return the number of entities deleted.
	 * @since 4.0
	 */
	long update(UpdateSpecification<T> spec);

	/**
	 * Deletes by the {@link PredicateSpecification} and returns the number of rows deleted.
	 * <p>
	 * This method uses {@link jakarta.persistence.criteria.CriteriaDelete Criteria API bulk delete} that maps directly to
	 * database delete operations. The persistence context is not synchronized with the result of the bulk delete.
	 *
	 * @param spec the {@link PredicateSpecification} to use for the delete query, must not be {@literal null}.
	 * @return the number of entities deleted.
	 * @since 3.0
	 * @see PredicateSpecification#unrestricted()
	 */
	default long delete(PredicateSpecification<T> spec) {
		return delete(DeleteSpecification.where(spec));
	}

	/**
	 * Deletes by the {@link UpdateSpecification} and returns the number of rows deleted.
	 * <p>
	 * This method uses {@link jakarta.persistence.criteria.CriteriaDelete Criteria API bulk delete} that maps directly to
	 * database delete operations. The persistence context is not synchronized with the result of the bulk delete.
	 *
	 * @param spec the {@link UpdateSpecification} to use for the delete query must not be {@literal null}.
	 * @return the number of entities deleted.
	 * @since 3.0
	 * @see DeleteSpecification#unrestricted()
	 */
	long delete(DeleteSpecification<T> spec);

	/**
	 * Returns entities matching the given {@link Specification} applying the {@code queryFunction} that defines the query
	 * and its result type.
	 * <p>
	 * The query object used with {@code queryFunction} is only valid inside the {@code findBy(…)} method call. This
	 * requires the query function to return a query result and not the {@link FluentQuery} object itself to ensure the
	 * query is executed inside the {@code findBy(…)} method.
	 *
	 * @param spec must not be null.
	 * @param queryFunction the query function defining projection, sorting, and the result type
	 * @return all entities matching the given Example.
	 * @since 4.0
	 */
	default <S extends T, R> R findBy(PredicateSpecification<T> spec,
			Function<? super SpecificationFluentQuery<S>, R> queryFunction) {
		return findBy(Specification.where(spec), queryFunction);
	}

	/**
	 * Returns entities matching the given {@link Specification} applying the {@code queryFunction} that defines the query
	 * and its result type.
	 * <p>
	 * The query object used with {@code queryFunction} is only valid inside the {@code findBy(…)} method call. This
	 * requires the query function to return a query result and not the {@link FluentQuery} object itself to ensure the
	 * query is executed inside the {@code findBy(…)} method.
	 *
	 * @param spec must not be null.
	 * @param queryFunction the query function defining projection, sorting, and the result type
	 * @return all entities matching the given specification.
	 * @since 3.0
	 * @throws InvalidDataAccessApiUsageException if the query function returns the {@link FluentQuery} instance.
	 */
	<S extends T, R> R findBy(Specification<T> spec, Function<? super SpecificationFluentQuery<S>, R> queryFunction);

	/**
	 * Extension to {@link FetchableFluentQuery} allowing slice results and pagination with a custom count
	 * {@link Specification}.
	 *
	 * @param <T>
	 * @since 3.5
	 */
	interface SpecificationFluentQuery<T> extends FluentQuery.FetchableFluentQuery<T> {

		@Override
		SpecificationFluentQuery<T> sortBy(Sort sort);

		@Override
		SpecificationFluentQuery<T> limit(int limit);

		@Override
		<R> SpecificationFluentQuery<R> as(Class<R> resultType);

		@Override
		default SpecificationFluentQuery<T> project(String... properties) {
			return this.project(Arrays.asList(properties));
		}

		@Override
		SpecificationFluentQuery<T> project(Collection<String> properties);

		/**
		 * Get a page of matching elements for {@link Pageable} and provide a custom {@link Specification count
		 * specification}.
		 *
		 * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must not be
		 *          {@literal null}. The given {@link Pageable} will override any previously specified {@link Sort sort} if
		 *          the {@link Sort} object is not {@link Sort#isUnsorted()}. Any potentially specified {@link #limit(int)}
		 *          will be overridden by {@link Pageable#getPageSize()}.
		 * @param countSpec specification used to count results.
		 * @return
		 */
		default Page<T> page(Pageable pageable, PredicateSpecification<?> countSpec) {
			return page(pageable, Specification.where(countSpec));
		}

		/**
		 * Get a page of matching elements for {@link Pageable} and provide a custom {@link Specification count
		 * specification}.
		 *
		 * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must not be
		 *          {@literal null}. The given {@link Pageable} will override any previously specified {@link Sort sort}.
		 *          Any potentially specified {@link #limit(int)} will be overridden by {@link Pageable#getPageSize()}.
		 * @param countSpec specification used to count results.
		 * @return
		 */
		Page<T> page(Pageable pageable, Specification<?> countSpec);
	}

}
