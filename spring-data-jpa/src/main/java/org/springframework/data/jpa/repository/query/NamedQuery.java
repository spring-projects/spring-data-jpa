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
package org.springframework.data.jpa.repository.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link RepositoryQuery} based on {@link jakarta.persistence.NamedQuery}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
final class NamedQuery extends AbstractJpaQuery {

	private static final String CANNOT_EXTRACT_QUERY = "Your persistence provider does not support extracting the JPQL query from a "
			+ "named query thus you can't use Pageable inside your query method; Make sure you "
			+ "have a JpaDialect configured at your EntityManagerFactoryBean as this affects "
			+ "discovering the concrete persistence provider";

	private static final Log LOG = LogFactory.getLog(NamedQuery.class);

	private final String queryName;
	private final String countQueryName;
	private final @Nullable String countProjection;
	private final QueryExtractor extractor;
	private final boolean namedCountQueryIsPresent;
	private final DeclaredQuery declaredQuery;
	private final QueryParameterSetter.QueryMetadataCache metadataCache;

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
			throw new IllegalStateException(String.format("Finder method %s is backed by a NamedQuery and must "
					+ "not contain a sort parameter as we cannot modify the query; Use @Query instead", method));
		}

		this.namedCountQueryIsPresent = hasNamedQuery(em, countQueryName);

		Query query = em.createNamedQuery(queryName);
		String queryString = extractor.extractQueryString(query);

		this.declaredQuery = DeclaredQuery.of(queryString, false);

		boolean weNeedToCreateCountQuery = !namedCountQueryIsPresent && method.getParameters().hasPageableParameter();
		boolean cantExtractQuery = !this.extractor.canExtractQuery();

		if (weNeedToCreateCountQuery && cantExtractQuery) {
			throw QueryCreationException.create(method, CANNOT_EXTRACT_QUERY);
		}

		if (parameters.hasPageableParameter()) {
			LOG.warn(String.format(
					"Finder method %s is backed by a NamedQuery but contains a Pageable parameter; Sorting delivered via this Pageable will not be applied",
					method));
		}

		this.metadataCache = new QueryParameterSetter.QueryMetadataCache();
	}

	/**
	 * Returns whether the named query with the given name exists.
	 *
	 * @param em must not be {@literal null}.
	 * @param queryName must not be {@literal null}.
	 * @return
	 */
	static boolean hasNamedQuery(EntityManager em, String queryName) {

		/*
		 * See DATAJPA-617, we have to use a dedicated em for the lookups to avoid a
		 * potential rollback of the running tx.
		 */
		EntityManager lookupEm = em.getEntityManagerFactory().createEntityManager();

		try {
			lookupEm.createNamedQuery(queryName);
			return true;
		} catch (IllegalArgumentException e) {

			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("Did not find named query %s", queryName));
			}
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

		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("Looking up named query %s", queryName));
		}

		if (!hasNamedQuery(em, queryName)) {
			return null;
		}

		try {

			RepositoryQuery query = new NamedQuery(method, em);
			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("Found named query %s", queryName));
			}
			return query;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	protected Query doCreateQuery(JpaParametersParameterAccessor accessor) {

		EntityManager em = getEntityManager();

		JpaQueryMethod queryMethod = getQueryMethod();
		ResultProcessor processor = queryMethod.getResultProcessor().withDynamicProjection(accessor);

		Class<?> typeToRead = getTypeToRead(processor.getReturnedType());

		Query query = typeToRead == null //
				? em.createNamedQuery(queryName) //
				: em.createNamedQuery(queryName, typeToRead);

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata(queryName, query);

		return parameterBinder.get().bindAndPrepare(query, metadata, accessor);
	}

	@Override
	protected TypedQuery<Long> doCreateCountQuery(JpaParametersParameterAccessor accessor) {

		EntityManager em = getEntityManager();
		TypedQuery<Long> countQuery;

		String cacheKey;
		if (namedCountQueryIsPresent) {
			cacheKey = countQueryName;
			countQuery = em.createNamedQuery(countQueryName, Long.class);

		} else {

			String countQueryString = declaredQuery.deriveCountQuery(null, countProjection).getQueryString();
			cacheKey = countQueryString;
			countQuery = em.createQuery(countQueryString, Long.class);
		}

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata(cacheKey, countQuery);

		return parameterBinder.get().bind(countQuery, metadata, accessor);
	}

	@Override
	protected Class<?> getTypeToRead(ReturnedType returnedType) {

		if (getQueryMethod().isNativeQuery()) {

			Class<?> type = returnedType.getReturnedType();
			Class<?> domainType = returnedType.getDomainType();

			// Domain or subtype -> use return type
			if (domainType.isAssignableFrom(type)) {
				return type;
			}

			// Domain type supertype -> use domain type
			if (type.isAssignableFrom(domainType)) {
				return domainType;
			}

			// Tuples for projection interfaces or explicit SQL mappings for everything else
			return type.isInterface() ? Tuple.class : null;
		}

		return declaredQuery.hasConstructorExpression() //
				? null //
				: super.getTypeToRead(returnedType);
	}
}
