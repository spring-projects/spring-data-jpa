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
import org.springframework.data.repository.query.Parameter;
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
 * @author Oliver Gierke
 * @since 2.0
 */
abstract class QueryParameterSetterFactory {

	abstract QueryParameterSetter create(ParameterBinding binding, String queryString);

	/**
	 * Creates a new {@link QueryParameterSetterFactory} for the given {@link JpaParameters}.
	 * 
	 * @param parameters must not be {@literal null}.
	 * @return a basic {@link QueryParameterSetterFactory} that can handle named and index parameters.
	 */
	static QueryParameterSetterFactory basic(JpaParameters parameters) {

		Assert.notNull(parameters, "JpaParameters must not be null!");

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

		Assert.notNull(parameters, "JpaParameters must not be null!");
		Assert.notNull(metadata, "ParameterMetadata must not be null!");

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
	static QueryParameterSetterFactory parsing(SpelExpressionParser parser,
			EvaluationContextProvider evaluationContextProvider, Parameters<?, ?> parameters) {

		Assert.notNull(parser, "SpelExpressionParser must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");
		Assert.notNull(parameters, "Parameters must not be null!");

		return new ExpressionBasedQueryParameterSetterFactory(parser, evaluationContextProvider, parameters);
	}

	/**
	 * Creates a {@link QueryParameterSetter} from a {@link JpaParameter}. Handles named and indexed parameters,
	 * TemporalType annotations and might ignore certain exception when requested to do so.
	 *
	 * @param valueExtractor extracts the relevant value from an array of method parameter values.
	 * @param binding the binding of the query parameter to be set.
	 * @param parameter the method parameter to bind.
	 * @param lenient when true certain exceptions thrown when setting the query parameters get ignored.
	 */
	private static QueryParameterSetter createSetter(Function<Object[], Object> valueExtractor, ParameterBinding binding,
			JpaParameter parameter, boolean lenient) {

		TemporalType temporalType = parameter != null && parameter.isTemporalParameter() //
				? parameter.getTemporalType() //
				: null;

		return new NamedOrIndexedQueryParameterSetter(valueExtractor.andThen(binding::prepare),
				ParameterImpl.of(parameter, binding), temporalType, lenient);
	}

	/**
	 * Handles bindings that are SpEL expressions by evaluating the expression to obtain a value.
	 *
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	private static class ExpressionBasedQueryParameterSetterFactory extends QueryParameterSetterFactory {

		private final SpelExpressionParser parser;
		private final EvaluationContextProvider evaluationContextProvider;
		private final Parameters<?, ?> parameters;

		/**
		 * @param parser must not be {@literal null}.
		 * @param evaluationContextProvider must not be {@literal null}.
		 * @param parameters must not be {@literal null}.
		 */
		ExpressionBasedQueryParameterSetterFactory(SpelExpressionParser parser,
				EvaluationContextProvider evaluationContextProvider, Parameters<?, ?> parameters) {

			Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");
			Assert.notNull(parser, "SpelExpressionParser must not be null!");
			Assert.notNull(parameters, "Parameters must not be null!");

			this.evaluationContextProvider = evaluationContextProvider;
			this.parser = parser;
			this.parameters = parameters;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.QueryParameterSetterFactory#create(org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding, java.lang.String)
		 */
		@Override
		public QueryParameterSetter create(ParameterBinding binding, String queryString) {

			if (!binding.isExpression()) {
				return null;
			}

			Expression expression = parser.parseExpression(binding.getExpression());

			return createSetter(values -> evaluateExpression(expression, values), binding, null, true);
		}

		/**
		 * Evaluates the given {@link Expression} against the given values.
		 * 
		 * @param expression must not be {@literal null}.
		 * @param values must not be {@literal null}.
		 * @return the result of the evaluation.
		 */
		private Object evaluateExpression(Expression expression, Object[] values) {

			EvaluationContext context = evaluationContextProvider.getEvaluationContext(parameters, values);
			return expression.getValue(context, Object.class);
		}
	}

	/**
	 * Extracts values for parameter bindings from method parameters. It handles named as well as indexed parameters.
	 *
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @since 2.0
	 */
	private static class BasicQueryParameterSetterFactory extends QueryParameterSetterFactory {

		private final JpaParameters parameters;

		/**
		 * @param parameters must not be {@literal null}.
		 */
		BasicQueryParameterSetterFactory(JpaParameters parameters) {

			Assert.notNull(parameters, "JpaParameters must not be null!");

			this.parameters = parameters;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.QueryParameterSetterFactory#create(org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding, java.lang.String)
		 */
		@Override
		public QueryParameterSetter create(ParameterBinding binding, String queryString) {

			Assert.notNull(binding, "Binding must not be null.");

			JpaParameter parameter = QueryUtils.hasNamedParameter(queryString) //
					? findParameterForBinding(binding) //
					: parameters.getBindableParameter(binding.getPosition() - 1);

			return parameter == null //
					? QueryParameterSetter.NOOP //
					: createSetter(values -> getValue(values, parameter), binding, parameter, false);
		}

		private JpaParameter findParameterForBinding(ParameterBinding binding) {

			return parameters.getBindableParameters().stream() //
					.filter(candidate -> binding.getName().equals(getName(candidate))) //
					.findFirst().orElse(null);
		}

		private Object getValue(Object[] values, Parameter parameter) {
			return new JpaParametersParameterAccessor(parameters, values).getValue(parameter);
		}

		private static String getName(JpaParameter p) {
			return p.getName().orElseThrow(() -> new IllegalStateException(ParameterBinder.PARAMETER_NEEDS_TO_BE_NAMED));
		}
	}

	/**
	 * {@link QueryParameterSetterFactory}
	 * 
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 */
	private static class CriteriaQueryParameterSetterFactory extends QueryParameterSetterFactory {

		private final JpaParameters parameters;
		private final List<ParameterMetadata<?>> expressions;

		/**
		 * Creates a new {@link QueryParameterSetterFactory} from the given {@link JpaParameters} and
		 * {@link ParameterMetadata}.
		 * 
		 * @param parameters must not be {@literal null}.
		 * @param metadata must not be {@literal null}.
		 */
		CriteriaQueryParameterSetterFactory(JpaParameters parameters, List<ParameterMetadata<?>> metadata) {

			Assert.notNull(parameters, "JpaParameters must not be null!");
			Assert.notNull(metadata, "Expressions must not be null!");

			this.parameters = parameters;
			this.expressions = metadata;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.QueryParameterSetterFactory#create(org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding, java.lang.String)
		 */
		@Override
		public QueryParameterSetter create(ParameterBinding binding, String queryString) {

			ParameterMetadata<?> metadata = expressions.get(binding.getPosition() - 1);

			if (metadata.isIsNullParameter()) {
				return QueryParameterSetter.NOOP;
			}

			JpaParameter parameter = parameters.getBindableParameter(binding.getPosition() - 1);
			TemporalType temporalType = parameter.isTemporalParameter() ? parameter.getTemporalType() : null;

			return new NamedOrIndexedQueryParameterSetter(values -> getAndPrepare(parameter, metadata, values),
					metadata.getExpression(), temporalType, false);
		}

		private Object getAndPrepare(JpaParameter parameter, ParameterMetadata<?> metadata, Object[] values) {

			JpaParametersParameterAccessor accessor = new JpaParametersParameterAccessor(parameters, values);

			return metadata.prepare(accessor.getValue(parameter));
		}
	}

	private static class ParameterImpl<T> implements javax.persistence.Parameter<T> {

		private final Class<T> parameterType;
		private final String name;
		private final Integer position;

		/**
		 * Creates a new {@link ParameterImpl} for the given {@link JpaParameter} and {@link ParameterBinding}.
		 * 
		 * @param parameter can be {@literal null}.
		 * @param binding must not be {@literal null}.
		 * @return a {@link javax.persistence.Parameter} object based on the information from the arguments.
		 */
		static javax.persistence.Parameter<?> of(JpaParameter parameter, ParameterBinding binding) {

			Class<?> type = parameter == null ? Object.class : parameter.getType();

			return new ParameterImpl<>(type, getName(parameter, binding), binding.getPosition());
		}

		/**
		 * Creates a new {@link ParameterImpl} for the given name, position and parameter type.
		 * 
		 * @param parameterType must not be {@literal null}.
		 * @param name can be {@literal null}.
		 * @param position can be {@literal null}.
		 */
		private ParameterImpl(Class<T> parameterType, String name, Integer position) {

			this.name = name;
			this.position = position;
			this.parameterType = parameterType;
		}

		/* 
		 * (non-Javadoc)
		 * @see javax.persistence.Parameter#getName()
		 */
		@Override
		public String getName() {
			return name;
		}

		/* 
		 * (non-Javadoc)
		 * @see javax.persistence.Parameter#getPosition()
		 */
		@Override
		public Integer getPosition() {
			return position;
		}

		/* 
		 * (non-Javadoc)
		 * @see javax.persistence.Parameter#getParameterType()
		 */
		@Override
		public Class<T> getParameterType() {
			return parameterType;
		}

		private static String getName(JpaParameter parameter, ParameterBinding binding) {

			if (parameter == null) {
				return binding.getName();
			}

			return parameter.isNamedParameter() //
					? parameter.getName().orElseThrow(() -> new IllegalArgumentException("o_O parameter needs to have a name!")) //
					: null;
		}
	}
}
