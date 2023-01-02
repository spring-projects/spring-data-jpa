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
package org.springframework.data.jpa.repository.support;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QSort;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.AbstractJPAQuery;

/**
 * QueryDsl specific extension of {@link SimpleJpaRepository} which adds implementation for
 * {@link QuerydslPredicateExecutor}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Jocelyn Ntakpe
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Greg Turnquist
 * @author Yanming Zhou
 * @deprecated Instead of this class use {@link QuerydslJpaPredicateExecutor}
 */
@Deprecated
public class QuerydslJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
		implements QuerydslPredicateExecutor<T> {

	private final EntityPath<T> path;
	private final PathBuilder<T> builder;
	private final Querydsl querydsl;
	private final EntityManager entityManager;

	/**
	 * Creates a new {@link QuerydslJpaRepository} from the given domain class and {@link EntityManager}. This will use
	 * the {@link SimpleEntityPathResolver} to translate the given domain class into an {@link EntityPath}.
	 *
	 * @param entityInformation must not be {@literal null}.
	 * @param entityManager must not be {@literal null}.
	 */
	public QuerydslJpaRepository(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
		this(entityInformation, entityManager, SimpleEntityPathResolver.INSTANCE);
	}

	/**
	 * Creates a new {@link QuerydslJpaRepository} from the given domain class and {@link EntityManager} and uses the
	 * given {@link EntityPathResolver} to translate the domain class into an {@link EntityPath}.
	 *
	 * @param entityInformation must not be {@literal null}.
	 * @param entityManager must not be {@literal null}.
	 * @param resolver must not be {@literal null}.
	 */
	public QuerydslJpaRepository(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager,
			EntityPathResolver resolver) {

		super(entityInformation, entityManager);

		this.path = resolver.createPath(entityInformation.getJavaType());
		this.builder = new PathBuilder<>(path.getType(), path.getMetadata());
		this.querydsl = new Querydsl(entityManager, builder);
		this.entityManager = entityManager;
	}

	@Override
	public Optional<T> findOne(Predicate predicate) {

		try {
			return Optional.ofNullable(createQuery(predicate).select(path).limit(2).fetchOne());
		} catch (NonUniqueResultException ex) {
			throw new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
		}
	}

	@Override
	public List<T> findAll(Predicate predicate) {
		return createQuery(predicate).select(path).fetch();
	}

	@Override
	public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {
		return executeSorted(createQuery(predicate).select(path), orders);
	}

	@Override
	public List<T> findAll(Predicate predicate, Sort sort) {

		Assert.notNull(sort, "Sort must not be null");

		return executeSorted(createQuery(predicate).select(path), sort);
	}

	@Override
	public List<T> findAll(OrderSpecifier<?>... orders) {

		Assert.notNull(orders, "Order specifiers must not be null");

		return executeSorted(createQuery(new Predicate[0]).select(path), orders);
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable) {

		Assert.notNull(pageable, "Pageable must not be null");

		final JPQLQuery<?> countQuery = createCountQuery(predicate);
		JPQLQuery<T> query = querydsl.applyPagination(pageable, createQuery(predicate).select(path));

		return PageableExecutionUtils.getPage(query.fetch(), pageable, countQuery::fetchCount);
	}

	@Override
	public <S extends T, R> R findBy(Predicate predicate,
			Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
		throw new UnsupportedOperationException(
				"Fluent Query API support for Querydsl is only found in QuerydslJpaPredicateExecutor.");
	}

	@Override
	public long count(Predicate predicate) {
		return createQuery(predicate).fetchCount();
	}

	@Override
	public boolean exists(Predicate predicate) {
		return createQuery(predicate).fetchCount() > 0;
	}

	/**
	 * Creates a new {@link JPQLQuery} for the given {@link Predicate}.
	 *
	 * @param predicate
	 * @return the Querydsl {@link JPQLQuery}.
	 */
	protected JPQLQuery<?> createQuery(Predicate... predicate) {

		AbstractJPAQuery<?, ?> query = doCreateQuery(getQueryHints().withFetchGraphs(entityManager), predicate);

		CrudMethodMetadata metadata = getRepositoryMethodMetadata();

		if (metadata == null) {
			return query;
		}

		LockModeType type = metadata.getLockModeType();
		return type == null ? query : query.setLockMode(type);
	}

	/**
	 * Creates a new {@link JPQLQuery} count query for the given {@link Predicate}.
	 *
	 * @param predicate, can be {@literal null}.
	 * @return the Querydsl count {@link JPQLQuery}.
	 */
	protected JPQLQuery<?> createCountQuery(@Nullable Predicate... predicate) {
		return doCreateQuery(getQueryHints(), predicate);
	}

	private AbstractJPAQuery<?, ?> doCreateQuery(QueryHints hints, @Nullable Predicate... predicate) {

		AbstractJPAQuery<?, ?> query = querydsl.createQuery(path);

		if (predicate != null) {
			query = query.where(predicate);
		}

		hints.forEach(query::setHint);

		return query;
	}

	/**
	 * Executes the given {@link JPQLQuery} after applying the given {@link OrderSpecifier}s.
	 *
	 * @param query must not be {@literal null}.
	 * @param orders must not be {@literal null}.
	 * @return
	 */
	private List<T> executeSorted(JPQLQuery<T> query, OrderSpecifier<?>... orders) {
		return executeSorted(query, new QSort(orders));
	}

	/**
	 * Executes the given {@link JPQLQuery} after applying the given {@link Sort}.
	 *
	 * @param query must not be {@literal null}.
	 * @param sort must not be {@literal null}.
	 * @return
	 */
	private List<T> executeSorted(JPQLQuery<T> query, Sort sort) {
		return querydsl.applySorting(sort, query).fetch();
	}
}
