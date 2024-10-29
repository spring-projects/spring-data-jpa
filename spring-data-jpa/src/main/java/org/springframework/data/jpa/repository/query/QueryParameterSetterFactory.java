/*
 * Copyright 2017-2024 the original author or authors.
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

import java.util.List;
import java.util.function.Function;

import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.ParameterBinding.BindingIdentifier;
import org.springframework.data.jpa.repository.query.ParameterBinding.MethodInvocationArgument;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.jpa.repository.query.QueryParameterSetter.NamedOrIndexedQueryParameterSetter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
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

	@Nullable
	abstract QueryParameterSetter create(ParameterBinding binding, DeclaredQuery declaredQuery);

	/**
	 * Creates a new {@link QueryParameterSetterFactory} for the given {@link JpaParameters}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @return a basic {@link QueryParameterSetterFactory} that can handle named and index parameters.
	 */
	static QueryParameterSetterFactory basic(JpaParameters parameters) {

		Assert.notNull(parameters, "JpaParameters must not be null");

		return new BasicQueryParameterSetterFactory(parameters);
	}

	/**
	 * Creates a new {@link QueryParameterSetterFactory} using the given {@link JpaParameters} and
	 * {@link ParameterMetadata}.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @return a {@link QueryParameterSetterFactory} for criteria Queries.
	 */
	static QueryParameterSetterFactory forCriteriaQuery(JpaParameters parameters, List<ParameterMetadata<?>> metadata) {

		Assert.notNull(parameters, "JpaParameters must not be null");
		Assert.notNull(metadata, "ParameterMetadata must not be null");

		return new CriteriaQueryParameterSetterFactory(parameters, metadata);
	}

	/**
	 * Creates a new {@link QueryParameterSetterFactory} for the given {@link SpelExpressionParser},
	 * {@link EvaluationContextProvider} and {@link Parameters}.
	 *
	 * @param parser must not be {@literal null}.
	 * @param evaluationContextProvider must not be {@literal null}.
	 * @param parameters must not be {@literal null}.
	 * @return a {@link QueryParameterSetterFactory} that can handle
	 *         {@link org.springframework.expression.spel.standard.SpelExpression}s.
	 */
	static QueryParameterSetterFactory parsing(ValueExpressionParser parser,
			ValueEvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(parser, "ValueExpressionParser must not be null");
		Assert.notNull(evaluationContextProvider, "ValueEvaluationContextProvider must not be null");

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
	private static QueryParameterSetter createSetter(Function<JpaParametersParameterAccessor, Object> valueExtractor,
			ParameterBinding binding, @Nullable JpaParameter parameter) {

		TemporalType temporalType = parameter != null && parameter.isTemporalParameter() //
				? parameter.getRequiredTemporalType() //
				: null;

		return new NamedOrIndexedQueryParameterSetter(valueExtractor.andThen(binding::prepare),
				ParameterImpl.of(parameter, binding), temporalType);
	}

	@Nullable
	static JpaParameter findParameterForBinding(Parameters<JpaParameters, JpaParameter> parameters, String name) {

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
		 * @param parameters must not be {@literal null}.
		 */
		ExpressionBasedQueryParameterSetterFactory(ValueExpressionParser parser,
				ValueEvaluationContextProvider evaluationContextProvider) {

			Assert.notNull(parser, "ValueExpressionParser must not be null");
			Assert.notNull(evaluationContextProvider, "ValueEvaluationContextProvider must not be null");

			this.parser = parser;
			this.evaluationContextProvider = evaluationContextProvider;
		}

		@Nullable
		@Override
		public QueryParameterSetter create(ParameterBinding binding, DeclaredQuery declaredQuery) {

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
		@Nullable
		private Object evaluateExpression(ValueExpression expression, JpaParametersParameterAccessor accessor) {

			ValueEvaluationContext evaluationContext = evaluationContextProvider.getEvaluationContext(accessor.getValues());
			return expression.evaluate(evaluationContext);
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

		/**
		 * @param parameters must not be {@literal null}.
		 */
		BasicQueryParameterSetterFactory(JpaParameters parameters) {

			Assert.notNull(parameters, "JpaParameters must not be null");

			this.parameters = parameters;
		}

		@Override
		public QueryParameterSetter create(ParameterBinding binding, DeclaredQuery declaredQuery) {

			Assert.notNull(binding, "Binding must not be null");

			JpaParameter parameter;
			if (!(binding.getOrigin() instanceof MethodInvocationArgument mia)) {
				return QueryParameterSetter.NOOP;
			}

			BindingIdentifier identifier = mia.identifier();

			if (declaredQuery.hasNamedParameter()) {
				parameter = findParameterForBinding(parameters, identifier.getName());
			} else {
				parameter = findParameterForBinding(parameters, identifier.getPosition() - 1);
			}

			return parameter == null //
					? QueryParameterSetter.NOOP //
					: createSetter(values -> getValue(values, parameter), binding, parameter);
		}

		@Nullable
		private Object getValue(JpaParametersParameterAccessor accessor, Parameter parameter) {
			return accessor.getValue(parameter);
		}
	}

	/**
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @see QueryParameterSetterFactory
	 */
	private static class CriteriaQueryParameterSetterFactory extends QueryParameterSetterFactory {

		private final JpaParameters parameters;
		private final List<ParameterMetadata<?>> parameterMetadata;

		/**
		 * Creates a new {@link QueryParameterSetterFactory} from the given {@link JpaParameters} and
		 * {@link ParameterMetadata}.
		 *
		 * @param parameters must not be {@literal null}.
		 * @param metadata must not be {@literal null}.
		 */
		CriteriaQueryParameterSetterFactory(JpaParameters parameters, List<ParameterMetadata<?>> metadata) {

			Assert.notNull(parameters, "JpaParameters must not be null");
			Assert.notNull(metadata, "Expressions must not be null");

			this.parameters = parameters;
			this.parameterMetadata = metadata;
		}

		@Override
		public QueryParameterSetter create(ParameterBinding binding, DeclaredQuery declaredQuery) {

			int parameterIndex = binding.getRequiredPosition() - 1;

			Assert.isTrue( //
					parameterIndex < parameterMetadata.size(), //
					() -> String.format( //
							"At least %s parameter(s) provided but only %s parameter(s) present in query", //
							binding.getRequiredPosition(), //
							parameterMetadata.size() //
					) //
			);

			ParameterMetadata<?> metadata = parameterMetadata.get(parameterIndex);

			if (metadata.isIsNullParameter()) {
				return QueryParameterSetter.NOOP;
			}

			JpaParameter parameter = parameters.getBindableParameter(parameterIndex);
			TemporalType temporalType = parameter.isTemporalParameter() ? parameter.getRequiredTemporalType() : null;

			return new NamedOrIndexedQueryParameterSetter(values -> getAndPrepare(parameter, metadata, values),
					metadata.getExpression(), temporalType);
		}

		@Nullable
		private Object getAndPrepare(JpaParameter parameter, ParameterMetadata<?> metadata,
				JpaParametersParameterAccessor accessor) {
			return metadata.prepare(accessor.getValue(parameter));
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

		@Nullable
		@Override
		public String getName() {
			return identifier.hasName() ? identifier.getName() : null;
		}

		@Nullable
		@Override
		public Integer getPosition() {
			return identifier.hasPosition() ? identifier.getPosition() : null;
		}

		@Override
		public Class<T> getParameterType() {
			return parameterType;
		}

	}

}
