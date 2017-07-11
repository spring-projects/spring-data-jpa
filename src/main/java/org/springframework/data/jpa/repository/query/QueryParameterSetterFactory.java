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

import java.util.List;
import java.util.function.Function;

import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.jpa.repository.query.QueryParameterSetter.NamedOrIndexedQueryParameterSetter;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import lombok.RequiredArgsConstructor;

/**
 * Encapsulates different strategies for the creation of a {@link QueryParameterSetter} from a {@link Query} and a
 * {@link ParameterBinding}
 *
 * @author Jens Schauder
 * @since 2.0
 */
interface QueryParameterSetterFactory {

	QueryParameterSetter create(ParameterBinding binding, String queryString);

	/**
	 * Handles bindings that are SpEL expressions by evaluating the expression to obtain a value.
	 *
	 * @author Jens Schauder
	 */
	@RequiredArgsConstructor
	class ExpressionBasedQueryParameterSetterFactory implements QueryParameterSetterFactory {

		private final EvaluationContextProvider evaluationContextProvider;
		private final SpelExpressionParser parser;
		private final Parameters<?, ?> parameters;

		@Override
		public QueryParameterSetter create(ParameterBinding binding, String queryString) {

			if (binding.isExpression()) {

				Expression expr = parseExpressionString(binding.getExpression());
				Function<Object[], Object> valueExtractor = vs -> {

					EvaluationContext context = evaluationContextProvider.getEvaluationContext(parameters, vs);
					return expr.getValue(context, Object.class);
				};

				return QueryParameterSetter.createLenient(valueExtractor, binding);
			}

			return null;
		}

		private Expression parseExpressionString(String expressionString) {
			return parser.parseExpression(expressionString);
		}
	}

	/**
	 * Extracts values for parameter bindings from method parameters. It handles named as well as indexed parameters.
	 *
	 * @author Jens Schauder
	 * @since 2.0
	 */
	@RequiredArgsConstructor
	class BasicQueryParameterSetterFactory implements QueryParameterSetterFactory {

		private final JpaParameters parameters;

		@Override
		public QueryParameterSetter create(ParameterBinding binding, String queryString) {

			JpaParameter parameter = QueryUtils.hasNamedParameter(queryString) //
					? findParameterForBinding(binding)
					: parameters.getBindableParameter(binding.getPosition() - 1);

			if (parameter == null) {
				return QueryParameterSetter.NOOP;
			}

			Function<Object[], Object> valueExtractor = values -> {

				JpaParametersParameterAccessor accessor = new JpaParametersParameterAccessor(parameters, values);

				return accessor.getValue(parameter);
			};

			return QueryParameterSetter.create(valueExtractor, binding, parameter);
		}

		private JpaParameter findParameterForBinding(ParameterBinding binding) {

			for (JpaParameter methodParameterCandidate : parameters.getBindableParameters()) {

				if (binding.getName().equals(getName(methodParameterCandidate))) {
					return methodParameterCandidate;
				}
			}

			return null;
		}

		private static String getName(JpaParameter p) {
			return p.getName().orElseThrow(() -> new IllegalArgumentException("Parameter needs to be named!"));
		}
	}

	@RequiredArgsConstructor
	class CriteriaQueryParameterSetterFactory implements QueryParameterSetterFactory {

		private final JpaParameters parameters;
		private final List<ParameterMetadata<?>> expressions;

		@Override
		public QueryParameterSetter create(ParameterBinding binding, String queryString) {

			ParameterMetadata<?> metadata = expressions.get(binding.getPosition() - 1);

			if (metadata.isIsNullParameter()) {
				return QueryParameterSetter.NOOP;
			}

			JpaParameter parameter = parameters.getBindableParameter(binding.getPosition() - 1);

			Function<Object[], Object> valueExtractor = vs -> {

				Object value = new JpaParametersParameterAccessor(parameters, vs).getValue(parameter);
				return metadata.prepare(value);
			};

			TemporalType temporalTypeOrNull = parameter.isTemporalParameter() ? parameter.getTemporalType() : null;

			return new NamedOrIndexedQueryParameterSetter(valueExtractor, metadata.getExpression(), temporalTypeOrNull,
					false);
		}
	}
}
