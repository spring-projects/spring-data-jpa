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
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.support.PageableUtils;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.Assert;

/**
 * Immutable implementation of {@link FetchableFluentQuery} based on Query by {@link Example}. All methods that return a
 * {@link FetchableFluentQuery} will return a new instance, not the original.
 *
 * @param <S> Domain type
 * @param <R> Result type
 * @author Greg Turnquist
 * @author Mark Paluch
 * @author Jens Schauder
 * @author J.R. Onyschak
 * @since 2.6
 */
class FetchableFluentQueryByExample<S, R> extends FluentQuerySupport<S, R> implements FetchableFluentQuery<R> {

	private final Example<S> example;
	private final Function<Sort, TypedQuery<S>> finder;
	private final Function<Example<S>, Long> countOperation;
	private final Function<Example<S>, Boolean> existsOperation;
	private final EntityManager entityManager;
	private final EscapeCharacter escapeCharacter;

	public FetchableFluentQueryByExample(Example<S> example, Function<Sort, TypedQuery<S>> finder,
			Function<Example<S>, Long> countOperation, Function<Example<S>, Boolean> existsOperation,
			EntityManager entityManager, EscapeCharacter escapeCharacter) {
		this(example, example.getProbeType(), (Class<R>) example.getProbeType(), Sort.unsorted(), Collections.emptySet(),
				finder, countOperation, existsOperation, entityManager, escapeCharacter);
	}

	private FetchableFluentQueryByExample(Example<S> example, Class<S> entityType, Class<R> returnType, Sort sort,
			Collection<String> properties, Function<Sort, TypedQuery<S>> finder, Function<Example<S>, Long> countOperation,
			Function<Example<S>, Boolean> existsOperation, EntityManager entityManager, EscapeCharacter escapeCharacter) {

		super(returnType, sort, properties, entityType);
		this.example = example;
		this.finder = finder;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
		this.entityManager = entityManager;
		this.escapeCharacter = escapeCharacter;
	}

	@Override
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		Assert.notNull(sort, "Sort must not be null");

		return new FetchableFluentQueryByExample<>(example, entityType, resultType, this.sort.and(sort), properties, finder,
				countOperation, existsOperation, entityManager, escapeCharacter);
	}

	@Override
	public <NR> FetchableFluentQuery<NR> as(Class<NR> resultType) {

		Assert.notNull(resultType, "Projection target type must not be null");
		if (!resultType.isInterface()) {
			throw new UnsupportedOperationException("Class-based DTOs are not yet supported.");
		}

		return new FetchableFluentQueryByExample<>(example, entityType, resultType, sort, properties, finder,
				countOperation, existsOperation, entityManager, escapeCharacter);
	}

	@Override
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryByExample<>(example, entityType, resultType, sort, mergeProperties(properties),
				finder, countOperation, existsOperation, entityManager, escapeCharacter);
	}

	@Override
	public R oneValue() {

		TypedQuery<S> limitedQuery = createSortedAndProjectedQuery();
		limitedQuery.setMaxResults(2); // Never need more than 2 values

		List<S> results = limitedQuery.getResultList();

		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1);
		}

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public R firstValue() {

		TypedQuery<S> limitedQuery = createSortedAndProjectedQuery();
		limitedQuery.setMaxResults(1); // Never need more than 1 value

		List<S> results = limitedQuery.getResultList();

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public List<R> all() {

		List<S> resultList = createSortedAndProjectedQuery().getResultList();

		return convert(resultList);
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
		return countOperation.apply(example);
	}

	@Override
	public boolean exists() {
		return existsOperation.apply(example);
	}

	private Page<R> readPage(Pageable pageable) {

		TypedQuery<S> pagedQuery = createSortedAndProjectedQuery();

		if (pageable.isPaged()) {
			pagedQuery.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
			pagedQuery.setMaxResults(pageable.getPageSize());
		}

		List<R> paginatedResults = convert(pagedQuery.getResultList());

		return PageableExecutionUtils.getPage(paginatedResults, pageable, () -> countOperation.apply(example));
	}

	private TypedQuery<S> createSortedAndProjectedQuery() {

		TypedQuery<S> query = finder.apply(sort);

		if (!properties.isEmpty()) {
			query.setHint(EntityGraphFactory.HINT, EntityGraphFactory.create(entityManager, entityType, properties));
		}

		return query;
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
		return getConversionFunction(example.getProbeType(), resultType);
	}

}
