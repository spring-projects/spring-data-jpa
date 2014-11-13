/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.support;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.JpaEntityGraph;
import org.springframework.data.jpa.util.Jpa21Utils;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QSort;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.FactoryExpression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.PathBuilder;

/**
 * QueryDsl specific extension of {@link SimpleJpaRepository} which adds implementation for
 * {@link QueryDslPredicateExecutor}.
 * 
 * @author Oliver Gierke
 */
public class QueryDslJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements
		QueryDslPredicateExecutor<T> {

	private static final EntityPathResolver DEFAULT_ENTITY_PATH_RESOLVER = SimpleEntityPathResolver.INSTANCE;

	private final EntityPath<T> path;
	private final PathBuilder<T> builder;
	private final Querydsl querydsl;
	private final EntityManager em;

	/**
	 * Creates a new {@link QueryDslJpaRepository} from the given domain class and {@link EntityManager}. This will use
	 * the {@link SimpleEntityPathResolver} to translate the given domain class into an {@link EntityPath}.
	 * 
	 * @param entityInformation must not be {@literal null}.
	 * @param entityManager must not be {@literal null}.
	 */
	public QueryDslJpaRepository(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
		this(entityInformation, entityManager, DEFAULT_ENTITY_PATH_RESOLVER);
	}

	/**
	 * Creates a new {@link QueryDslJpaRepository} from the given domain class and {@link EntityManager} and uses the
	 * given {@link EntityPathResolver} to translate the domain class into an {@link EntityPath}.
	 * 
	 * @param entityInformation must not be {@literal null}.
	 * @param entityManager must not be {@literal null}.
	 * @param resolver must not be {@literal null}.
	 */
	public QueryDslJpaRepository(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager,
			EntityPathResolver resolver) {

		super(entityInformation, entityManager);
		this.em = entityManager;
		this.path = resolver.createPath(entityInformation.getJavaType());
		this.builder = new PathBuilder<T>(path.getType(), path.getMetadata());
		this.querydsl = new Querydsl(entityManager, builder);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findOne(com.mysema.query.types.Predicate)
	 */
	public T findOne(Predicate predicate) {
		return createQuery(predicate).uniqueResult(path);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.mysema.query.types.Predicate)
	 */
	public List<T> findAll(Predicate predicate) {
		return createQuery(predicate).list(path);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.mysema.query.types.Predicate, com.mysema.query.types.OrderSpecifier<?>[])
	 */
	public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

		JPQLQuery query = createQuery(predicate);
		query = querydsl.applySorting(new QSort(orders), query);
		return query.list(path);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#findAll(com.mysema.query.types.Predicate, org.springframework.data.domain.Pageable)
	 */
	public Page<T> findAll(Predicate predicate, Pageable pageable) {

		JPQLQuery countQuery = createQuery(predicate);
		JPQLQuery query = querydsl.applyPagination(pageable, createQuery(predicate));

		Long total = countQuery.count();
		List<T> content = total > pageable.getOffset() ? query.list(path) : Collections.<T> emptyList();

		return new PageImpl<T>(content, pageable, total);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.QueryDslPredicateExecutor#count(com.mysema.query.types.Predicate)
	 */
	public long count(Predicate predicate) {
		return createQuery(predicate).count();
	}

	/**
	 * Creates a new {@link JPQLQuery} for the given {@link Predicate}.
	 * 
	 * @param predicate
	 * @return the Querydsl {@link JPQLQuery}.
	 */
	protected JPQLQuery createQuery(Predicate... predicate) {

		JPAQuery query = querydsl.createQuery(path).where(predicate);
		CrudMethodMetadata metadata = getRepositoryMethodMetadata();

		if (metadata == null) {
			return query;
		}

		LockModeType type = metadata.getLockModeType();
		query = type == null ? query : query.setLockMode(type);

		for (Entry<String, Object> hint : metadata.getQueryHints().entrySet()) {
			query.setHint(hint.getKey(), hint.getValue());
		}

		JpaEntityGraph jpaEntityGraph = metadata.getEntityGraph();

		if (jpaEntityGraph == null) {
			return query;
		}

		EntityGraph<?> entityGraph = Jpa21Utils.tryGetFetchGraph(em, jpaEntityGraph);

		if (entityGraph == null) {
			return query;
		}

		query.setHint(jpaEntityGraph.getType().getKey(), entityGraph);

		return query;
	}

	@Override
	public Page<T> findAll(FactoryExpression<T> factoryExpression, Predicate predicate, Pageable pageable) {
		JPQLQuery countQuery = createQuery(predicate);
		JPQLQuery query = querydsl.applyPagination(pageable, createQuery(predicate));

		Long total = countQuery.count();
		List<T> content = total > pageable.getOffset() ? query.list(factoryExpression) : Collections.<T> emptyList();

		return new PageImpl<T>(content, pageable, total);
	}
}
