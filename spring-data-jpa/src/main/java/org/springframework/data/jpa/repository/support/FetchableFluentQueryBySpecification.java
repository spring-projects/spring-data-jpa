/*
 * Copyright 2021-2025 the original author or authors.
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
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor.SpecificationFluentQuery;
import org.springframework.data.jpa.repository.query.ScrollDelegate;
import org.springframework.data.jpa.support.PageableUtils;
import org.springframework.data.projection.ProjectionFactory;
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
 * @author Christoph Strobl
 * @since 3.0
 */
class FetchableFluentQueryBySpecification<S, R> extends FluentQuerySupport<S, R>
		implements FluentQuery.FetchableFluentQuery<R>, SpecificationFluentQuery<R> {

	private final Specification<S> spec;
	private final Function<FluentQuerySupport<?, ?>, TypedQuery<S>> finder;
	private final SpecificationScrollDelegate<S> scroll;
	private final Function<Specification<S>, Long> countOperation;
	private final Function<Specification<S>, Boolean> existsOperation;
	private final EntityManager entityManager;

	FetchableFluentQueryBySpecification(Specification<S> spec, Class<S> entityType,
			Function<FluentQuerySupport<?, ?>, TypedQuery<S>> finder, SpecificationScrollDelegate<S> scrollDelegate,
			Function<Specification<S>, Long> countOperation, Function<Specification<S>, Boolean> existsOperation,
			EntityManager entityManager, ProjectionFactory projectionFactory) {
		this(spec, entityType, (Class<R>) entityType, Sort.unsorted(), 0, Collections.emptySet(), finder, scrollDelegate,
				countOperation, existsOperation, entityManager, projectionFactory);
	}

	private FetchableFluentQueryBySpecification(Specification<S> spec, Class<S> entityType, Class<R> resultType,
			Sort sort, int limit, Collection<String> properties, Function<FluentQuerySupport<?, ?>, TypedQuery<S>> finder,
			SpecificationScrollDelegate<S> scrollDelegate, Function<Specification<S>, Long> countOperation,
			Function<Specification<S>, Boolean> existsOperation, EntityManager entityManager,
			ProjectionFactory projectionFactory) {

		super(resultType, sort, limit, properties, entityType, projectionFactory);
		this.spec = spec;
		this.finder = finder;
		this.scroll = scrollDelegate;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
		this.entityManager = entityManager;
	}

	@Override
	public SpecificationFluentQuery<R> sortBy(Sort sort) {

		Assert.notNull(sort, "Sort must not be null");

		return getSorted(this.sort.and(sort));
	}

	private FetchableFluentQueryBySpecification<S, R> getSorted(Sort sort) {

		if (this.sort == sort) {
			return this;
		}

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder,
				scroll, countOperation, existsOperation, entityManager, projectionFactory);
	}

	@Override
	public SpecificationFluentQuery<R> limit(int limit) {

		Assert.isTrue(limit >= 0, "Limit must not be negative");

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder,
				scroll, countOperation, existsOperation, entityManager, projectionFactory);
	}

	@Override
	public <NR> SpecificationFluentQuery<NR> as(Class<NR> resultType) {

		Assert.notNull(resultType, "Projection target type must not be null");

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder,
				scroll, countOperation, existsOperation, entityManager, projectionFactory);
	}

	@Override
	public SpecificationFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder,
				scroll, countOperation, existsOperation, entityManager, projectionFactory);
	}

	@Override
	public @Nullable R oneValue() {

		List<?> results = createSortedAndProjectedQuery(this.sort) //
				.setMaxResults(2) // Never need more than 2 values
				.getResultList();

		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1);
		}

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public @Nullable R firstValue() {

		List<?> results = createSortedAndProjectedQuery(this.sort) //
				.setMaxResults(1) // Never need more than 1 value
				.getResultList();

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public List<R> all() {
		return all(this.sort);
	}

	private List<R> all(Sort sort) {
		return convert(createSortedAndProjectedQuery(sort).getResultList());
	}

	@Override
	public Window<R> scroll(ScrollPosition scrollPosition) {

		Assert.notNull(scrollPosition, "ScrollPosition must not be null");

		return scroll.scroll(this, scrollPosition).map(getConversionFunction());
	}

	@Override
	public Slice<R> slice(Pageable pageable) {
		return pageable.isUnpaged() ? new PageImpl<>(all(pageable.getSort())) : readSlice(pageable);
	}

	@Override
	public Page<R> page(Pageable pageable) {
		return pageable.isUnpaged() ? new PageImpl<>(all(pageable.getSort())) : readPage(pageable, spec);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Page<R> page(Pageable pageable, Specification<?> countSpec) {
		return pageable.isUnpaged() ? new PageImpl<>(all(pageable.getSort()))
				: readPage(pageable, (Specification) countSpec);
	}

	@Override
	public Stream<R> stream() {

		return createSortedAndProjectedQuery(this.sort) //
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

	private TypedQuery<S> createSortedAndProjectedQuery(Sort sort) {

		TypedQuery<S> query = finder.apply(getSorted(sort));

		if (!properties.isEmpty()) {
			query.setHint(EntityGraphFactory.HINT, EntityGraphFactory.create(entityManager, entityType, properties));
		}

		if (limit != 0) {
			query.setMaxResults(limit);
		}

		return query;
	}

	private Slice<R> readSlice(Pageable pageable) {

		TypedQuery<S> pagedQuery = createSortedAndProjectedQuery(pageable.getSort());

		if (pageable.isPaged()) {
			pagedQuery.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
			pagedQuery.setMaxResults(pageable.getPageSize() + 1);
		}

		List<S> resultList = pagedQuery.getResultList();
		boolean hasNext = resultList.size() > pageable.getPageSize();
		if (hasNext) {
			resultList = resultList.subList(0, pageable.getPageSize());
		}

		List<R> slice = convert(resultList);

		return new SliceImpl<>(slice, pageable, hasNext);
	}

	private Slice<R> readSlice(Pageable pageable, @Nullable Specification<S> countSpec) {

		TypedQuery<S> pagedQuery = createSortedAndProjectedQuery(pageable.getSort());

		if (pageable.isPaged()) {
			pagedQuery.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
			pagedQuery.setMaxResults(pageable.getPageSize() + 1);
		}

		List<S> resultList = pagedQuery.getResultList();
		boolean hasNext = resultList.size() > pageable.getPageSize();
		if (hasNext) {
			resultList = resultList.subList(0, pageable.getPageSize());
		}

		List<R> slice = convert(resultList);

		return new SliceImpl<>(slice, pageable, hasNext);
	}

	private Page<R> readPage(Pageable pageable, @Nullable Specification<S> countSpec) {

		Sort sort = pageable.getSortOr(this.sort);
		TypedQuery<S> pagedQuery = createSortedAndProjectedQuery(sort);

		if (pageable.isPaged()) {
			pagedQuery.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
			pagedQuery.setMaxResults(pageable.getPageSize());
		}

		List<R> paginatedResults = convert(pagedQuery.getResultList());

		return PageableExecutionUtils.getPage(paginatedResults, withSort(pageable, sort),
				() -> countOperation.apply(countSpec));
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

	static class SpecificationScrollDelegate<T> extends ScrollDelegate<T> {

		private final ScrollQueryFactory<TypedQuery<T>> scrollFunction;

		SpecificationScrollDelegate(ScrollQueryFactory<TypedQuery<T>> scrollQueryFactory,
				JpaEntityInformation<T, ?> entity) {
			super(entity);
			this.scrollFunction = scrollQueryFactory;
		}

		public Window<T> scroll(FluentQuerySupport<?, ?> q, ScrollPosition scrollPosition) {

			Query query = scrollFunction.createQuery(q, scrollPosition);

			if (q.limit > 0) {
				query = query.setMaxResults(q.limit);
			}

			return scroll(query, q.sort, scrollPosition);
		}
	}
}
