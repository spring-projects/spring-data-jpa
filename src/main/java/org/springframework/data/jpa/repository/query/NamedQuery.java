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
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link RepositoryQuery} based on {@link javax.persistence.NamedQuery}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
final class NamedQuery extends AbstractJpaQuery {

	private static final String CANNOT_EXTRACT_QUERY = "Your persistence provider does not support extracting the JPQL query from a "
			+ "named query thus you can't use Pageable inside your query method. Make sure you "
			+ "have a JpaDialect configured at your EntityManagerFactoryBean as this affects "
			+ "discovering the concrete persistence provider.";

	private static final Logger LOG = LoggerFactory.getLogger(NamedQuery.class);

	private final String queryName;
	private final String countQueryName;
	private final @Nullable String countProjection;
	private final QueryExtractor extractor;
	private final boolean namedCountQueryIsPresent;

	/**
	 * Creates a new {@link NamedQuery}.
	 */
	private NamedQuery(JpaQueryMethod method, EntityManager em) {

		super(method, em);

		this.queryName = method.getNamedQueryName();
		this.countQueryName = method.getNamedCountQueryName();
		this.extractor = method.getQueryExtractor();
		this.countProjection = method.getCountQueryProjection();

		Parameters<?, ?> parameters = method.getParameters();

		if (parameters.hasSortParameter()) {
			throw new IllegalStateException(String.format("Finder method %s is backed " + "by a NamedQuery and must "
					+ "not contain a sort parameter as we cannot modify the query! Use @Query instead!", method));
		}

		this.namedCountQueryIsPresent = hasNamedQuery(em, countQueryName);

		boolean weNeedToCreateCountQuery = !namedCountQueryIsPresent && method.getParameters().hasPageableParameter();
		boolean cantExtractQuery = !this.extractor.canExtractQuery();

		if (weNeedToCreateCountQuery && cantExtractQuery) {
			throw QueryCreationException.create(method, CANNOT_EXTRACT_QUERY);
		}

		if (parameters.hasPageableParameter()) {
			LOG.warn("Finder method {} is backed by a NamedQuery" + " but contains a Pageable parameter! Sorting delivered "
					+ "via this Pageable will not be applied!", method);
		}
	}

	/**
	 * Returns whether the named query with the given name exists.
	 *
	 * @param em must not be {@literal null}.
	 * @param queryName must not be {@literal null}.
	 * @return
	 */
	private static boolean hasNamedQuery(EntityManager em, String queryName) {

		/*
		 * See DATAJPA-617, we have to use a dedicated em for the lookups to avoid a
		 * potential rollback of the running tx.
		 */
		EntityManager lookupEm = em.getEntityManagerFactory().createEntityManager();

		try {
			lookupEm.createNamedQuery(queryName);
			return true;
		} catch (IllegalArgumentException e) {
			LOG.debug("Did not find named query {}", queryName);
			return false;
		} finally {
			lookupEm.close();
		}
	}

	/**
	 * Looks up a named query for the given {@link org.springframework.data.repository.query.QueryMethod}.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @return
	 */
	@Nullable
	public static RepositoryQuery lookupFrom(JpaQueryMethod method, EntityManager em) {

		final String queryName = method.getNamedQueryName();

		LOG.debug("Looking up named query {}", queryName);

		if (!hasNamedQuery(em, queryName)) {
			return null;
		}

		try {
			RepositoryQuery query = new NamedQuery(method, em);
			LOG.debug("Found named query {}!", queryName);
			return query;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateQuery(java.lang.Object[])
	 */
	@Override
	protected Query doCreateQuery(Object[] values) {

		EntityManager em = getEntityManager();

		Query query = getTypeToRead() //
				.<Query> map(it -> em.createNamedQuery(queryName, it)) //
				.orElseGet(() -> em.createNamedQuery(queryName));

		return parameterBinder.get().bindAndPrepare(query, values);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
	 */
	@Override
	protected TypedQuery<Long> doCreateCountQuery(Object[] values) {

		EntityManager em = getEntityManager();
		TypedQuery<Long> countQuery;

		if (namedCountQueryIsPresent) {
			countQuery = em.createNamedQuery(countQueryName, Long.class);
		} else {
			Query query = createQuery(values);
			String queryString = extractor.extractQueryString(query);

			if (queryString == null) {
				throw new IllegalStateException(String.format("Cannot extract query string for query %s is null!", query));
			}

			countQuery = em.createQuery(QueryUtils.createCountQueryFor(queryString, countProjection), Long.class);
		}

		return parameterBinder.get().bind(countQuery, values, LENIENT);
	}
}
