/*
 * Copyright 2023 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.ExpressionBasedStringQuery.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

/**
 * An {@link AbstractJpaQueryContext} used to handle @{@link org.springframework.data.jpa.repository.Query}-based
 * queries.
 *
 * @author Greg Turnquist
 */
class AnnotationBasedQueryContext extends AbstractJpaQueryContext {

	private final SpelExpressionParser parser;
	private final QueryMethodEvaluationContextProvider evaluationContextProvider;
	private final List<ParameterBinding> bindings;
	private final DeclaredQuery declaredQuery;
	@Nullable private final String queryString;
	private final String countQueryString;
	private final boolean nativeQuery;
	private final QueryRewriter queryRewriter;

	private static final String EXPRESSION_PARAMETER = "$1#{";
	private static final String QUOTED_EXPRESSION_PARAMETER = "$1__HASH__{";
	private static final Pattern EXPRESSION_PARAMETER_QUOTING = Pattern.compile("([:?])#\\{");
	private static final Pattern EXPRESSION_PARAMETER_UNQUOTING = Pattern.compile("([:?])__HASH__\\{");
	private static final String ENTITY_NAME_VARIABLE_EXPRESSION = "#{#entityName";

	AnnotationBasedQueryContext(JpaQueryMethod method, EntityManager entityManager, String queryString,
			@Nullable String countQueryString, QueryMethodEvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser parser, QueryRewriter queryRewriter) {

		super(Optional.of(method), entityManager);

		this.parser = parser;
		this.evaluationContextProvider = evaluationContextProvider;
		this.bindings = new ArrayList<>();

		this.declaredQuery = DeclaredQuery.of(queryString, method.isNativeQuery());
		this.countQueryString = countQueryString;
		this.queryString = renderQuery(queryString);
		this.nativeQuery = method.isNativeQuery();
		this.queryRewriter = queryRewriter;

		validateQueries();
	}

	public ContextualQuery getQueryString() {
		return ContextualQuery.of(this.queryString);
	}

	public String getCountQueryString() {

		return queryMethod().getCountQuery() != null //
				? queryMethod().getCountQuery() //
				: countQueryString;
	}

	public boolean isNativeQuery() {
		return nativeQuery;
	}

	@Override
	protected ContextualQuery createQuery(JpaParametersParameterAccessor accessor) {
		return ContextualQuery.of(queryString);
	}

	@Override
	protected ContextualQuery postProcessQuery(ContextualQuery query, JpaParametersParameterAccessor accessor) {

		DeclaredQuery declaredQuery = DeclaredQuery.of(query.getQuery(), nativeQuery);

		String queryString = QueryEnhancerFactory.forQuery(declaredQuery) //
				.applySorting(accessor.getSort(), declaredQuery.getAlias());

		return query.alterQuery(queryString);
	}

	@Override
	protected Query turnIntoJpaQuery(ContextualQuery query, JpaParametersParameterAccessor accessor) {

		ResultProcessor processor = queryMethod().getResultProcessor().withDynamicProjection(accessor);

		ReturnedType returnedType = processor.getReturnedType();
		Class<?> typeToRead = getTypeToRead(returnedType);

		String potentiallyRewrittenQuery = potentiallyRewriteQuery(query.getQuery(), accessor);

		if (typeToRead == null) {
			return nativeQuery //
					? getEntityManager().createNativeQuery(potentiallyRewrittenQuery) //
					: getEntityManager().createQuery(potentiallyRewrittenQuery);
		}

		return nativeQuery //
				? getEntityManager().createNativeQuery(potentiallyRewrittenQuery, typeToRead) //
				: getEntityManager().createQuery(potentiallyRewrittenQuery, typeToRead);
	}

	@Override
	protected Class<?> getTypeToRead(ReturnedType returnedType) {

		if (!nativeQuery) {
			return super.getTypeToRead(returnedType);
		}

		Class<?> result = queryMethod().isQueryForEntity() ? returnedType.getDomainType() : null;

		if (declaredQuery.hasConstructorExpression() || declaredQuery.isDefaultProjection()) {
			return result;
		}

		return returnedType.isProjecting() && !getMetamodel().isJpaManaged(returnedType.getReturnedType()) //
				? Tuple.class
				: result;
	}

	@Override
	public Query createJpaCountQuery(JpaParametersParameterAccessor accessor) {

		Query query = queryMethod().isNativeQuery() //
				? getEntityManager().createNativeQuery(getCountQueryString()) //
				: getEntityManager().createQuery(getCountQueryString(), Long.class);

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata(queryString, query);

		parameterBinder.get().bind(metadata.withQuery(query), accessor, QueryParameterSetter.ErrorHandling.LENIENT);

		return query;
	}

	@Override
	protected ParameterBinder createBinder() {
		return ParameterBinderFactory.createQueryAwareBinder(queryMethod().getParameters(), declaredQuery, parser,
				evaluationContextProvider);
	}

	@Override
	protected Query bindParameters(Query query, JpaParametersParameterAccessor accessor) {

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata(queryString, query);

		return parameterBinder.get().bindAndPrepare(query, metadata, accessor);
	}

	// Internals

	private String renderQuery(String initialQuery) {

		if (!containsExpression(initialQuery)) {
			return ParameterBindingParser.INSTANCE
					.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(initialQuery, this.bindings, new Metadata());
		}

		StandardEvaluationContext evalContext = new StandardEvaluationContext();
		evalContext.setVariable("entityName", queryMethod().getEntityInformation().getEntityName());

		String potentiallyQuotedQueryString = potentiallyQuoteExpressionsParameter(initialQuery);

		Expression expr = parser.parseExpression(potentiallyQuotedQueryString, ParserContext.TEMPLATE_EXPRESSION);

		String result = expr.getValue(evalContext, String.class);

		String processedQuery = result == null //
				? potentiallyQuotedQueryString //
				: potentiallyUnquoteParameterExpressions(result);

		return ParameterBindingParser.INSTANCE
				.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(processedQuery, this.bindings, new Metadata());
	}

	private void validateQueries() {

		if (nativeQuery) {

			Parameters<?, ?> parameters = queryMethod().getParameters();

			if (parameters.hasSortParameter() && !queryString.contains("#sort")) {
				throw new InvalidJpaQueryMethodException(
						"Cannot use native queries with dynamic sorting in method " + getQueryMethod());
			}
		}

		validateJpaQuery(queryString, String.format("Validation failed for query with method %s", getQueryMethod()));

		if (queryMethod().isPageQuery() && getCountQueryString() != null) {
			validateJpaQuery(getCountQueryString(),
					String.format("Count query validation failed for method %s", queryMethod()));
		}
	}

	private void validateJpaQuery(@Nullable String query, String errorMessage) {

		if (queryMethod().isProcedureQuery()) {
			return;
		}

		try (EntityManager validatingEm = getEntityManager().getEntityManagerFactory().createEntityManager()) {

			if (nativeQuery) {
				validatingEm.createNativeQuery(query);
			} else {
				validatingEm.createQuery(query);
			}
		} catch (RuntimeException ex) {

			// Needed as there's ambiguities in how an invalid query string shall be expressed by the persistence provider
			// https://java.net/projects/jpa-spec/lists/jsr338-experts/archive/2012-07/message/17
			throw new IllegalArgumentException(errorMessage, ex);
		}
	}

	/**
	 * Use the {@link QueryRewriter}, potentially rewrite the query, using relevant {@link Sort} and {@link Pageable}
	 * information.
	 *
	 * @param originalQuery
	 * @param accessor
	 * @return
	 */
	private String potentiallyRewriteQuery(String originalQuery, JpaParametersParameterAccessor accessor) {

		Sort sort = accessor.getSort();
		Pageable pageable = accessor.getPageable();

		return pageable != null && pageable.isPaged() //
				? queryRewriter.rewrite(originalQuery, pageable) //
				: queryRewriter.rewrite(originalQuery, sort);
	}

	private static String potentiallyUnquoteParameterExpressions(String result) {
		return EXPRESSION_PARAMETER_UNQUOTING.matcher(result).replaceAll(EXPRESSION_PARAMETER);
	}

	private static String potentiallyQuoteExpressionsParameter(String query) {
		return EXPRESSION_PARAMETER_QUOTING.matcher(query).replaceAll(QUOTED_EXPRESSION_PARAMETER);
	}

	private static boolean containsExpression(String query) {
		return query.contains(ENTITY_NAME_VARIABLE_EXPRESSION);
	}
}
