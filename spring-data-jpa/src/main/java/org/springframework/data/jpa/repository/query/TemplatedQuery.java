/*
 * Copyright 2013-2025 the original author or authors.
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

import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.Assert;

/**
 * Factory methods to obtain {@link EntityQuery} from a declared query using SpEL template-expressions.
 * <p>
 * Currently, the following template variables are available:
 * <ol>
 * <li>{@code #entityName} - the simple class name of the given entity</li>
 * <ol>
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Tom Hombergs
 * @author Michael J. Simons
 * @author Diego Krupitza
 * @author Greg Turnquist
 */
class TemplatedQuery {

	private static final String EXPRESSION_PARAMETER = "$1#{";
	private static final String QUOTED_EXPRESSION_PARAMETER = "$1__HASH__{";

	private static final Pattern EXPRESSION_PARAMETER_QUOTING = Pattern.compile("([:?])#\\{");
	private static final Pattern EXPRESSION_PARAMETER_UNQUOTING = Pattern.compile("([:?])__HASH__\\{");

	private static final String ENTITY_NAME = "entityName";
	private static final String ENTITY_NAME_VARIABLE = "#" + ENTITY_NAME;
	private static final String ENTITY_NAME_VARIABLE_EXPRESSION = "#{" + ENTITY_NAME_VARIABLE;

	private static final Environment DEFAULT_ENVIRONMENT;

	static {
		DEFAULT_ENVIRONMENT = new StandardEnvironment();
	}

	/**
	 * Create a {@link DefaultEntityQuery} given {@link String query}, {@link JpaQueryMethod} and
	 * {@link JpaQueryConfiguration}.
	 *
	 * @param queryString must not be {@literal null}.
	 * @param queryMethod must not be {@literal null}.
	 * @param queryContext must not be {@literal null}.
	 * @return the created {@link DefaultEntityQuery}.
	 */
	public static EntityQuery create(String queryString, JpaQueryMethod queryMethod, JpaQueryConfiguration queryContext) {
		return create(queryMethod.getDeclaredQuery(queryString), queryMethod.getEntityInformation(), queryContext);
	}

	/**
	 * Create a {@link DefaultEntityQuery} given {@link DeclaredQuery query}, {@link JpaEntityMetadata} and
	 * {@link JpaQueryConfiguration}.
	 *
	 * @param declaredQuery must not be {@literal null}.
	 * @param entityMetadata must not be {@literal null}.
	 * @param queryContext must not be {@literal null}.
	 * @return the created {@link DefaultEntityQuery}.
	 */
	public static EntityQuery create(DeclaredQuery declaredQuery, JpaEntityMetadata<?> entityMetadata,
			JpaQueryConfiguration queryContext) {

		ValueExpressionParser expressionParser = queryContext.getValueExpressionDelegate().getValueExpressionParser();
		String resolvedExpressionQuery = renderQueryIfExpressionOrReturnQuery(declaredQuery.getQueryString(),
				entityMetadata, expressionParser);

		return EntityQuery.create(declaredQuery.rewrite(resolvedExpressionQuery), queryContext.getSelector());
	}

	/**
	 * @param query, the query expression potentially containing a SpEL expression. Must not be {@literal null}.
	 * @param metadata the {@link JpaEntityMetadata} for the given entity. Must not be {@literal null}.
	 * @param parser Must not be {@literal null}.
	 */
	static String renderQueryIfExpressionOrReturnQuery(String query, JpaEntityMetadata<?> metadata,
			ValueExpressionParser parser) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(metadata, "metadata must not be null");
		Assert.notNull(parser, "parser must not be null");

		if (!containsExpression(query)) {
			return query;
		}

		SimpleEvaluationContext evalContext = SimpleEvaluationContext.forReadOnlyDataBinding().build();
		evalContext.setVariable(ENTITY_NAME, metadata.getEntityName());

		query = potentiallyQuoteExpressionsParameter(query);

		ValueExpression expr = parser.parse(query);

		String result = Objects.toString(expr.evaluate(ValueEvaluationContext.of(DEFAULT_ENVIRONMENT, evalContext)));

		if (result == null) {
			return query;
		}

		return potentiallyUnquoteParameterExpressions(result);
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
