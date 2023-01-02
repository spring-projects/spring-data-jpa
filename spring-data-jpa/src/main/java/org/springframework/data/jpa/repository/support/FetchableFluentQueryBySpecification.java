/*
 * Copyright 2021-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.support.PageableUtils;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.Assert;

/**
 * Immutable implementation of {@link FetchableFluentQuery} based on a {@link Specification}. All methods that return a
 * {@link FetchableFluentQuery} will return a new instance, not the original.
 *
 * @param <S> Domain type
 * @param <R> Result type
 * @author Greg Turnquist
 * @since 3.0
 */
class FetchableFluentQueryBySpecification<S, R> extends FluentQuerySupport<S, R>
		implements FluentQuery.FetchableFluentQuery<R> {

	private final Specification<S> spec;
	private final Function<Sort, TypedQuery<S>> finder;
	private final Function<Specification<S>, Long> countOperation;
	private final Function<Specification<S>, Boolean> existsOperation;
	private final EntityManager entityManager;

	public FetchableFluentQueryBySpecification(Specification<S> spec, Class<S> entityType, Sort sort,
			Collection<String> properties, Function<Sort, TypedQuery<S>> finder,
			Function<Specification<S>, Long> countOperation, Function<Specification<S>, Boolean> existsOperation,
			EntityManager entityManager) {
		this(spec, entityType, (Class<R>) entityType, Sort.unsorted(), Collections.emptySet(), finder, countOperation,
				existsOperation, entityManager);
	}

	private FetchableFluentQueryBySpecification(Specification<S> spec, Class<S> entityType, Class<R> resultType,
			Sort sort, Collection<String> properties, Function<Sort, TypedQuery<S>> finder,
			Function<Specification<S>, Long> countOperation, Function<Specification<S>, Boolean> existsOperation,
			EntityManager entityManager) {

		super(resultType, sort, properties, entityType);
		this.spec = spec;
		this.finder = finder;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
		this.entityManager = entityManager;
	}

	@Override
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		Assert.notNull(sort, "Sort must not be null");

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, this.sort.and(sort), properties,
				finder, countOperation, existsOperation, entityManager);
	}

	@Override
	public <NR> FetchableFluentQuery<NR> as(Class<NR> resultType) {

		Assert.notNull(resultType, "Projection target type must not be null");
		if (!resultType.isInterface()) {
			throw new UnsupportedOperationException("Class-based DTOs are not yet supported.");
		}

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, properties, finder,
				countOperation, existsOperation, entityManager);
	}

	@Override
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, properties, finder,
				countOperation, existsOperation, entityManager);
	}

	@Override
	public R oneValue() {

		List<?> results = createSortedAndProjectedQuery() //
				.setMaxResults(2) // Never need more than 2 values
				.getResultList();

		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1);
		}

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public R firstValue() {

		List<?> results = createSortedAndProjectedQuery() //
				.setMaxResults(1) // Never need more than 1 value
				.getResultList();

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public List<R> all() {
		return convert(createSortedAndProjectedQuery().getResultList());
	}

	@Override
	public Page<R> page(Pageable pageable) {
		return pageable.isUnpaged() ? new PageImpl<>(all()) : readPage(pageable);
	}

	@Override
	public Stream<R> stream() {

		return createSortedAndProjectedQuery() //
				.getResultStream() //
				.map(getConversionFunction());
	}

	@Override
	public long count() {
		return countOperation.apply(spec);
	}

	@Override
	public boolean exists() {
		return existsOperation.apply(spec);
	}

	private TypedQuery<S> createSortedAndProjectedQuery() {

		TypedQuery<S> query = finder.apply(sort);

		if (!properties.isEmpty()) {
			query.setHint(EntityGraphFactory.HINT, EntityGraphFactory.create(entityManager, entityType, properties));
		}

		return query;
	}

	private Page<R> readPage(Pageable pageable) {

		TypedQuery<S> pagedQuery = createSortedAndProjectedQuery();

		if (pageable.isPaged()) {
			pagedQuery.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
			pagedQuery.setMaxResults(pageable.getPageSize());
		}

		List<R> paginatedResults = convert(pagedQuery.getResultList());

		return PageableExecutionUtils.getPage(paginatedResults, pageable, () -> countOperation.apply(spec));
	}

	private List<R> convert(List<S> resultList) {

		Function<Object, R> conversionFunction = getConversionFunction();
		List<R> mapped = new ArrayList<>(resultList.size());

		for (S s : resultList) {
			mapped.add(conversionFunction.apply(s));
		}
		return mapped;
	}

	private Function<Object, R> getConversionFunction() {
		return getConversionFunction(entityType, resultType);
	}
}
