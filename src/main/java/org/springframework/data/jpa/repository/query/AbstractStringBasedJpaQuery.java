/*
 * Copyright 2008-2018 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.QueryParameterSetter.ErrorHandling.*;

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
 * @author Jens Schauder
 */
abstract class AbstractStringBasedJpaQuery extends AbstractJpaQuery {

	private final DeclaredQuery query;
	private final DeclaredQuery countQuery;
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
		this.countQuery = query.deriveCountQuery(method.getCountQuery(), method.getCountQueryProjection());

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

		// it is ok to reuse the binding contained in the ParameterBinder although we create a new query String because the
		// parameters in the query do not change.
		return parameterBinder.get().bindAndPrepare(query, values);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#createBinder(java.lang.Object[])
	 */
	@Override
	protected ParameterBinder createBinder() {

		return ParameterBinderFactory.createQueryAwareBinder(getQueryMethod().getParameters(), query, parser,
				evaluationContextProvider);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
	 */
	@Override
	protected Query doCreateCountQuery(Object[] values) {

		String queryString = countQuery.getQueryString();
		EntityManager em = getEntityManager();

		Query query = getQueryMethod().isNativeQuery() //
				? em.createNativeQuery(queryString) //
				: em.createQuery(queryString, Long.class);

		return parameterBinder.get().bind(query, values, LENIENT);
	}

	/**
	 * @return the query
	 */
	public DeclaredQuery getQuery() {
		return query;
	}

	/**
	 * @return the countQuery
	 */
	public DeclaredQuery getCountQuery() {
		return countQuery;
	}

	/**
	 * Creates an appropriate JPA query from an {@link EntityManager} according to the current {@link AbstractJpaQuery}
	 * type.
	 */
	protected Query createJpaQuery(String queryString) {

		EntityManager em = getEntityManager();

		if (this.query.hasConstructorExpression() || this.query.isDefaultProjection()) {
			return em.createQuery(queryString);
		}

		return getTypeToRead() //
				.<Query> map(it -> em.createQuery(queryString, it)) //
				.orElseGet(() -> em.createQuery(queryString));
	}
}
