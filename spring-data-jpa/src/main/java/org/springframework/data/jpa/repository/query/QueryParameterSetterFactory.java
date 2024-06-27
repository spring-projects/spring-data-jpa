/*
 * Copyright 2017-2025 the original author or authors.
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

import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;

import java.util.function.Function;

import org.springframework.data.expression.ValueEvaluationContext;

import org.jspecify.annotations.Nullable;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.ParameterBinding.BindingIdentifier;
import org.springframework.data.jpa.repository.query.ParameterBinding.MethodInvocationArgument;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * Encapsulates different strategies for the creation of a {@link QueryParameterSetter} from a {@link Query} and a
 * {@link ParameterBinding}
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 2.0
 */
abstract class QueryParameterSetterFactory {

	/**
	 * Creates a {@link QueryParameterSetter} for the given {@link ParameterBinding}. This factory may return
	 * {@literal null} if it doesn't support the given {@link ParameterBinding}.
	 *
	 * @param binding the parameter binding to create a {@link QueryParameterSetter} for.
	 * @return
	 */
	abstract @Nullable QueryParameterSetter create(ParameterBinding binding, IntrospectedQuery introspectedQuery);

	/**
	 * Creates a new {@link QueryParameterSetterFactory} for the given {@link JpaParameters}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param preferNamedParameters whether to prefer named parameters.
	 * @return a basic {@link QueryParameterSetterFactory} that can handle named and index parameters.
	 */
	static QueryParameterSetterFactory basic(JpaParameters parameters, boolean preferNamedParameters) {
		return new BasicQueryParameterSetterFactory(parameters, preferNamedParameters);
	}

	/**
	 * Creates a new {@link QueryParameterSetterFactory} using the given {@link JpaParameters}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @return a {@link QueryParameterSetterFactory} for Part-Tree Queries.
	 */
	static QueryParameterSetterFactory forPartTreeQuery(JpaParameters parameters) {
		return new PartTreeQueryParameterSetterFactory(parameters);
	}

	/**
	 * Creates a new {@link QueryParameterSetterFactory} to bind
	 * {@link org.springframework.data.jpa.repository.query.ParameterBinding.Synthetic} parameters.
	 *
	 * @return a {@link QueryParameterSetterFactory} for JPQL Queries.
	 */
	static QueryParameterSetterFactory forSynthetic() {
		return new SyntheticParameterSetterFactory();
	}

	/**
	 * Creates a new {@link QueryParameterSetterFactory} for the given {@link SpelExpressionParser},
	 * {@link EvaluationContextProvider} and {@link Parameters}.
	 *
	 * @param parser must not be {@literal null}.
	 * @param evaluationContextProvider must not be {@literal null}.
	 * @return a {@link QueryParameterSetterFactory} that can handle
	 *         {@link org.springframework.expression.spel.standard.SpelExpression}s.
	 */
	static QueryParameterSetterFactory parsing(ValueExpressionParser parser,
			ValueEvaluationContextProvider evaluationContextProvider) {
		return new ExpressionBasedQueryParameterSetterFactory(parser, evaluationContextProvider);
	}

	/**
	 * Creates a {@link QueryParameterSetter} from a {@link JpaParameter}. Handles named and indexed parameters,
	 * TemporalType annotations and might ignore certain exception when requested to do so.
	 *
	 * @param valueExtractor extracts the relevant value from an array of method parameter values.
	 * @param binding the binding of the query parameter to be set.
	 * @param parameter the method parameter to bind.
	 */
	private static QueryParameterSetter createSetter(Function<JpaParametersParameterAccessor, @Nullable Object> valueExtractor,
			ParameterBinding binding, @Nullable JpaParameter parameter) {

		TemporalType temporalType = parameter != null && parameter.isTemporalParameter() //
				? parameter.getRequiredTemporalType() //
				: null;

		return QueryParameterSetter.create(valueExtractor.andThen(binding::prepare), ParameterImpl.of(parameter, binding),
				temporalType);
	}

	static @Nullable JpaParameter findParameterForBinding(Parameters<JpaParameters, JpaParameter> parameters, String name) {

		JpaParameters bindableParameters = parameters.getBindableParameters();

		for (JpaParameter bindableParameter : bindableParameters) {
			if (name.equals(getRequiredName(bindableParameter))) {
				return bindableParameter;
			}
		}

		return null;
	}

	private static String getRequiredName(JpaParameter p) {
		return p.getName().orElseThrow(() -> new IllegalStateException(ParameterBinder.PARAMETER_NEEDS_TO_BE_NAMED));
	}

	static JpaParameter findParameterForBinding(Parameters<JpaParameters, JpaParameter> parameters, int parameterIndex) {

		JpaParameters bindableParameters = parameters.getBindableParameters();

		Assert.isTrue( //
				parameterIndex < bindableParameters.getNumberOfParameters(), //
				() -> String.format( //
						"At least %s parameter(s) provided but only %s parameter(s) present in query", //
						parameterIndex + 1, //
						bindableParameters.getNumberOfParameters() //
				) //
		);

		return bindableParameters.getParameter(parameterIndex);
	}

	/**
	 * Handles bindings that are SpEL expressions by evaluating the expression to obtain a value.
	 *
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	private static class ExpressionBasedQueryParameterSetterFactory extends QueryParameterSetterFactory {

		private final ValueExpressionParser parser;
		private final ValueEvaluationContextProvider evaluationContextProvider;

		/**
		 * @param parser must not be {@literal null}.
		 * @param evaluationContextProvider must not be {@literal null}.
		 */
		ExpressionBasedQueryParameterSetterFactory(ValueExpressionParser parser,
				ValueEvaluationContextProvider evaluationContextProvider) {

			Assert.notNull(parser, "ValueExpressionParser must not be null");
			Assert.notNull(evaluationContextProvider, "ValueEvaluationContextProvider must not be null");

			this.parser = parser;
			this.evaluationContextProvider = evaluationContextProvider;
		}

		@Override
		public @Nullable QueryParameterSetter create(ParameterBinding binding, IntrospectedQuery introspectedQuery) {

			if (!(binding.getOrigin() instanceof ParameterBinding.Expression e)) {
				return null;
			}

			return createSetter(values -> evaluateExpression(e.expression(), values), binding, null);
		}

		/**
		 * Evaluates the given {@link Expression} against the given values.
		 *
		 * @param expression must not be {@literal null}.
		 * @param accessor must not be {@literal null}.
		 * @return the result of the evaluation.
		 */
		private @Nullable Object evaluateExpression(ValueExpression expression, JpaParametersParameterAccessor accessor) {

			ValueEvaluationContext evaluationContext = evaluationContextProvider.getEvaluationContext(accessor.getValues());
			return expression.evaluate(evaluationContext);
		}
	}

	/**
	 * Handles synthetic bindings that have been captured during parameter augmenting.
	 *
	 * @author Mark Paluch
	 * @since 4.0
	 */
	private static class SyntheticParameterSetterFactory extends QueryParameterSetterFactory {

		@Override
		public @Nullable QueryParameterSetter create(ParameterBinding binding, IntrospectedQuery query) {

			if (!(binding.getOrigin() instanceof ParameterBinding.Synthetic s)) {
				return null;
			}

			return createSetter(values -> s.value(), binding, null);
		}
	}

	/**
	 * Extracts values for parameter bindings from method parameters. It handles named as well as indexed parameters.
	 *
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 * @since 2.0
	 */
	private static class BasicQueryParameterSetterFactory extends QueryParameterSetterFactory {

		private final JpaParameters parameters;
		private final boolean preferNamedParameters;

		/**
		 * @param parameters must not be {@literal null}.
		 * @param preferNamedParameters whether to use named parameters.
		 */
		BasicQueryParameterSetterFactory(JpaParameters parameters, boolean preferNamedParameters) {

			Assert.notNull(parameters, "JpaParameters must not be null");

			this.parameters = parameters;
			this.preferNamedParameters = preferNamedParameters;
		}

		@Override
		public @Nullable QueryParameterSetter create(ParameterBinding binding, IntrospectedQuery introspectedQuery) {

			Assert.notNull(binding, "Binding must not be null");

			if (!(binding.getOrigin() instanceof MethodInvocationArgument mia)) {
				return null;
			}

			BindingIdentifier identifier = mia.identifier();
			JpaParameter parameter;

			if (preferNamedParameters && identifier.hasName()) {
				parameter = findParameterForBinding(parameters, identifier.getName());
			} else if (identifier.hasPosition()) {
				parameter = findParameterForBinding(parameters, identifier.getPosition() - 1);
			} else {
				// this can happen when a query uses parameters in ORDER BY and the COUNT query just needs to drop a binding.
				parameter = null;
			}

			return parameter == null //
					? QueryParameterSetter.NOOP //
					: createSetter(values -> getValue(values, parameter), binding, parameter);
		}

		protected @Nullable Object getValue(JpaParametersParameterAccessor accessor, Parameter parameter) {
			return accessor.getValue(parameter);
		}
	}

	/**
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 * @see QueryParameterSetterFactory
	 */
	private static class PartTreeQueryParameterSetterFactory extends BasicQueryParameterSetterFactory {

		private final JpaParameters parameters;

		private PartTreeQueryParameterSetterFactory(JpaParameters parameters) {
			super(parameters, false);
			this.parameters = parameters.getBindableParameters();
		}

		@Override
		public @Nullable QueryParameterSetter create(ParameterBinding binding, IntrospectedQuery query) {

			if (binding instanceof ParameterBinding.PartTreeParameterBinding ptb) {

				if (ptb.isIsNullParameter()) {
					return QueryParameterSetter.NOOP;
				}

				return super.create(binding, query);
			}

			return null;
		}
	}

	static class ParameterImpl<T> implements jakarta.persistence.Parameter<T> {

		private final BindingIdentifier identifier;
		private final Class<T> parameterType;

		/**
		 * Creates a new {@link ParameterImpl} for the given {@link JpaParameter} and {@link ParameterBinding}.
		 *
		 * @param parameter can be {@literal null}.
		 * @param binding must not be {@literal null}.
		 * @return a {@link jakarta.persistence.Parameter} object based on the information from the arguments.
		 */
		static jakarta.persistence.Parameter<?> of(@Nullable JpaParameter parameter, ParameterBinding binding) {

			Class<?> type = parameter == null ? Object.class : parameter.getType();

			return new ParameterImpl<>(binding.getIdentifier(), type);
		}

		public ParameterImpl(BindingIdentifier identifier, Class<T> parameterType) {
			this.identifier = identifier;
			this.parameterType = parameterType;
		}

		@Override
		public @Nullable String getName() {
			return identifier.hasName() ? identifier.getName() : null;
		}

		@Override
		public @Nullable Integer getPosition() {
			return identifier.hasPosition() ? identifier.getPosition() : null;
		}

		@Override
		public Class<T> getParameterType() {
			return parameterType;
		}
	}

}
