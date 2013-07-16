/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Extension of {@link StringQuery} that evaluates the given query string as a SpEL template-expression.
 * <p>
 * Currently the following template variables are available:
 * <ol>
 * <li>#{#domainType} - the simple class name of the given entity</li>
 * <ol>
 * </p>
 * 
 * @author Thomas Darimont
 */
class ExpressionBasedStringQuery extends StringQuery {

	private static final String DOMAIN_TYPE = "domainType";

	/**
	 * @param query
	 */
	public ExpressionBasedStringQuery(String query, Class<?> domainClass) {
		super(renderQueryIfExpressionOrReturnQuery(query, domainClass));
	}

	private static String renderQueryIfExpressionOrReturnQuery(String query, Class<?> domainClass) {

		if (!containsExpression(query)) {
			return query;
		}

		StandardEvaluationContext evalContext = new StandardEvaluationContext();
		evalContext.setVariable(DOMAIN_TYPE, domainClass.getSimpleName());

		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expr = parser.parseExpression(query, ParserContext.TEMPLATE_EXPRESSION);

		Object result = expr.getValue(evalContext, String.class);

		if (result == null) {
			return query;
		}

		return String.valueOf(result);
	}

	private static boolean containsExpression(String query) {
		return query.contains("#{#" + DOMAIN_TYPE + "}");
	}
}
