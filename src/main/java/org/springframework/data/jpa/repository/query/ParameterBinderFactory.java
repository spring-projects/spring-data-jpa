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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.jpa.repository.query.QueryParameterSetterFactory.BasicQueryParameterSetterFactory;
import org.springframework.data.jpa.repository.query.QueryParameterSetterFactory.CriteriaQueryParameterSetterFactory;
import org.springframework.data.jpa.repository.query.QueryParameterSetterFactory.ExpressionBasedQueryParameterSetterFactory;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Factory for differently configured {@link ParameterBinder}.
 * 
 * @author Jens Schauder
 */
class ParameterBinderFactory {

	/**
	 * create a {@link ParameterBinder} that just matches parameter by name if those are available, or by index/position
	 * otherwise.
	 * 
	 * @param parameters method parameters that are available for binding
	 * @return a {@link ParameterBinder} that can assign values for the method parameters to query parameters of a
	 *         {@link javax.persistence.Query}
	 */
	static ParameterBinder createParameterBinder(JpaParameters parameters) {

		List<QueryParameterSetter> setters = createParameterSetters( //
				getParameterBindings(parameters), //
				null, //
				new BasicQueryParameterSetterFactory(parameters) //
		);

		return new ParameterBinder(parameters, setters);
	}

	/**
	 * create a {@link ParameterBinder} that just matches method parameter to parameters of a
	 * javax.{@link javax.persistence.criteria.CriteriaQuery}.
	 *
	 * @param parameters method parameters that are available for binding
	 * @return a {@link ParameterBinder} that can assign values for the method parameters to query parameters of a
	 *         {@link javax.persistence.criteria.CriteriaQuery}
	 */
	static ParameterBinder createCriteriaParameterBinder(JpaParameters parameters,
			List<ParameterMetadata<?>> expressions) {

		List<QueryParameterSetter> setters = createParameterSetters( //
				getParameterBindings(parameters), //
				null, //
				new CriteriaQueryParameterSetterFactory(parameters, expressions) //
		);

		return new ParameterBinder(parameters, setters);
	}

	/**
	 * create a {@link ParameterBinder} that just matches parameter by name if those are available, or by index/position
	 * otherwise. The resulting {@link ParameterBinder} can also handle SpEL expressions in the query. Uses the supplied
	 * query in order to ensure that all query parameters are bound.
	 *
	 * @param parameters method parameters that are available for binding
	 * @return a {@link ParameterBinder} that can assign values for the method parameters to query parameters of a
	 *         {@link javax.persistence.Query} while processing SpEL expressions where applicable.
	 */
	static ParameterBinder createQueryAwareParameterBinder(JpaParameters parameters, StringQuery query,
			EvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {

		List<QueryParameterSetter> parameterSetters = createParameterSetters( //
				query.getParameterBindings(), //
				query.getQueryString(), //
				new ExpressionBasedQueryParameterSetterFactory(evaluationContextProvider, parser, parameters), //
				new BasicQueryParameterSetterFactory(parameters) //
		);

		return new ParameterBinder(parameters, parameterSetters);
	}

	private static List<ParameterBinding> getParameterBindings(JpaParameters parameters) {

		List<ParameterBinding> result = new ArrayList<>();
		int bindableParameterIndex = 0;

		for (JpaParameter parameter : parameters) {

			if (parameter.isBindable()) {
				result.add(new ParameterBinding(++bindableParameterIndex));
			}
		}

		return result;
	}

	private static List<QueryParameterSetter> createParameterSetters(List<ParameterBinding> parameterBindings,
			String queryString, QueryParameterSetterFactory... strategies) {

		return parameterBindings.stream() //
				.map((pb) -> createQueryParameterSetter(pb, strategies, queryString)) //
				.collect(Collectors.toList());
	}

	private static QueryParameterSetter createQueryParameterSetter(ParameterBinding binding,
			QueryParameterSetterFactory[] strategies, String queryString) {

		for (QueryParameterSetterFactory strategy : strategies) {
			QueryParameterSetter candidate = strategy.create(binding, queryString);
			if (candidate != null)
				return candidate;
		}

		return QueryParameterSetter.NOOP;
	}

}
