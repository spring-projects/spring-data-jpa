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

import org.springframework.data.jpa.support.SpelParserAwareEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * A {@link ParameterBinder} that is able to detect and dynamically evaluate SpEL expression based parameters.
 * 
 * @author Thomas Darimont
 */
class ExpressionAwareParameterBinder extends ParameterBinder {

	private final SpelParserAwareEvaluationContextProvider evaluationContextProvider;

	/**
	 * Creates a new {@literal ExpressionAwareParameterBinder}.
	 * 
	 * @param parameters
	 * @param evaluationContextProvider must not be {@literal null}.
	 */
	public ExpressionAwareParameterBinder(JpaParameters parameters,
			SpelParserAwareEvaluationContextProvider evaluationContextProvider) {
		this(parameters, new Object[0], evaluationContextProvider);
	}

	/**
	 * Creates a new {@literal ExpressionAwareParameterBinder}.
	 * 
	 * @param parameters
	 * @param values
	 * @param evaluationContextProvider must not be {@literal null}.
	 */
	public ExpressionAwareParameterBinder(JpaParameters parameters, Object[] values,
			SpelParserAwareEvaluationContextProvider evaluationContextProvider) {

		super(parameters, values);

		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

		this.evaluationContextProvider = evaluationContextProvider;
	}

	/**
	 * Parses the given {@code expressionString} into a SpEL {@link Expression}.
	 * 
	 * @param expressionString
	 * @return
	 */
	protected Expression parseExpressionString(String expressionString) {
		return evaluationContextProvider.getParser().parseExpression(expressionString);
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
