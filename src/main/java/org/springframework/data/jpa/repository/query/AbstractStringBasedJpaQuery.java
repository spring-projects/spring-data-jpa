/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * Base class for {@link String} based JPA queries.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
abstract class AbstractStringBasedJpaQuery extends AbstractJpaQuery {

	private final StringQuery query;
	private final StringQuery countQuery;
	private final EvaluationContextProvider evaluationContextProvider;
	private final SpelExpressionParser parser;

	/**
	 * Creates a new {@link AbstractStringBasedJpaQuery} from the given {@link JpaQueryMethod}, {@link EntityManager} and
	 * query {@link String}.
	 * 
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param queryString must not be {@literal null}.
	 * @param evaluationContextProvider must not be {@literal null}.
	 * @param parser must not be {@literal null}.
	 */
	public AbstractStringBasedJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
			EvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {

		super(method, em);

		Assert.hasText(queryString, "Query string must not be null or empty!");
		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null!");
		Assert.notNull(parser, "Parser must not be null or empty!");

		this.evaluationContextProvider = evaluationContextProvider;
		this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation(), parser);
		this.countQuery = new StringQuery(method.getCountQuery() != null ? method.getCountQuery()
				: QueryUtils.createCountQueryFor(this.query.getQueryString(), method.getCountQueryProjection()));
		this.parser = parser;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateQuery(java.lang.Object[])
	 */
	@Override
	public Query doCreateQuery(Object[] values) {

		ParameterAccessor accessor = new ParametersParameterAccessor(getQueryMethod().getParameters(), values);
		String sortedQueryString = QueryUtils.applySorting(query.getQueryString(), accessor.getSort(), query.getAlias());

		Query query = createJpaQuery(sortedQueryString);

		return createBinder(values).bindAndPrepare(query);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#createBinder(java.lang.Object[])
	 */
	@Override
	protected ParameterBinder createBinder(Object[] values) {
		return new SpelExpressionStringQueryParameterBinder(getQueryMethod().getParameters(), values, query,
				evaluationContextProvider, parser);
	}

	/**
	 * Creates an appropriate JPA query from an {@link EntityManager} according to the current {@link AbstractJpaQuery}
	 * type.
	 * 
	 * @param queryString
	 * @return
	 */
	public Query createJpaQuery(String queryString) {
		return getEntityManager().createQuery(queryString);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
	 */
	@Override
	protected Query doCreateCountQuery(Object[] values) {

		String queryString = countQuery.getQueryString();
		EntityManager em = getEntityManager();

		return createBinder(values).bind(
				getQueryMethod().isNativeQuery() ? em.createNativeQuery(queryString) : em.createQuery(queryString, Long.class));
	}

	/**
	 * @return the query
	 */
	public StringQuery getQuery() {
		return query;
	}

	/**
	 * @return the countQuery
	 */
	public StringQuery getCountQuery() {
		return countQuery;
	}
}
