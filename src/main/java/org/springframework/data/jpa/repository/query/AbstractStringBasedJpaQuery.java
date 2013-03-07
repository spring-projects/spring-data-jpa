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

import org.springframework.data.jpa.repository.support.JpaQueryContext;
import org.springframework.data.repository.augment.QueryAugmentationEngine;
import org.springframework.data.repository.augment.QueryContext.QueryMode;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
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

	/**
	 * Creates a new {@link AbstractStringBasedJpaQuery} from the given {@link JpaQueryMethod}, {@link EntityManager} and
	 * query {@link String}.
	 * 
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param queryString must not be {@literal null}.
	 */
	public AbstractStringBasedJpaQuery(JpaQueryMethod method, EntityManager em, String queryString) {

		super(method, em);

		Assert.hasText(queryString, "Query string must not be null or empty!");

		this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation());
		this.countQuery = new StringQuery(method.getCountQuery() != null ? method.getCountQuery()
				: QueryUtils.createCountQueryFor(this.query.getQueryString()));
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
		return new StringQueryParameterBinder(getQueryMethod().getParameters(), values, query);
	}

	/**
	 * Creates an appropriate JPA query from an {@link EntityManager} according to the current {@link AbstractJpaQuery}
	 * type.
	 * 
	 * @param queryString
	 * @return
	 */
	public Query createJpaQuery(String queryString) {
		Query query = getEntityManager().createQuery(queryString);
		query = potentiallyAugment(query);
		return query;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
	 */
	@Override
	protected TypedQuery<Long> doCreateCountQuery(Object[] values) {
		return createBinder(values).bind(getEntityManager().createQuery(countQuery.getQueryString(), Long.class));
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

	protected Query potentiallyAugment(Query query) {
		return potentiallyAugment(query, QueryMode.FIND);
	}

	private Query potentiallyAugment(Query query, QueryMode mode) {

		QueryAugmentationEngine engine = getAugmentationEngine();

		if (engine != null
				&& engine.augmentationNeeded(JpaQueryContext.class, mode, getQueryMethod().getEntityInformation())) {
			JpaQueryContext context = new JpaQueryContext(mode, getEntityManager(), query);
			return engine.invokeAugmentors(context).getQuery();
		} else {
			return query;
		}
	}
}
