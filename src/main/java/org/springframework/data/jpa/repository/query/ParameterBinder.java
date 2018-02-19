/*
 * Copyright 2008-2018 the original author or authors.
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

import javax.persistence.Query;

import org.springframework.data.jpa.repository.query.QueryParameterSetter.ErrorHandling;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.util.Assert;

/**
 * {@link ParameterBinder} is used to bind method parameters to a {@link Query}. This is usually done whenever an
 * {@link AbstractJpaQuery} is executed.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Jens Schauder
 */
public class ParameterBinder {

	static final String PARAMETER_NEEDS_TO_BE_NAMED = "For queries with named parameters you need to use provide names for method parameters. Use @Param for query method parameters, or when on Java 8+ use the javac flag -parameters.";

	private final JpaParameters parameters;
	private final Iterable<QueryParameterSetter> parameterSetters;

	/**
	 * Creates a new {@link ParameterBinder} for the given {@link JpaParameters} and {@link QueryParameterSetter}s.
	 *
	 * @param parameters must not be {@literal null}.
	 * @param parameterSetters must not be {@literal null}.
	 */
	public ParameterBinder(JpaParameters parameters, Iterable<QueryParameterSetter> parameterSetters) {

		Assert.notNull(parameters, "JpaParameters must not be null!");
		Assert.notNull(parameterSetters, "Parameter setters must not be null!");

		this.parameters = parameters;
		this.parameterSetters = parameterSetters;
	}

	public <T extends Query> T bind(T jpaQuery, Object[] values) {
		return bind(jpaQuery, values, ErrorHandling.STRICT);
	}

	public <T extends Query> T bind(T jpaQuery, Object[] values, ErrorHandling errorHandling) {

		parameterSetters.forEach(it -> it.setParameter(jpaQuery, values, errorHandling));

		return jpaQuery;
	}

	/**
	 * Binds the parameters to the given query and applies special parameter types (e.g. pagination).
	 *
	 * @param query must not be {@literal null}.
	 * @param values values of method parameters to be assigned to the query parameters.
	 */
	Query bindAndPrepare(Query query, Object[] values) {

		Assert.notNull(query, "Query must not be null!");

		ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);

		Query result = bind(query, values);

		if (!parameters.hasPageableParameter() || accessor.getPageable().isUnpaged()) {
			return result;
		}

		result.setFirstResult((int) accessor.getPageable().getOffset());
		result.setMaxResults(accessor.getPageable().getPageSize());

		return result;
	}
}
