/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.EntityType;

import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

import com.mysema.commons.lang.Assert;

/**
 * An {@link AbstractJpaQueryContext} used to handle {@link Example}-based queries.
 *
 * @param <T>
 * @author Greg Turnquist
 */
class QueryByExampleQueryContext<T> extends AbstractJpaQueryContext {

	private final Example<T> example;
	private final Sort sort;
	private final Consumer<Query> queryHints;

	QueryByExampleQueryContext(EntityManager entityManager, Example<T> example, Sort sort, Consumer<Query> queryHints) {

		super(Optional.empty(), entityManager);

		Assert.notNull(example, "example must not be null");
		Assert.notNull(sort, "sort must not be null");
		Assert.notNull(queryHints, "queryHints must not be null");

		this.example = example;
		this.sort = sort;
		this.queryHints = queryHints;
	}

	@Override
	protected ContextualQuery createQuery(JpaParametersParameterAccessor accessor) {
		return doCreateQuery("select %s from %s %s", false);
	}

	@Override
	protected Query turnIntoJpaQuery(ContextualQuery query, JpaParametersParameterAccessor accessor) {
		return getEntityManager().createQuery(query.getQuery(), (Class<?>) example.getProbeType());
	}

	@Override
	public Query createJpaCountQuery(JpaParametersParameterAccessor accessor) {

		ContextualQuery contextualQuery = doCreateQuery("select count(%s) from %s %s", true);

		return getEntityManager().createQuery(contextualQuery.getQuery(), Long.class);
	}

	private ContextualQuery doCreateQuery(String queryTemplate, boolean countQuery) {

		EntityType<?> model = getEntityManager().getMetamodel().entity(example.getProbeType());

		String alias = alias(example.getProbeType());
		String entityName = model.getName();

		String query = String.format(queryTemplate, alias, entityName, alias);

		ExpressionContext predicate = QueryByExampleExpressionBuilder.getExpression(model, example);

		if (predicate != null) {

			String joins = predicate.joins(alias);
			query += joins.isEmpty() ? "" : " " + joins;

			String criteria = predicate.criteria(alias);
			query += criteria.isEmpty() ? "" : " where " + criteria;
		}

		if (!countQuery && sort != null && sort.isSorted()) {
			query += " order by " + String.join(",", QueryUtils.toOrders(sort, example.getProbeType()));
		}

		System.out.println(query);

		return ContextualQuery.of(query);
	}

	@Override
	protected Query applyQueryHints(Query query) {

		queryHints.accept(query);
		return query;
	}
}
