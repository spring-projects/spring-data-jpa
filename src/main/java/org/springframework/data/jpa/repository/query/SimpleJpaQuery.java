/*
 * Copyright 2008-2013 the original author or authors.
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
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * {@link RepositoryQuery} implementation that inspects a {@link org.springframework.data.repository.query.QueryMethod}
 * for the existanve of an {@link org.springframework.data.jpa.repository.Query} annotation and creates a JPA
 * {@link Query} from it.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
final class SimpleJpaQuery extends AbstractJpaQuery {

	private static final Logger LOG = LoggerFactory.getLogger(SimpleJpaQuery.class);

	private final StringQuery query;
	private final StringQuery countQuery;

	private final JpaQueryMethod method;

	/**
	 * Creates a new {@link SimpleJpaQuery} that encapsulates a simple query string.
	 */
	SimpleJpaQuery(JpaQueryMethod method, EntityManager em, String queryString) {

		super(method, em);

		this.method = method;
		this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation());

		Parameters parameters = method.getParameters();
		boolean hasPagingOrSortingParameter = parameters.hasPageableParameter() || parameters.hasSortParameter();

		if (method.isNativeQuery() && hasPagingOrSortingParameter) {
			throw new IllegalStateException("Cannot use native queries with dynamic sorting and/or pagination!");
		}

		String preparedQueryString = this.query.getQuery();

		if (!method.isNativeQuery()) {
			validateQuery(preparedQueryString, em, String.format("Validation failed for query for method %s!", method));
		}

		this.countQuery = new StringQuery(method.getCountQuery() != null ? method.getCountQuery()
				: QueryUtils.createCountQueryFor(preparedQueryString));

		if (!method.isNativeQuery() && method.isPageQuery()) {
			validateQuery(this.countQuery.getQuery(), em,
					String.format("Count query validation failed for method %s!", method));
		}
	}

	/**
	 * Validates the given query for syntactical correctness.
	 * 
	 * @param query
	 * @param em
	 */
	private final void validateQuery(String query, EntityManager em, String errorMessage) {

		EntityManager validatingEm = null;

		try {

			validatingEm = em.getEntityManagerFactory().createEntityManager();
			validatingEm.createQuery(query);

		} catch (RuntimeException e) {

			// Needed as there's ambiguities in how an invalid query string shall be expressed by the persistence provider
			// http://java.net/projects/jpa-spec/lists/jsr338-experts/archive/2012-07/message/17
			throw new IllegalArgumentException(errorMessage, e);

		} finally {

			if (validatingEm != null) {
				validatingEm.close();
			}
		}
	}

	/**
	 * Creates a new {@link SimpleJpaQuery} encapsulating the query annotated on the given {@link JpaQueryMethod}.
	 */
	SimpleJpaQuery(JpaQueryMethod method, EntityManager em) {
		this(method, em, method.getAnnotatedQuery());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#createBinder(java.lang.Object[])
	 */
	@Override
	protected ParameterBinder createBinder(Object[] values) {
		return new StringQueryParameterBinder(getQueryMethod().getParameters(), values, query);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#createQuery(java.lang.Object[])
	 */
	@Override
	public Query doCreateQuery(Object[] values) {

		ParameterAccessor accessor = new ParametersParameterAccessor(method.getParameters(), values);
		String sortedQueryString = QueryUtils.applySorting(query.getQuery(), accessor.getSort(), query.getAlias());
		EntityManager em = getEntityManager();

		Query query = null;

		if (method.isNativeQuery()) {
			query = method.isQueryForEntity() ? em.createNativeQuery(sortedQueryString, method.getReturnedObjectType()) : em
					.createNativeQuery(sortedQueryString);
		} else {
			query = em.createQuery(sortedQueryString);
		}

		return createBinder(values).bindAndPrepare(query);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
	 */
	@Override
	protected TypedQuery<Long> doCreateCountQuery(Object[] values) {
		return createBinder(values).bind(getEntityManager().createQuery(countQuery.getQuery(), Long.class));
	}

	/**
	 * Creates a {@link RepositoryQuery} from the given {@link org.springframework.data.repository.query.QueryMethod} that
	 * is potentially annotated with {@link org.springframework.data.jpa.repository.Query}.
	 * 
	 * @param queryMethod
	 * @param em
	 * @return the {@link RepositoryQuery} derived from the annotation or {@code null} if no annotation found.
	 */
	public static RepositoryQuery fromQueryAnnotation(JpaQueryMethod queryMethod, EntityManager em) {

		LOG.debug("Looking up query for method {}", queryMethod.getName());

		String query = queryMethod.getAnnotatedQuery();

		return query == null ? null : new SimpleJpaQuery(queryMethod, em, query);
	}
}
