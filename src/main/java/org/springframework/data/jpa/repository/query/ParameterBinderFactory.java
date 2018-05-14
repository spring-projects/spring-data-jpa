/*
 * Copyright 2017-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.util.StreamUtils;
import org.springframework.expression.spel.standard.SpelExpressionParser;
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
	 * @return a {@link ParameterBinder} that can assign values for the method parameters to query parameters of a
	 *         {@link javax.persistence.Query}
	 */
	static ParameterBinder createBinder(JpaParameters parameters) {

		Assert.notNull(parameters, "JpaParameters must not be null!");

		QueryParameterSetterFactory setterFactory = QueryParameterSetterFactory.basic(parameters);
		List<ParameterBinding> bindings = getBindings(parameters);

		return new ParameterBinder(parameters, createSetters(bindings, setterFactory));
	}

	/**
	 * Creates a {@link ParameterBinder} that just matches method parameter to parameters of a
	 * {@link javax.persistence.criteria.CriteriaQuery}.
	 *
	 * @param parameters method parameters that are available for binding, must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @return a {@link ParameterBinder} that can assign values for the method parameters to query parameters of a
	 *         {@link javax.persistence.criteria.CriteriaQuery}
	 */
	static ParameterBinder createCriteriaBinder(JpaParameters parameters, List<ParameterMetadata<?>> metadata) {

		Assert.notNull(parameters, "JpaParameters must not be null!");
		Assert.notNull(metadata, "Parameter metadata must not be null!");

		QueryParameterSetterFactory setterFactory = QueryParameterSetterFactory.forCriteriaQuery(parameters, metadata);
		List<ParameterBinding> bindings = getBindings(parameters);

		return new ParameterBinder(parameters, createSetters(bindings, setterFactory));
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
	 *         {@link javax.persistence.Query} while processing SpEL expressions where applicable.
	 */
	static ParameterBinder createQueryAwareBinder(JpaParameters parameters, DeclaredQuery query,
			SpelExpressionParser parser, QueryMethodEvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(parameters, "JpaParameters must not be null!");
		Assert.notNull(query, "StringQuery must not be null!");
		Assert.notNull(parser, "SpelExpressionParser must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

		List<ParameterBinding> bindings = query.getParameterBindings();
		QueryParameterSetterFactory expressionSetterFactory = QueryParameterSetterFactory.parsing(parser,
				evaluationContextProvider, parameters);
		QueryParameterSetterFactory basicSetterFactory = QueryParameterSetterFactory.basic(parameters);

		return new ParameterBinder(parameters, createSetters(bindings, query, expressionSetterFactory, basicSetterFactory),
				!query.usesPaging());
	}

	private static List<ParameterBinding> getBindings(JpaParameters parameters) {

		List<ParameterBinding> result = new ArrayList<>();
		int bindableParameterIndex = 0;

		for (JpaParameter parameter : parameters) {

			if (parameter.isBindable()) {
				result.add(new ParameterBinding(++bindableParameterIndex));
			}
		}

		return result;
	}

	private static Iterable<QueryParameterSetter> createSetters(List<ParameterBinding> parameterBindings,
			QueryParameterSetterFactory... factories) {
		return createSetters(parameterBindings, EmptyDeclaredQuery.EMPTY_QUERY, factories);
	}

	private static Iterable<QueryParameterSetter> createSetters(List<ParameterBinding> parameterBindings,
			DeclaredQuery declaredQuery, QueryParameterSetterFactory... strategies) {

		return parameterBindings.stream() //
				.map(it -> createQueryParameterSetter(it, strategies, declaredQuery)) //
				.collect(StreamUtils.toUnmodifiableList());
	}

	private static QueryParameterSetter createQueryParameterSetter(ParameterBinding binding,
			QueryParameterSetterFactory[] strategies, DeclaredQuery declaredQuery) {

		return Arrays.stream(strategies)//
				.map(it -> it.create(binding, declaredQuery))//
				.filter(Objects::nonNull)//
				.findFirst()//
				.orElse(QueryParameterSetter.NOOP);
	}
}
