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

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.ParameterBinding.BindingIdentifier;
import org.springframework.data.jpa.repository.query.ParameterBinding.ParameterOrigin;
import org.springframework.util.Assert;

/**
 * Factory for differently configured {@link ParameterBinder}.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 * @since 2.0
 */
class ParameterBinderFactory {

	/**
	 * Create a {@link ParameterBinder} that just matches parameter by name if those are available, or by index/position
	 * otherwise.
	 *
	 * @param parameters method parameters that are available for binding, must not be {@literal null}.
	 * @param preferNamedParameters
	 * @return a {@link ParameterBinder} that can assign values for the method parameters to query parameters of a
	 *         {@link jakarta.persistence.Query}
	 */
	static ParameterBinder createBinder(JpaParameters parameters, boolean preferNamedParameters) {

		Assert.notNull(parameters, "JpaParameters must not be null");

		QueryParameterSetterFactory setterFactory = QueryParameterSetterFactory.basic(parameters, preferNamedParameters);
		List<ParameterBinding> bindings = getBindings(parameters);

		return new ParameterBinder(parameters, createSetters(bindings, setterFactory));
	}

	/**
	 * Creates a {@link ParameterBinder} that matches method parameter to parameters of a
	 * {@link jakarta.persistence.Query} and that can bind synthetic parameters.
	 *
	 * @param parameters method parameters that are available for binding, must not be {@literal null}.
	 * @param bindings parameter bindings for method argument and synthetic parameters, must not be {@literal null}.
	 * @return a {@link ParameterBinder} that can assign values for the method parameters to query parameters of a
	 *         {@link jakarta.persistence.Query}
	 */
	static ParameterBinder createBinder(JpaParameters parameters, List<ParameterBinding> bindings) {

		Assert.notNull(parameters, "JpaParameters must not be null");
		Assert.notNull(bindings, "Parameter bindings must not be null");

		return new ParameterBinder(parameters,
				createSetters(bindings, QueryParameterSetterFactory.forPartTreeQuery(parameters),
						QueryParameterSetterFactory.forSynthetic()));
	}

	/**
	 * Creates a {@link ParameterBinder} that just matches parameter by name if those are available, or by index/position
	 * otherwise. The resulting {@link ParameterBinder} can also handle SpEL expressions in the query. Uses the supplied
	 * query in order to ensure that all query parameters are bound.
	 *
	 * @param parameters method parameters that are available for binding, must not be {@literal null}.
	 * @param query the {@link StringQuery} the binders shall be created for, must not be {@literal null}.
	 * @param parser must not be {@literal null}.
	 * @param evaluationContextProvider must not be {@literal null}.
	 * @return a {@link ParameterBinder} that can assign values for the method parameters to query parameters of a
	 *         {@link jakarta.persistence.Query} while processing SpEL expressions where applicable.
	 */
	static ParameterBinder createQueryAwareBinder(JpaParameters parameters, IntrospectedQuery query,
			ValueExpressionParser parser, ValueEvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(parameters, "JpaParameters must not be null");
		Assert.notNull(query, "StringQuery must not be null");
		Assert.notNull(parser, "SpelExpressionParser must not be null");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null");

		QueryParameterSetterFactory expressionSetterFactory = QueryParameterSetterFactory.parsing(parser,
				evaluationContextProvider);

		QueryParameterSetterFactory basicSetterFactory = QueryParameterSetterFactory.basic(parameters,
				query.hasNamedParameter());

		boolean usesPaging = query instanceof EntityQuery eq && eq.usesPaging();

		// TODO: lets maybe obtain the bindable query and pass that on to create the setters?
		return new ParameterBinder(parameters, createSetters(query.getParameterBindings(), query, expressionSetterFactory, basicSetterFactory),
				!usesPaging);
	}

	static List<ParameterBinding> getBindings(JpaParameters parameters) {

		List<ParameterBinding> result = new ArrayList<>(parameters.getNumberOfParameters());
		int bindableParameterIndex = 0;

		for (JpaParameter parameter : parameters) {

			if (parameter.isBindable()) {
				int index = ++bindableParameterIndex;
				BindingIdentifier bindingIdentifier = parameter.getName().map(it -> BindingIdentifier.of(it, index))
						.orElseGet(() -> BindingIdentifier.of(index));

				result.add(new ParameterBinding(bindingIdentifier, ParameterOrigin.ofParameter(bindingIdentifier)));
			}
		}

		return result;
	}

	private static Iterable<QueryParameterSetter> createSetters(List<ParameterBinding> parameterBindings,
			QueryParameterSetterFactory... factories) {
		return createSetters(parameterBindings, EmptyIntrospectedQuery.EMPTY_QUERY, factories);
	}

	private static Iterable<QueryParameterSetter> createSetters(List<ParameterBinding> parameterBindings,
			IntrospectedQuery query, QueryParameterSetterFactory... strategies) {

		List<QueryParameterSetter> setters = new ArrayList<>(parameterBindings.size());
		for (ParameterBinding parameterBinding : parameterBindings) {
			setters.add(createQueryParameterSetter(parameterBinding, strategies, query));
		}

		return setters;
	}

	private static QueryParameterSetter createQueryParameterSetter(ParameterBinding binding,
			QueryParameterSetterFactory[] strategies, IntrospectedQuery query) {

		for (QueryParameterSetterFactory strategy : strategies) {

			QueryParameterSetter setter = strategy.create(binding, query);

			if (setter != null) {
				return setter;
			}
		}

		return QueryParameterSetter.NOOP;
	}
}
