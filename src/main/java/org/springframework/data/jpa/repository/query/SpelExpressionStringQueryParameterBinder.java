/*
 * Copyright 2014 the original author or authors.
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

import javax.persistence.Query;

import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * A {@link StringQueryParameterBinder} that is able to bind synthetic query parameters.
 *
 * @author Thomas Darimont
 */
class SpelExpressionStringQueryParameterBinder extends StringQueryParameterBinder {

	private final StringQuery query;
	private final EvaluationContextProvider evaluationContextProvider;
	private final SpelExpressionParser parser;

	/**
	 * Creates a new {@link SpelExpressionStringQueryParameterBinder}.
	 *
	 * @param parameters must not be {@literal null}
	 * @param values must not be {@literal null}
	 * @param query must not be {@literal null}
	 * @param evaluationContextProvider must not be {@literal null}
	 * @param parser must not be {@literal null}
	 */
	public SpelExpressionStringQueryParameterBinder(JpaParameters parameters, Object[] values, StringQuery query,
			EvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {

		super(parameters, values, query);
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");
		Assert.notNull(parser, "SpelExpressionParser must not be null!");

		this.evaluationContextProvider = evaluationContextProvider;
		this.query = query;
		this.parser = parser;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.ParameterBinder#bind(javax.persistence.Query)
	 */
	@Override
	public <T extends Query> T bind(T jpaQuery) {
		return potentiallyBindExpressionParameters(super.bind(jpaQuery));
	}

	/**
	 * @param jpaQuery must not be {@literal null}
	 * @return
	 */
	private <T extends Query> T potentiallyBindExpressionParameters(T jpaQuery) {

		if (isJpaParameterInformationReliable(jpaQuery) && jpaQuery.getParameters().isEmpty()) {
			// We can rely on the fact there are no parameters in the given query.
			return jpaQuery;
		}

		for (ParameterBinding binding : query.getParameterBindings()) {

			if (binding.isExpression()) {

				Expression expr = parseExpressionString(binding.getExpression());

				Object value = evaluateExpression(expr);

				try {
					if (binding.getName() != null) {
						jpaQuery.setParameter(binding.getName(), binding.prepare(value));
					} else {
						jpaQuery.setParameter(binding.getPosition(), binding.prepare(value));
					}
				} catch (IllegalArgumentException iae) {
					/*
					 * Since Eclipse doesn't reliably report whether a query has parameters
					 * we simply try to set the parameters and ignore possible failures.
					 *
					 */
				}
			}
		}

		return jpaQuery;
	}

	private <T extends Query> boolean isJpaParameterInformationReliable(T jpaQuery) {

		String className = jpaQuery.getClass().getName();
		return className.startsWith("org.apache.openjpa") || className.startsWith("org.hibernate");
	}

	/**
	 * Parses the given {@code expressionString} into a SpEL {@link Expression}.
	 *
	 * @param expressionString
	 * @return
	 */
	protected Expression parseExpressionString(String expressionString) {
		return parser.parseExpression(expressionString);
	}

	/**
	 * Evaluates the given {@code expressionString} as a SpEL {@link Expression}.
	 *
	 * @param expressionString
	 * @return
	 */
	protected Object evaluateExpression(String expressionString) {
		return evaluateExpression(parseExpressionString(expressionString));
	}

	/**
	 * Evaluates the given SpEL {@link Expression}.
	 *
	 * @param expr
	 * @return
	 */
	protected Object evaluateExpression(Expression expr) {
		return expr.getValue(getEvaluationContext(), Object.class);
	}

	/**
	 * Returns the {@link StandardEvaluationContext} to use for evaluation.
	 *
	 * @return
	 */
	protected EvaluationContext getEvaluationContext() {
		return evaluationContextProvider.getEvaluationContext(getParameters(), getValues());
	}
}
