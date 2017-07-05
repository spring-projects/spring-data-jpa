/*
 * Copyright 2017 the original author or authors.
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

import java.util.function.Predicate;

import javax.persistence.Query;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * Encapsulates different strategies for the creation of a {@link QueryParameterSetter} from a {@link Query} and a
 * {@link ParameterBinding}
 *
 * @author Jens Schauder
 * @since 2.0
 */
interface QueryParameterSetterStrategy {

	QueryParameterSetter create(Query jpaQuery, ParameterBinding binding);

	/**
	 * Handles bindings that are SpEL expressions by evalutating the expression to obtain a value.
	 *
	 * @author Jens Schauder
	 */
	class ExpressionParameterStrategy implements QueryParameterSetterStrategy {

		private final EvaluationContextProvider evaluationContextProvider;
		private final SpelExpressionParser parser;
		private final Parameters<?, ?> parameters;
		private final Object[] values;

		ExpressionParameterStrategy(EvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser,
				Parameters<?, ?> parameters, Object[] values) {

			this.parameters = parameters;
			this.values = values;

			Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");
			Assert.notNull(parser, "SpelExpressionParser must not be null!");
			this.evaluationContextProvider = evaluationContextProvider;
			this.parser = parser;
		}

		@Override
		public QueryParameterSetter create(Query jpaQuery, ParameterBinding binding) {

			if (binding.isExpression()) {

				Expression expr = parseExpressionString(binding.getExpression());

				Object value = evaluateExpression(expr);

				return QueryParameterSetter.lenientQueryParameterSetter(binding.getName(), binding.getPosition(),
						binding.prepare(value));

			}
			return null;
		}

		private Expression parseExpressionString(String expressionString) {
			return parser.parseExpression(expressionString);
		}

		private Object evaluateExpression(Expression expr) {
			return expr.getValue(getEvaluationContext(), Object.class);
		}

		private EvaluationContext getEvaluationContext() {
			return evaluationContextProvider.getEvaluationContext(parameters, values);
		}
	}

	/**
	 * Extracts values for parameter bindings from method parameters. It handles named as well as indexed parameters.
	 *
	 * @author Jens Schauder
	 * @since 2.0
	 */
	class BasicParameterStrategy implements QueryParameterSetterStrategy {

		private final JpaParameters parameters;
		private final ParameterAccessor accessor;

		BasicParameterStrategy(JpaParameters parameters, ParameterAccessor accessor) {

			this.parameters = parameters;
			this.accessor = accessor;
		}

		@Override
		public QueryParameterSetter create(Query jpaQuery, ParameterBinding binding) {

			JpaParameter parameter = findParameterForBinding( //
					QueryUtils.hasNamedParameter(jpaQuery) //
							? matchByName(binding) : matchByIndex(binding) //
			);

			if (parameter == null) {
				return QueryParameterSetter.NOOP;
			}

			Object preparedValue = binding.prepare(accessor.getBindableValue(parameter.getIndex()));

			return QueryParameterSetter.fromJpaParameter(parameter, binding.getPosition(), preparedValue);
		}

		private JpaParameter findParameterForBinding(Predicate<JpaParameter> predicate) {

			for (JpaParameter methodParameterCandidate : parameters.getBindableParameters()) {
				if (predicate.test(methodParameterCandidate)) {
					return methodParameterCandidate;
				}
			}

			return null;
		}

		private static Predicate<JpaParameter> matchByIndex(ParameterBinding binding) {
			return p -> p.getIndex() + 1 == binding.getPosition();
		}

		private static Predicate<JpaParameter> matchByName(ParameterBinding binding) {

			return p -> binding.getName().equals( //
					p.getName().orElseThrow( //
							() -> new IllegalArgumentException("Parameter needs to be named!") //
					));
		}

	}
}
