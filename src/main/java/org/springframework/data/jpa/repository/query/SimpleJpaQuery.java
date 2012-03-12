/*
 * Copyright 2008-2011 the original author or authors.
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
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * {@link RepositoryQuery} implementation that inspects a {@link QueryMethod} for the existanve of an
 * {@link org.springframework.data.jpa.repository.Query} annotation and creates a JPA {@link Query} from it.
 * 
 * @author Oliver Gierke
 */
final class SimpleJpaQuery extends AbstractJpaQuery {

	private static final Logger LOG = LoggerFactory.getLogger(SimpleJpaQuery.class);

	private final String queryString;
	private final String countQuery;
	private final String alias;

	private final JpaQueryMethod method;

	/**
	 * Creates a new {@link SimpleJpaQuery} that encapsulates a simple query string.
	 */
	SimpleJpaQuery(JpaQueryMethod method, EntityManager em, String queryString) {

		super(method, em);

		this.queryString = queryString;
		this.alias = QueryUtils.detectAlias(queryString);
		this.countQuery = method.getCountQuery() == null ? QueryUtils.createCountQueryFor(queryString) : method
				.getCountQuery();
		this.method = method;

		Parameters parameters = method.getParameters();
		boolean hasPagingOrSortingParameter = parameters.hasPageableParameter() || parameters.hasSortParameter();

		if (method.isNativeQuery() && hasPagingOrSortingParameter) {
			throw new IllegalStateException("Cannot use native queries with dynamic sorting and/or pagination!");
		}

		// Try to create a Query object already to fail fast
		if (!method.isNativeQuery()) {
			em.createQuery(queryString);
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
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#createQuery(java.lang.Object[])
	 */
	@Override
	public Query doCreateQuery(Object[] values) {

		ParameterAccessor accessor = new ParametersParameterAccessor(method.getParameters(), values);
		String sortedQueryString = QueryUtils.applySorting(queryString, accessor.getSort(), alias);

		Query query = null;

		if (method.isNativeQuery()) {
			query = method.isModifyingQuery() ? getEntityManager().createNativeQuery(sortedQueryString) : getEntityManager()
					.createNativeQuery(sortedQueryString, method.getReturnedObjectType());
		} else {
			query = method.isModifyingQuery() ? getEntityManager().createQuery(sortedQueryString) : getEntityManager()
					.createQuery(sortedQueryString);
		}

		return createBinder(values).bindAndPrepare(query);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
	 */
	@Override
	protected TypedQuery<Long> doCreateCountQuery(Object[] values) {

		return createBinder(values).bind(getEntityManager().createQuery(countQuery, Long.class));
	}

	/**
	 * Creates a {@link RepositoryQuery} from the given {@link QueryMethod} that is potentially annotated with
	 * {@link org.springframework.data.jpa.repository.Query}.
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