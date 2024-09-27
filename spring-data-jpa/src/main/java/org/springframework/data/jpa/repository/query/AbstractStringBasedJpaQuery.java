/*
 * Copyright 2008-2024 the original author or authors.
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
import org.springframework.data.domain.Sort;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
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

	private final DeclaredQuery query;
	private final Lazy<DeclaredQuery> countQuery;
	private final ValueExpressionDelegate valueExpressionDelegate;
	private final QueryParameterSetter.QueryMetadataCache metadataCache = new QueryParameterSetter.QueryMetadataCache();
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
	 * @param queryRewriter must not be {@literal null}.
	 * @param valueExpressionDelegate must not be {@literal null}.
	 */
	public AbstractStringBasedJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
			@Nullable String countQueryString, QueryRewriter queryRewriter,
			ValueExpressionDelegate valueExpressionDelegate) {

		super(method, em);

		Assert.hasText(queryString, "Query string must not be null or empty");
		Assert.notNull(valueExpressionDelegate, "ValueExpressionDelegate must not be null");
		Assert.notNull(queryRewriter, "QueryRewriter must not be null");

		this.valueExpressionDelegate = valueExpressionDelegate;
		this.valueExpressionContextProvider = valueExpressionDelegate.createValueContextProvider(method.getParameters());
		this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation(), valueExpressionDelegate,
				method.isNativeQuery());

		this.countQuery = Lazy.of(() -> {

			if (StringUtils.hasText(countQueryString)) {

				return new ExpressionBasedStringQuery(countQueryString, method.getEntityInformation(), valueExpressionDelegate,
						method.isNativeQuery());
			}

			return query.deriveCountQuery(method.getCountQueryProjection());
		});

		this.countParameterBinder = Lazy.of(() -> {
			return this.createBinder(this.countQuery.get());
		});

		this.queryRewriter = queryRewriter;

		JpaParameters parameters = method.getParameters();
		if (parameters.hasPageableParameter() || parameters.hasSortParameter()) {
			this.querySortRewriter = new CachingQuerySortRewriter();
		} else {
			this.querySortRewriter = NoOpQuerySortRewriter.INSTANCE;
		}

		Assert.isTrue(method.isNativeQuery() || !query.usesJdbcStyleParameters(),
				"JDBC style parameters (?) are not supported for JPA queries");
	}

	@Override
	public Query doCreateQuery(JpaParametersParameterAccessor accessor) {

		Sort sort = accessor.getSort();
		String sortedQueryString = getSortedQueryString(sort);

		ResultProcessor processor = getQueryMethod().getResultProcessor().withDynamicProjection(accessor);

		Query query = createJpaQuery(sortedQueryString, sort, accessor.getPageable(), processor.getReturnedType());

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata(sortedQueryString, query);

		// it is ok to reuse the binding contained in the ParameterBinder although we create a new query String because the
		// parameters in the query do not change.
		return parameterBinder.get().bindAndPrepare(query, metadata, accessor);
	}

	String getSortedQueryString(Sort sort) {
		return querySortRewriter.getSorted(query, sort);
	}

	@Override
	protected ParameterBinder createBinder() {
		return createBinder(query);
	}

	protected ParameterBinder createBinder(DeclaredQuery query) {
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

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata(queryString, query);

		countParameterBinder.get().bind(metadata.withQuery(query), accessor, QueryParameterSetter.ErrorHandling.LENIENT);

		return query;
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

		return QueryEnhancerFactory.forQuery(cachableQuery.getDeclaredQuery()).applySorting(cachableQuery.getSort(),
				cachableQuery.getAlias());
	}

	/**
	 * Query Sort Rewriter interface.
	 */
	interface QuerySortRewriter {
		String getSorted(DeclaredQuery query, Sort sort);
	}

	/**
	 * No-op query rewriter.
	 */
	enum NoOpQuerySortRewriter implements QuerySortRewriter {
		INSTANCE;

		public String getSorted(DeclaredQuery query, Sort sort) {

			if (sort.isSorted()) {
				throw new UnsupportedOperationException("NoOpQueryCache does not support sorting");
			}

			return query.getQueryString();
		}
	}

	/**
	 * Caching variant of {@link QuerySortRewriter}.
	 */
	class CachingQuerySortRewriter implements QuerySortRewriter {

		private final ConcurrentLruCache<CachableQuery, String> queryCache = new ConcurrentLruCache<>(16,
				AbstractStringBasedJpaQuery.this::applySorting);

		@Override
		public String getSorted(DeclaredQuery query, Sort sort) {

			if (sort.isUnsorted()) {
				return query.getQueryString();
			}

			return queryCache.get(new CachableQuery(query, sort));
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

		private final DeclaredQuery declaredQuery;
		private final String queryString;
		private final Sort sort;

		CachableQuery(DeclaredQuery query, Sort sort) {

			this.declaredQuery = query;
			this.queryString = query.getQueryString();
			this.sort = sort;
		}

		DeclaredQuery getDeclaredQuery() {
			return declaredQuery;
		}

		Sort getSort() {
			return sort;
		}

		@Nullable
		String getAlias() {
			return declaredQuery.getAlias();
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
