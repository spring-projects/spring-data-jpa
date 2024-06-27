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

import java.util.Objects;

import org.springframework.data.domain.Pageable;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.jpa.repository.QueryRewriter;
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

	private final StringQuery query;
	private final Lazy<IntrospectedQuery> countQuery;
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
	 * @param countQueryString must not be {@literal null}.
	 * @param queryConfiguration must not be {@literal null}.
	 */
	public AbstractStringBasedJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
			@Nullable String countQueryString, JpaQueryConfiguration queryConfiguration) {

		super(method, em);

		Assert.hasText(queryString, "Query string must not be null or empty");
		Assert.notNull(queryConfiguration, "JpaQueryConfiguration must not be null");

		this.valueExpressionDelegate = queryConfiguration.getValueExpressionDelegate();
		this.valueExpressionContextProvider = valueExpressionDelegate.createValueContextProvider(method.getParameters());
		this.query = ExpressionBasedStringQuery.create(queryString, method, queryConfiguration);

		this.countQuery = Lazy.of(() -> {

			if (StringUtils.hasText(countQueryString)) {
				return ExpressionBasedStringQuery.create(countQueryString, method, queryConfiguration);
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

	@Override
	public Query doCreateQuery(JpaParametersParameterAccessor accessor) {

		Sort sort = accessor.getSort();
		ResultProcessor processor = getQueryMethod().getResultProcessor().withDynamicProjection(accessor);
		ReturnedType returnedType = processor.getReturnedType();
		String sortedQueryString = getSortedQueryString(sort, returnedType);
		Query query = createJpaQuery(sortedQueryString, sort, accessor.getPageable(), returnedType);

		// it is ok to reuse the binding contained in the ParameterBinder, although we create a new query String because the
		// parameters in the query do not change.
		return parameterBinder.get().bindAndPrepare(query, accessor);
	}

	String getSortedQueryString(Sort sort, ReturnedType returnedType) {
		return querySortRewriter.getSorted(query, sort, returnedType);
	}

	@Override
	protected ParameterBinder createBinder() {
		return createBinder(query);
	}

	protected ParameterBinder createBinder(IntrospectedQuery query) {
		return ParameterBinderFactory.createQueryAwareBinder(getQueryMethod().getParameters(), query,
				valueExpressionDelegate, valueExpressionContextProvider);
	}

	@Override
	protected Query doCreateCountQuery(JpaParametersParameterAccessor accessor) {

		String queryString = countQuery.get().getQueryString();
		EntityManager em = getEntityManager();

		Query query = getQueryMethod().isNativeQuery() //
				? em.createNativeQuery(queryString) //
				: em.createQuery(queryString, Long.class);

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
	public IntrospectedQuery getCountQuery() {
		return countQuery.get();
	}

	/**
	 * Creates an appropriate JPA query from an {@link EntityManager} according to the current {@link AbstractJpaQuery}
	 * type.
	 */
	protected Query createJpaQuery(String queryString, Sort sort, @Nullable Pageable pageable,
			ReturnedType returnedType) {

		EntityManager em = getEntityManager();

		if (this.query.hasConstructorExpression() || this.query.isDefaultProjection()) {
			return em.createQuery(potentiallyRewriteQuery(queryString, sort, pageable));
		}

		Class<?> typeToRead = getTypeToRead(returnedType);

		return typeToRead == null //
				? em.createQuery(potentiallyRewriteQuery(queryString, sort, pageable)) //
				: em.createQuery(potentiallyRewriteQuery(queryString, sort, pageable), typeToRead);
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

	String applySorting(CachableQuery cachableQuery) {
		return cachableQuery.getDeclaredQuery().getQueryEnhancer()
				.rewrite(new DefaultQueryRewriteInformation(cachableQuery.getSort(), cachableQuery.getReturnedType()));
	}

	/**
	 * Query Sort Rewriter interface.
	 */
	interface QuerySortRewriter {
		String getSorted(StringQuery query, Sort sort, ReturnedType returnedType);
	}

	/**
	 * No-op query rewriter.
	 */
	enum SimpleQuerySortRewriter implements QuerySortRewriter {

		INSTANCE;

		public String getSorted(StringQuery query, Sort sort, ReturnedType returnedType) {
			return query.getQueryEnhancer().rewrite(new DefaultQueryRewriteInformation(sort, returnedType));
		}
	}

	static class UnsortedCachingQuerySortRewriter implements QuerySortRewriter {

		private volatile @Nullable String cachedQueryString;

		public String getSorted(StringQuery query, Sort sort, ReturnedType returnedType) {

			if (sort.isSorted()) {
				throw new UnsupportedOperationException("NoOpQueryCache does not support sorting");
			}

			String cachedQueryString = this.cachedQueryString;
			if (cachedQueryString == null) {
				this.cachedQueryString = cachedQueryString = query.getQueryEnhancer()
						.rewrite(new DefaultQueryRewriteInformation(sort, returnedType));
			}

			return cachedQueryString;
		}
	}

	/**
	 * Caching variant of {@link QuerySortRewriter}.
	 */
	class CachingQuerySortRewriter implements QuerySortRewriter {

		private final ConcurrentLruCache<CachableQuery, String> queryCache = new ConcurrentLruCache<>(16,
				AbstractStringBasedJpaQuery.this::applySorting);

		private volatile @Nullable String cachedQueryString;

		@Override
		public String getSorted(StringQuery query, Sort sort, ReturnedType returnedType) {

			if (sort.isUnsorted()) {

				String cachedQueryString = this.cachedQueryString;
				if (cachedQueryString == null) {
					this.cachedQueryString = cachedQueryString = queryCache.get(new CachableQuery(query, sort, returnedType));
				}

				return cachedQueryString;
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

		private final StringQuery query;
		private final String queryString;
		private final Sort sort;
		private final ReturnedType returnedType;

		CachableQuery(StringQuery query, Sort sort, ReturnedType returnedType) {

			this.query = query;
			this.queryString = query.getQueryString();
			this.sort = sort;
			this.returnedType = returnedType;
		}

		StringQuery getDeclaredQuery() {
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
}
