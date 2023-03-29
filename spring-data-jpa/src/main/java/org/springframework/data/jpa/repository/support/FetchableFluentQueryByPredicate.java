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
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.query.ScrollDelegate;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.Assert;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.AbstractJPAQuery;

/**
 * Immutable implementation of {@link FetchableFluentQuery} based on a Querydsl {@link Predicate}. All methods that
 * return a {@link FetchableFluentQuery} will return a new instance, not the original.
 *
 * @param <S> Domain type
 * @param <R> Result type
 * @author Greg Turnquist
 * @author Mark Paluch
 * @author Jens Schauder
 * @author J.R. Onyschak
 * @since 2.6
 */
class FetchableFluentQueryByPredicate<S, R> extends FluentQuerySupport<S, R> implements FetchableFluentQuery<R> {

	private final Predicate predicate;
	private final Function<Sort, AbstractJPAQuery<?, ?>> finder;

	private final PredicateScrollDelegate<S> scroll;
	private final BiFunction<Sort, Pageable, AbstractJPAQuery<?, ?>> pagedFinder;
	private final Function<Predicate, Long> countOperation;
	private final Function<Predicate, Boolean> existsOperation;
	private final EntityManager entityManager;

	public FetchableFluentQueryByPredicate(Predicate predicate, Class<S> entityType,
			Function<Sort, AbstractJPAQuery<?, ?>> finder, PredicateScrollDelegate<S> scroll,
			BiFunction<Sort, Pageable, AbstractJPAQuery<?, ?>> pagedFinder, Function<Predicate, Long> countOperation,
			Function<Predicate, Boolean> existsOperation, EntityManager entityManager) {
		this(predicate, entityType, (Class<R>) entityType, Sort.unsorted(), 0, Collections.emptySet(), finder, scroll,
				pagedFinder, countOperation, existsOperation, entityManager);
	}

	private FetchableFluentQueryByPredicate(Predicate predicate, Class<S> entityType, Class<R> resultType, Sort sort,
			int limit, Collection<String> properties, Function<Sort, AbstractJPAQuery<?, ?>> finder,
			PredicateScrollDelegate<S> scroll, BiFunction<Sort, Pageable, AbstractJPAQuery<?, ?>> pagedFinder,
			Function<Predicate, Long> countOperation, Function<Predicate, Boolean> existsOperation,
			EntityManager entityManager) {

		super(resultType, sort, limit, properties, entityType);
		this.predicate = predicate;
		this.finder = finder;
		this.scroll = scroll;
		this.pagedFinder = pagedFinder;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
		this.entityManager = entityManager;
	}

	@Override
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		Assert.notNull(sort, "Sort must not be null");

		return new FetchableFluentQueryByPredicate<>(predicate, entityType, resultType, this.sort.and(sort), limit,
				properties, finder, scroll, pagedFinder, countOperation, existsOperation, entityManager);
	}

	@Override
	public FetchableFluentQuery<R> limit(int limit) {

		Assert.isTrue(limit >= 0, "Limit must not be negative");

		return new FetchableFluentQueryByPredicate<>(predicate, entityType, resultType, sort, limit, properties, finder,
				scroll, pagedFinder, countOperation, existsOperation, entityManager);
	}

	@Override
	public <NR> FetchableFluentQuery<NR> as(Class<NR> resultType) {

		Assert.notNull(resultType, "Projection target type must not be null");

		if (!resultType.isInterface()) {
			throw new UnsupportedOperationException("Class-based DTOs are not yet supported.");
		}

		return new FetchableFluentQueryByPredicate<>(predicate, entityType, resultType, sort, limit, properties, finder,
				scroll, pagedFinder, countOperation, existsOperation, entityManager);
	}

	@Override
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryByPredicate<>(predicate, entityType, resultType, sort, limit,
				mergeProperties(properties), finder, scroll, pagedFinder, countOperation, existsOperation, entityManager);
	}

	@Override
	public R oneValue() {

		List<?> results = createSortedAndProjectedQuery() //
				.limit(2) // Never need more than 2 values
				.fetch();

		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1);
		}

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public R firstValue() {

		List<?> results = createSortedAndProjectedQuery() //
				.limit(1) // Never need more than 1 value
				.fetch();

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public List<R> all() {
		return convert(createSortedAndProjectedQuery().fetch());
	}

	@Override
	public Window<R> scroll(ScrollPosition scrollPosition) {

		Assert.notNull(scrollPosition, "ScrollPosition must not be null");

		return scroll.scroll(sort, limit, scrollPosition).map(getConversionFunction());
	}

	@Override
	public Page<R> page(Pageable pageable) {
		return pageable.isUnpaged() ? new PageImpl<>(all()) : readPage(pageable);
	}

	@Override
	public Stream<R> stream() {

		return createSortedAndProjectedQuery() //
				.stream() //
				.map(getConversionFunction());
	}

	@Override
	public long count() {
		return countOperation.apply(predicate);
	}

	@Override
	public boolean exists() {
		return existsOperation.apply(predicate);
	}

	private AbstractJPAQuery<?, ?> createSortedAndProjectedQuery() {

		AbstractJPAQuery<?, ?> query = finder.apply(sort);

		if (!properties.isEmpty()) {
			query.setHint(EntityGraphFactory.HINT, EntityGraphFactory.create(entityManager, entityType, properties));
		}

		if (limit != 0) {
			query.limit(limit);
		}

		return query;
	}

	private Page<R> readPage(Pageable pageable) {

		AbstractJPAQuery<?, ?> query = pagedFinder.apply(sort, pageable);

		if (!properties.isEmpty()) {
			query.setHint(EntityGraphFactory.HINT, EntityGraphFactory.create(entityManager, entityType, properties));
		}

		List<R> paginatedResults = convert(query.fetch());

		return PageableExecutionUtils.getPage(paginatedResults, pageable, () -> countOperation.apply(predicate));
	}

	private List<R> convert(List<?> resultList) {

		Function<Object, R> conversionFunction = getConversionFunction();
		List<R> mapped = new ArrayList<>(resultList.size());

		for (Object o : resultList) {
			mapped.add(conversionFunction.apply(o));
		}

		return mapped;
	}

	private Function<Object, R> getConversionFunction() {
		return getConversionFunction(entityType, resultType);
	}


	static class PredicateScrollDelegate<T> extends ScrollDelegate<T> {

		private final ScrollQueryFactory scrollFunction;

		PredicateScrollDelegate(ScrollQueryFactory scrollQueryFactory, JpaEntityInformation<T, ?> entity) {
			super(entity);
			this.scrollFunction = scrollQueryFactory;
		}

		public Window<T> scroll(Sort sort, int limit, ScrollPosition scrollPosition) {

			Query query = scrollFunction.createQuery(sort, scrollPosition);
			if (limit > 0) {
				query = query.setMaxResults(limit);
			}
			return scroll(query, sort, scrollPosition);
		}
	}

}
