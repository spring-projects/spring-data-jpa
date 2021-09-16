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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPQLQuery;

/**
 * Immutable implementation of {@link FetchableFluentQuery} based on a Querydsl {@link Predicate}. All methods that
 * return a {@link FetchableFluentQuery} will return a new instance, not the original.
 *
 * @param <S> Domain type
 * @param <R> Result type
 * @author Greg Turnquist
 * @since 2.6
 */
class FetchableFluentQueryByPredicate<S, R> extends FluentQuerySupport<R> implements FetchableFluentQuery<R> {

	private final Predicate predicate;
	private final Function<Sort, JPQLQuery<S>> finder;
	private final BiFunction<Sort, Pageable, JPQLQuery<S>> pagedFinder;
	private final Function<Predicate, Long> countOperation;
	private final Function<Predicate, Boolean> existsOperation;
	private final Class<S> entityType;

	public FetchableFluentQueryByPredicate(Predicate predicate, Class<R> resultType, Function<Sort, JPQLQuery<S>> finder,
			BiFunction<Sort, Pageable, JPQLQuery<S>> pagedFinder, Function<Predicate, Long> countOperation,
			Function<Predicate, Boolean> existsOperation, Class<S> entityType,
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> context) {
		this(predicate, resultType, Sort.unsorted(), null, finder, pagedFinder, countOperation, existsOperation, entityType,
				context);
	}

	private FetchableFluentQueryByPredicate(Predicate predicate, Class<R> resultType, Sort sort,
			@Nullable Collection<String> properties, Function<Sort, JPQLQuery<S>> finder,
			BiFunction<Sort, Pageable, JPQLQuery<S>> pagedFinder, Function<Predicate, Long> countOperation,
			Function<Predicate, Boolean> existsOperation, Class<S> entityType,
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> context) {

		super(resultType, sort, properties, context);
		this.predicate = predicate;
		this.finder = finder;
		this.pagedFinder = pagedFinder;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
		this.entityType = entityType;
	}

	@Override
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		return new FetchableFluentQueryByPredicate<>(this.predicate, this.resultType, this.sort.and(sort), this.properties,
				this.finder, this.pagedFinder, this.countOperation, this.existsOperation, this.entityType, this.context);
	}

	@Override
	public <NR> FetchableFluentQuery<NR> as(Class<NR> resultType) {

		if (!resultType.isInterface()) {
			throw new UnsupportedOperationException("Class-based DTOs are not yet supported.");
		}

		return new FetchableFluentQueryByPredicate<>(this.predicate, resultType, this.sort, this.properties, this.finder,
				this.pagedFinder, this.countOperation, this.existsOperation, this.entityType, this.context);
	}

	@Override
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryByPredicate<>(this.predicate, this.resultType, this.sort,
				mergeProperties(properties), this.finder, this.pagedFinder, this.countOperation, this.existsOperation,
				this.entityType, this.context);
	}

	@Override
	public R oneValue() {

		List<R> results = this.finder.apply(this.sort) //
				.limit(2) // Never need more than 2 values
				.stream() //
				.map(getConversionFunction(this.entityType, this.resultType)) //
				.collect(Collectors.toList());

		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1);
		}

		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public R firstValue() {

		List<R> results = this.finder.apply(this.sort) //
				.limit(1) // Never need more than 1 value
				.stream() //
				.map(getConversionFunction(this.entityType, this.resultType)) //
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
				.stream() //
				.map(getConversionFunction(this.entityType, this.resultType));
	}

	@Override
	public long count() {
		return this.countOperation.apply(this.predicate);
	}

	@Override
	public boolean exists() {
		return this.existsOperation.apply(this.predicate);
	}

	private Page<R> readPage(Pageable pageable) {

		JPQLQuery<S> pagedQuery = this.pagedFinder.apply(this.sort, pageable);

		List<R> paginatedResults = pagedQuery.stream() //
				.map(getConversionFunction(this.entityType, this.resultType)) //
				.collect(Collectors.toList());

		return PageableExecutionUtils.getPage(paginatedResults, pageable, () -> this.countOperation.apply(this.predicate));
	}
}
