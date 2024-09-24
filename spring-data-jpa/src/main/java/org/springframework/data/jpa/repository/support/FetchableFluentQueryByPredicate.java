/*
 * Copyright 2021-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.query.ScrollDelegate;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionBase;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Visitor;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLSerializer;
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
 * @author Christoph Strobl
 * @since 2.6
 */
class FetchableFluentQueryByPredicate<S, R> extends FluentQuerySupport<S, R> implements FetchableFluentQuery<R> {

	private final EntityPath<?> entityPath;
	private final JpaEntityInformation<S, ?> entityInformation;
	private final ScrollQueryFactory<AbstractJPAQuery<?, ?>> scrollQueryFactory;
	private final Predicate predicate;
	private final Function<Sort, AbstractJPAQuery<?, ?>> finder;

	private final BiFunction<Sort, Pageable, AbstractJPAQuery<?, ?>> pagedFinder;
	private final Function<Predicate, Long> countOperation;
	private final Function<Predicate, Boolean> existsOperation;
	private final EntityManager entityManager;

	FetchableFluentQueryByPredicate(EntityPath<?> entityPath, Predicate predicate,
			JpaEntityInformation<S, ?> entityInformation, Function<Sort, AbstractJPAQuery<?, ?>> finder,
			ScrollQueryFactory<AbstractJPAQuery<?, ?>> scrollQueryFactory,
			BiFunction<Sort, Pageable, AbstractJPAQuery<?, ?>> pagedFinder, Function<Predicate, Long> countOperation,
			Function<Predicate, Boolean> existsOperation, EntityManager entityManager, ProjectionFactory projectionFactory) {
		this(entityPath, predicate, entityInformation, (Class<R>) entityInformation.getJavaType(), Sort.unsorted(), 0,
				Collections.emptySet(), finder, scrollQueryFactory,
				pagedFinder, countOperation, existsOperation, entityManager, projectionFactory);
	}

	private FetchableFluentQueryByPredicate(EntityPath<?> entityPath, Predicate predicate,
			JpaEntityInformation<S, ?> entityInformation, Class<R> resultType, Sort sort, int limit,
			Collection<String> properties, Function<Sort, AbstractJPAQuery<?, ?>> finder,
			ScrollQueryFactory<AbstractJPAQuery<?, ?>> scrollQueryFactory,
			BiFunction<Sort, Pageable, AbstractJPAQuery<?, ?>> pagedFinder,
			Function<Predicate, Long> countOperation, Function<Predicate, Boolean> existsOperation,
			EntityManager entityManager, ProjectionFactory projectionFactory) {

		super(resultType, sort, limit, properties, entityInformation.getJavaType(), projectionFactory);
		this.entityInformation = entityInformation;
		this.entityPath = entityPath;
		this.predicate = predicate;
		this.finder = finder;
		this.scrollQueryFactory = scrollQueryFactory;
		this.pagedFinder = pagedFinder;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
		this.entityManager = entityManager;
	}

	@Override
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		Assert.notNull(sort, "Sort must not be null");

		return new FetchableFluentQueryByPredicate<>(entityPath, predicate, entityInformation, resultType,
				this.sort.and(sort), limit, properties, finder, scrollQueryFactory, pagedFinder, countOperation,
				existsOperation, entityManager, projectionFactory);
	}

	@Override
	public FetchableFluentQuery<R> limit(int limit) {

		Assert.isTrue(limit >= 0, "Limit must not be negative");

		return new FetchableFluentQueryByPredicate<>(entityPath, predicate, entityInformation, resultType, sort, limit,
				properties, finder, scrollQueryFactory, pagedFinder, countOperation, existsOperation, entityManager,
				projectionFactory);
	}

	@Override
	public <NR> FetchableFluentQuery<NR> as(Class<NR> resultType) {

		Assert.notNull(resultType, "Projection target type must not be null");

		return new FetchableFluentQueryByPredicate<>(entityPath, predicate, entityInformation, resultType, sort, limit,
				properties, finder, scrollQueryFactory, pagedFinder, countOperation, existsOperation, entityManager,
				projectionFactory);
	}

	@Override
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryByPredicate<>(entityPath, predicate, entityInformation, resultType, sort, limit,
				mergeProperties(properties), finder, scrollQueryFactory, pagedFinder, countOperation, existsOperation,
				entityManager,
				projectionFactory);
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

		return new PredicateScrollDelegate<>(scrollQueryFactory, entityInformation)
				.scroll(returnedType, sort, limit, scrollPosition).map(getConversionFunction());
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
		applyQuerySettings(this.returnedType, this.limit, query, null);
		return query;
	}

	private void applyQuerySettings(ReturnedType returnedType, int limit, AbstractJPAQuery<?, ?> query,
			@Nullable ScrollPosition scrollPosition) {

		List<String> inputProperties = returnedType.getInputProperties();

		if (returnedType.needsCustomConstruction() && !inputProperties.isEmpty()) {

			Collection<String> requiredSelection;
			if (scrollPosition instanceof KeysetScrollPosition && returnedType.getReturnedType().isInterface()) {
				requiredSelection = new LinkedHashSet<>(inputProperties);
				sort.forEach(it -> requiredSelection.add(it.getProperty()));
				entityInformation.getIdAttributeNames().forEach(requiredSelection::add);
			} else {
				requiredSelection = inputProperties;
			}

			PathBuilder<?> builder = new PathBuilder<>(entityPath.getType(), entityPath.getMetadata());
			Expression<?>[] projection = requiredSelection.stream().map(builder::get).toArray(Expression[]::new);

			if (returnedType.getReturnedType().isInterface()) {
				query.select(new JakartaTuple(projection));
			} else {
				query.select(new DtoProjection(returnedType.getReturnedType(), projection));
			}
		}

		if (!properties.isEmpty()) {
			query.setHint(EntityGraphFactory.HINT, EntityGraphFactory.create(entityManager, entityType, properties));
		}

		if (limit != 0) {
			query.limit(limit);
		}
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

	class PredicateScrollDelegate<T> extends ScrollDelegate<T> {

		private final ScrollQueryFactory<AbstractJPAQuery<?, ?>> scrollFunction;

		PredicateScrollDelegate(ScrollQueryFactory<AbstractJPAQuery<?, ?>> scrollQueryFactory,
				JpaEntityInformation<T, ?> entity) {
			super(entity);
			this.scrollFunction = scrollQueryFactory;
		}

		public Window<T> scroll(ReturnedType returnedType, Sort sort, int limit, ScrollPosition scrollPosition) {

			AbstractJPAQuery<?, ?> query = scrollFunction.createQuery(returnedType, sort, scrollPosition);

			applyQuerySettings(returnedType, limit, query, scrollPosition);

			return scroll(query.createQuery(), sort, scrollPosition);
		}
	}

	private static class DtoProjection extends ExpressionBase<Object> {

		private final Expression<?>[] projection;

		public DtoProjection(Class<?> resultType, Expression<?>[] projection) {
			super(resultType);
			this.projection = projection;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <R, C> R accept(Visitor<R, C> v, @Nullable C context) {

			if (v instanceof JPQLSerializer s) {

				s.append("new ").append(getType().getName()).append("(");
				boolean first = true;
				for (Expression<?> expression : projection) {
					if (first) {
						first = false;
					} else {
						s.append(", ");
					}

					expression.accept(v, context);
				}

				s.append(")");
			}

			return (R) this;
		}
	}
}
