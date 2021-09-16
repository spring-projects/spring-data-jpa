/*
 * Copyright 2021 the original author or authors.
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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;

/**
 * Immutable implementation of {@link FetchableFluentQuery} based on Query by {@link Example}. All methods that return a
 * {@link FetchableFluentQuery} will return a new instance, not the original.
 *
 * @param <S> Domain type
 * @param <R> Result type
 * @author Greg Turnquist
 * @since 2.6
 */
class FetchableFluentQueryByExample<S, R> extends FluentQuerySupport<R> implements FetchableFluentQuery<R> {

	private final Example<S> example;
	private final Function<Sort, TypedQuery<S>> finder;
	private final Function<Example<S>, Long> countOperation;
	private final Function<Example<S>, Boolean> existsOperation;
	private final EntityManager entityManager;
	private final EscapeCharacter escapeCharacter;

	public FetchableFluentQueryByExample(Example<S> example, Function<Sort, TypedQuery<S>> finder,
			Function<Example<S>, Long> countOperation, Function<Example<S>, Boolean> existsOperation,
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> context,
			EntityManager entityManager, EscapeCharacter escapeCharacter) {
		this(example, (Class<R>) example.getProbeType(), Sort.unsorted(), null, finder, countOperation, existsOperation,
				context, entityManager, escapeCharacter);
	}

	private FetchableFluentQueryByExample(Example<S> example, Class<R> returnType, Sort sort,
			@Nullable Collection<String> properties, Function<Sort, TypedQuery<S>> finder,
			Function<Example<S>, Long> countOperation, Function<Example<S>, Boolean> existsOperation,
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> context,
			EntityManager entityManager, EscapeCharacter escapeCharacter) {

		super(returnType, sort, properties, context);
		this.example = example;
		this.finder = finder;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
		this.entityManager = entityManager;
		this.escapeCharacter = escapeCharacter;
	}

	@Override
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		return new FetchableFluentQueryByExample<S, R>(this.example, this.resultType, this.sort.and(sort), this.properties,
				this.finder, this.countOperation, this.existsOperation, this.context, this.entityManager, this.escapeCharacter);
	}

	@Override
	public <NR> FetchableFluentQuery<NR> as(Class<NR> resultType) {

		if (!resultType.isInterface()) {
			throw new UnsupportedOperationException("Class-based DTOs are not yet supported.");
		}

		return new FetchableFluentQueryByExample<S, NR>(this.example, resultType, this.sort, this.properties, this.finder,
				this.countOperation, this.existsOperation, this.context, this.entityManager, this.escapeCharacter);
	}

	@Override
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryByExample<>(this.example, this.resultType, this.sort, mergeProperties(properties),
				this.finder, this.countOperation, this.existsOperation, this.context, this.entityManager, this.escapeCharacter);
	}

	@Override
	public R oneValue() {

		TypedQuery<S> limitedQuery = this.finder.apply(this.sort);
		limitedQuery.setMaxResults(2); // Never need more than 2 values

		List<R> results = limitedQuery //
				.getResultStream() //
				.map(getConversionFunction(this.example.getProbeType(), this.resultType)) //
				.collect(Collectors.toList());
		;

		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1);
		}

		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public R firstValue() {

		TypedQuery<S> limitedQuery = this.finder.apply(this.sort);
		limitedQuery.setMaxResults(1); // Never need more than 1 value

		List<R> results = limitedQuery //
				.getResultStream() //
				.map(getConversionFunction(this.example.getProbeType(), this.resultType)) //
				.collect(Collectors.toList());

		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public List<R> all() {
		return stream().collect(Collectors.toList());
	}

	@Override
	public Page<R> page(Pageable pageable) {
		return pageable.isUnpaged() ? new PageImpl<>(all()) : readPage(pageable);
	}

	@Override
	public Stream<R> stream() {

		return this.finder.apply(this.sort) //
				.getResultStream() //
				.map(getConversionFunction(this.example.getProbeType(), this.resultType));
	}

	@Override
	public long count() {
		return this.countOperation.apply(example);
	}

	@Override
	public boolean exists() {
		return this.existsOperation.apply(example);
	}

	private Page<R> readPage(Pageable pageable) {

		TypedQuery<S> pagedQuery = this.finder.apply(this.sort);

		if (pageable.isPaged()) {
			pagedQuery.setFirstResult((int) pageable.getOffset());
			pagedQuery.setMaxResults(pageable.getPageSize());
		}

		List<R> paginatedResults = pagedQuery.getResultStream() //
				.map(getConversionFunction(this.example.getProbeType(), this.resultType)) //
				.collect(Collectors.toList());

		return PageableExecutionUtils.getPage(paginatedResults, pageable, () -> this.countOperation.apply(this.example));
	}
}
