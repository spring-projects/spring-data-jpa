/*
 * Copyright 2008-2025 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link String} based JPA queries.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Tom Hombergs
 * @author David Madden
 * @author Mark Paluch
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Christoph Strobl
 */
abstract class AbstractStringBasedJpaQuery extends AbstractJpaQuery {

	private final EntityQuery query;
	private final Map<Class<?>, Boolean> knownProjections = new ConcurrentHashMap<>();
	private final Lazy<ParametrizedQuery> countQuery;
	private final ValueExpressionDelegate valueExpressionDelegate;
	private final QueryRewriter queryRewriter;
	private final QuerySortRewriter querySortRewriter;
	private final Lazy<ParameterBinder> countParameterBinder;
	private final ValueEvaluationContextProvider valueExpressionContextProvider;

	/**
	 * Creates a new {@link AbstractStringBasedJpaQuery} from the given {@link JpaQueryMethod}, {@link EntityManager} and
	 * query {@link String}.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param queryString must not be {@literal null}.
	 * @param countQuery can be {@literal null} if not defined.
	 * @param queryConfiguration must not be {@literal null}.
	 */
	AbstractStringBasedJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
			@Nullable String countQueryString, JpaQueryConfiguration queryConfiguration) {
		this(method, em, method.getDeclaredQuery(queryString),
				countQueryString != null ? method.getDeclaredQuery(countQueryString) : null, queryConfiguration);
	}

	/**
	 * Creates a new {@link AbstractStringBasedJpaQuery} from the given {@link JpaQueryMethod}, {@link EntityManager} and
	 * query {@link String}.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param countQuery can be {@literal null}.
	 * @param queryConfiguration must not be {@literal null}.
	 */
	public AbstractStringBasedJpaQuery(JpaQueryMethod method, EntityManager em, DeclaredQuery query,
			@Nullable DeclaredQuery countQuery, JpaQueryConfiguration queryConfiguration) {

		super(method, em);

		Assert.notNull(query, "Query must not be null");
		Assert.notNull(queryConfiguration, "JpaQueryConfiguration must not be null");

		this.valueExpressionDelegate = queryConfiguration.getValueExpressionDelegate();
		this.valueExpressionContextProvider = valueExpressionDelegate.createValueContextProvider(method.getParameters());

		this.query = TemplatedQuery.create(query, method.getEntityInformation(), queryConfiguration);

		this.countQuery = Lazy.of(() -> {

			if (countQuery != null) {
				return TemplatedQuery.create(countQuery, method.getEntityInformation(), queryConfiguration);
			}

			return this.query.deriveCountQuery(method.getCountQueryProjection());
		});

		this.countParameterBinder = Lazy.of(() -> this.createBinder(this.countQuery.get()));

		this.queryRewriter = queryConfiguration.getQueryRewriter(method);

		JpaParameters parameters = method.getParameters();

		if (parameters.hasDynamicProjection()) {
			this.querySortRewriter = SimpleQuerySortRewriter.INSTANCE;
		} else {
			if (parameters.hasPageableParameter() || parameters.hasSortParameter()) {
				this.querySortRewriter = new CachingQuerySortRewriter();
			} else {
				this.querySortRewriter = new UnsortedCachingQuerySortRewriter();
			}
		}

		Assert.isTrue(method.isNativeQuery() || !this.query.usesJdbcStyleParameters(),
				"JDBC style parameters (?) are not supported for JPA queries");
	}

	private DeclaredQuery createQuery(String queryString, boolean nativeQuery) {
		return nativeQuery ? DeclaredQuery.nativeQuery(queryString) : DeclaredQuery.jpqlQuery(queryString);
	}

	@Override
	public Query doCreateQuery(JpaParametersParameterAccessor accessor) {

		Sort sort = accessor.getSort();
		ResultProcessor processor = getQueryMethod().getResultProcessor().withDynamicProjection(accessor);
		ReturnedType returnedType = getReturnedType(processor);
		QueryProvider sortedQuery = getSortedQuery(sort, returnedType);
		Query query = createJpaQuery(sortedQuery, sort, accessor.getPageable(), returnedType);

		// it is ok to reuse the binding contained in the ParameterBinder, although we create a new query String because the
		// parameters in the query do not change.
		return parameterBinder.get().bindAndPrepare(query, accessor);
	}

	/**
	 * Post-process {@link ReturnedType} to determine if the query is projecting by checking the projection and property
	 * assignability.
	 *
	 * @param processor
	 * @return
	 */
	private ReturnedType getReturnedType(ResultProcessor processor) {

		ReturnedType returnedType = processor.getReturnedType();
		Class<?> returnedJavaType = processor.getReturnedType().getReturnedType();

		if (query.isDefaultProjection() || !returnedType.isProjecting() || returnedJavaType.isInterface()
				|| query.isNativeQuery()) {
			return returnedType;
		}

		Boolean known = knownProjections.get(returnedJavaType);

		if (known != null && known) {
			return returnedType;
		}

		if ((known != null && !known) || returnedJavaType.isArray()) {
			if (known == null) {
				knownProjections.put(returnedJavaType, false);
			}
			return new NonProjectingReturnedType(returnedType);
		}

		String alias = query.getAlias();
		String projection = query.getProjection();

		// we can handle single-column and no function projections here only
		if (StringUtils.hasText(projection) && (projection.indexOf(',') != -1 || projection.indexOf('(') != -1)) {
			return returnedType;
		}

		if (StringUtils.hasText(alias) && StringUtils.hasText(projection)) {
			alias = alias.trim();
			projection = projection.trim();
			if (projection.startsWith(alias + ".")) {
				projection = projection.substring(alias.length() + 1);
			}
		}

		if (StringUtils.hasText(projection)) {

			int space = projection.indexOf(' ');

			if (space != -1) {
				projection = projection.substring(0, space);
			}

			Class<?> propertyType;

			try {
				PropertyPath from = PropertyPath.from(projection, getQueryMethod().getEntityInformation().getJavaType());
				propertyType = from.getLeafType();
			} catch (PropertyReferenceException ignored) {
				propertyType = null;
			}

			if (propertyType == null
					|| (returnedJavaType.isAssignableFrom(propertyType) || propertyType.isAssignableFrom(returnedJavaType))) {
				knownProjections.put(returnedJavaType, false);
				return new NonProjectingReturnedType(returnedType);
			} else {
				knownProjections.put(returnedJavaType, true);
			}
		}

		return returnedType;
	}

	String getSortedQueryString(Sort sort, ReturnedType returnedType) {
		return querySortRewriter.getSorted(query, sort, returnedType);
	}

	@Override
	protected ParameterBinder createBinder() {
		return createBinder(query);
	}

	protected ParameterBinder createBinder(ParametrizedQuery query) {
		return ParameterBinderFactory.createQueryAwareBinder(getQueryMethod().getParameters(), query,
				valueExpressionDelegate, valueExpressionContextProvider);
	}

	@Override
	protected Query doCreateCountQuery(JpaParametersParameterAccessor accessor) {

		String queryString = countQuery.get().getQueryString();
		EntityManager em = getEntityManager();

		String queryStringToUse = potentiallyRewriteQuery(queryString, accessor.getSort(), accessor.getPageable());

		Query query = getQueryMethod().isNativeQuery() //
				? em.createNativeQuery(queryStringToUse) //
				: em.createQuery(queryStringToUse, Long.class);

		countParameterBinder.get().bind(new QueryParameterSetter.BindableQuery(query), accessor,
				QueryParameterSetter.ErrorHandling.LENIENT);

		return query;
	}

	/**
	 * @return the query
	 */
	public EntityQuery getQuery() {
		return query;
	}

	/**
	 * @return the countQuery
	 */
	public ParametrizedQuery getCountQuery() {
		return countQuery.get();
	}

	/**
	 * Creates an appropriate JPA query from an {@link EntityManager} according to the current {@link AbstractJpaQuery}
	 * type.
	 */
	protected Query createJpaQuery(QueryProvider query, Sort sort, @Nullable Pageable pageable,
			ReturnedType returnedType) {

		EntityManager em = getEntityManager();
		String queryToUse = potentiallyRewriteQuery(query.getQueryString(), sort, pageable);

		if (this.query.hasConstructorExpression() || this.query.isDefaultProjection()) {
			return em.createQuery(queryToUse);
		}

		Class<?> typeToRead = getTypeToRead(returnedType);

		return typeToRead == null //
				? em.createQuery(queryToUse) //
				: em.createQuery(queryToUse, typeToRead);
	}

	/**
	 * Use the {@link QueryRewriter}, potentially rewrite the query, using relevant {@link Sort} and {@link Pageable}
	 * information.
	 *
	 * @param originalQuery
	 * @param sort
	 * @param pageable
	 * @return
	 */
	protected String potentiallyRewriteQuery(String originalQuery, Sort sort, @Nullable Pageable pageable) {

		return pageable != null && pageable.isPaged() //
				? queryRewriter.rewrite(originalQuery, pageable) //
				: queryRewriter.rewrite(originalQuery, sort);
	}

	QueryProvider applySorting(CachableQuery cachableQuery) {
		return cachableQuery.getDeclaredQuery()
				.rewrite(new DefaultQueryRewriteInformation(cachableQuery.getSort(), cachableQuery.getReturnedType()));
	}

	/**
	 * Query Sort Rewriter interface.
	 */
	interface QuerySortRewriter {
		QueryProvider getSorted(EntityQuery query, Sort sort, ReturnedType returnedType);
	}

	/**
	 * No-op query rewriter.
	 */
	enum SimpleQuerySortRewriter implements QuerySortRewriter {

		INSTANCE;

		public QueryProvider getSorted(EntityQuery query, Sort sort, ReturnedType returnedType) {
			return query.rewrite(new DefaultQueryRewriteInformation(sort, returnedType));
		}
	}

	static class UnsortedCachingQuerySortRewriter implements QuerySortRewriter {

		private volatile @Nullable QueryProvider cachedQuery;

		public QueryProvider getSorted(EntityQuery query, Sort sort, ReturnedType returnedType) {

			if (sort.isSorted()) {
				throw new UnsupportedOperationException("NoOpQueryCache does not support sorting");
			}

			QueryProvider cachedQuery = this.cachedQuery;
			if (cachedQuery == null) {
				this.cachedQuery = cachedQuery = query
						.rewrite(new DefaultQueryRewriteInformation(sort, returnedType));
			}

			return cachedQuery;
		}
	}

	/**
	 * Caching variant of {@link QuerySortRewriter}.
	 */
	class CachingQuerySortRewriter implements QuerySortRewriter {

		private final ConcurrentLruCache<CachableQuery, QueryProvider> queryCache = new ConcurrentLruCache<>(16,
				AbstractStringBasedJpaQuery.this::applySorting);

		private volatile @Nullable QueryProvider cachedQuery;

		@Override
		public QueryProvider getSorted(EntityQuery query, Sort sort, ReturnedType returnedType) {

			if (sort.isUnsorted()) {

				QueryProvider cachedQuery = this.cachedQuery;
				if (cachedQuery == null) {
					this.cachedQuery = cachedQuery = queryCache.get(new CachableQuery(query, sort, returnedType));
				}

				return cachedQuery;
			}

			return queryCache.get(new CachableQuery(query, sort, returnedType));
		}
	}

	/**
	 * Value object with optimized {@link Object#equals(Object)} to cache a query based on its query string and
	 * {@link Sort sorting}.
	 *
	 * @since 3.2.3
	 * @author Christoph Strobl
	 */
	static class CachableQuery {

		private final EntityQuery query;
		private final String queryString;
		private final Sort sort;
		private final ReturnedType returnedType;

		CachableQuery(EntityQuery query, Sort sort, ReturnedType returnedType) {

			this.query = query;
			this.queryString = query.getQueryString();
			this.sort = sort;
			this.returnedType = returnedType;
		}

		EntityQuery getDeclaredQuery() {
			return query;
		}

		Sort getSort() {
			return sort;
		}

		public ReturnedType getReturnedType() {
			return returnedType;
		}

		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			CachableQuery that = (CachableQuery) o;

			if (!Objects.equals(queryString, that.queryString)) {
				return false;
			}
			return Objects.equals(sort, that.sort);
		}

		@Override
		public int hashCode() {

			int result = queryString != null ? queryString.hashCode() : 0;
			result = 31 * result + (sort != null ? sort.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Non-projecting {@link ReturnedType} wrapper that delegates to the original {@link ReturnedType} but always returns
	 * {@code false} for {@link #isProjecting()}. This type is to indicate that this query is not projecting, even if the
	 * original {@link ReturnedType} was because we e.g. select a nested property and do not want DTO constructor
	 * expression rewriting to kick in.
	 */
	private static class NonProjectingReturnedType extends ReturnedType {

		private final ReturnedType delegate;

		NonProjectingReturnedType(ReturnedType delegate) {
			super(delegate.getDomainType());
			this.delegate = delegate;
		}

		@Override
		public boolean isProjecting() {
			return false;
		}

		@Override
		public Class<?> getReturnedType() {
			return delegate.getReturnedType();
		}

		@Override
		public boolean needsCustomConstruction() {
			return false;
		}

		@Override
		@Nullable
		public Class<?> getTypeToRead() {
			return delegate.getTypeToRead();
		}

		@Override
		public List<String> getInputProperties() {
			return delegate.getInputProperties();
		}
	}
}
