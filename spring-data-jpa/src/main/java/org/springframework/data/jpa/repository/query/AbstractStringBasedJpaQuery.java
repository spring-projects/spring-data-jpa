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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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
 */
abstract class AbstractStringBasedJpaQuery extends AbstractJpaQuery {

	private final DeclaredQuery query;
	private final DeclaredQuery countQuery;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private final SpelExpressionParser parser;
	private final QueryParameterSetter.QueryMetadataCache metadataCache = new QueryParameterSetter.QueryMetadataCache();
	private final QueryRewriter queryRewriter;

	/**
	 * Creates a new {@link AbstractStringBasedJpaQuery} from the given {@link JpaQueryMethod}, {@link EntityManager} and
	 * query {@link String}.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param queryString must not be {@literal null}.
	 * @param countQueryString must not be {@literal null}.
	 * @param evaluationContextProvider must not be {@literal null}.
	 * @param parser must not be {@literal null}.
	 * @param queryRewriter must not be {@literal null}.
	 */
	public AbstractStringBasedJpaQuery(JpaQueryMethod method, EntityManager em, String queryString,
			@Nullable String countQueryString, QueryRewriter queryRewriter, QueryMethodEvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser parser) {

		super(method, em);

		Assert.hasText(queryString, "Query string must not be null or empty");
		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null");
		Assert.notNull(parser, "Parser must not be null");
		Assert.notNull(queryRewriter, "QueryRewriter must not be null");

		this.evaluationContextProvider = evaluationContextProvider;
		this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation(), parser,
				method.isNativeQuery());

		DeclaredQuery countQuery = query.deriveCountQuery(countQueryString, method.getCountQueryProjection());
		this.countQuery = ExpressionBasedStringQuery.from(countQuery, method.getEntityInformation(), parser,
				method.isNativeQuery());

		this.parser = parser;
		this.queryRewriter = queryRewriter;

		Assert.isTrue(method.isNativeQuery() || !query.usesJdbcStyleParameters(),
				"JDBC style parameters (?) are not supported for JPA queries");
	}

	@Override
	public Query doCreateQuery(JpaParametersParameterAccessor accessor) {

		String sortedQueryString = QueryEnhancerFactory.forQuery(query) //
				.applySorting(accessor.getSort(), query.getAlias());
		ResultProcessor processor = getQueryMethod().getResultProcessor().withDynamicProjection(accessor);

		Query query = createJpaQuery(sortedQueryString, accessor.getSort(), accessor.getPageable(),
				processor.getReturnedType());

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata(sortedQueryString, query);

		// it is ok to reuse the binding contained in the ParameterBinder although we create a new query String because the
		// parameters in the query do not change.
		return parameterBinder.get().bindAndPrepare(query, metadata, accessor);
	}

	@Override
	protected ParameterBinder createBinder() {

		return ParameterBinderFactory.createQueryAwareBinder(getQueryMethod().getParameters(), query, parser,
				evaluationContextProvider);
	}

	@Override
	protected Query doCreateCountQuery(JpaParametersParameterAccessor accessor) {

		String queryString = countQuery.getQueryString();
		EntityManager em = getEntityManager();

		Query query = getQueryMethod().isNativeQuery() //
				? em.createNativeQuery(queryString) //
				: em.createQuery(queryString, Long.class);

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata(queryString, query);

		parameterBinder.get().bind(metadata.withQuery(query), accessor, QueryParameterSetter.ErrorHandling.LENIENT);

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
		return countQuery;
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
}
