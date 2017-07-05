/*
 * Copyright 2013-2017 the original author or authors.
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

import javax.persistence.Query;

import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * {@link ParameterBinder} has a {@link StringQuery} and therefor knows which query parameters need values. For binding
 * values to a query it iterates the required bindings and tries to find a value for each binding. For finding those
 * values it uses a list of strategies.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Jens Schauder
 */
class QueryAwareParameterBinder extends ParameterBinder {

	private final StringQuery query;

	private final List<QueryParameterSetterStrategy> queryParameterSetterStrategies = new ArrayList<>();

	/**
	 * Creates a new {@link QueryAwareParameterBinder} from the given {@link Parameters}, method arguments and
	 * {@link StringQuery}.
	 * 
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 */
	QueryAwareParameterBinder(JpaParameters parameters, Object[] values, StringQuery query,
			EvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {

		super(parameters, values);

		Assert.notNull(query, "StringQuery must not be null!");
		this.query = query;

		queryParameterSetterStrategies
				.add(new QueryParameterSetterStrategy.ExpressionParameterStrategy(evaluationContextProvider, parser, parameters, values.clone()));
		queryParameterSetterStrategies.add(new QueryParameterSetterStrategy.BasicParameterStrategy(parameters, accessor));
	}

	@Override
	public <T extends Query> T bind(T jpaQuery) {

		for (ParameterBinding binding : query.getParameterBindings()) {
			createQueryParameterSetter(jpaQuery, binding).setParameter(jpaQuery);
		}

		return jpaQuery;
	}

	private <T extends Query> QueryParameterSetter createQueryParameterSetter(T jpaQuery, ParameterBinding binding) {

		for (QueryParameterSetterStrategy strategy : queryParameterSetterStrategies) {
			QueryParameterSetter candidate = strategy.create(jpaQuery, binding);
			if (candidate != null)
				return candidate;
		}

		return QueryParameterSetter.NOOP;
	}
}
