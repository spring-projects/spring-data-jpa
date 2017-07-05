/*
 * Copyright 2008-2017 the original author or authors.
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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
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

	/** Meta information for the method parameters */
	private final JpaParameters parameters;
	protected final ParameterAccessor accessor;

	/**
	 * Creates a new {@link ParameterBinder}.
	 * 
	 * @param parameters must not be {@literal null}.
	 * @param values values of the parameters passed to the method. Must not be {@literal null}.
	 */
	public ParameterBinder(JpaParameters parameters, Object[] values) {

		Assert.notNull(parameters, "JpaParameters must not be null!");
		Assert.notNull(values, "Values must not be null!");

		Assert.isTrue(parameters.getNumberOfParameters() == values.length, "Invalid number of parameters given!");

		this.parameters = parameters;
		this.accessor = new ParametersParameterAccessor(parameters, values.clone());
	}

	ParameterBinder(JpaParameters parameters) {
		this(parameters, new Object[0]);
	}

	/**
	 * Returns the {@link Pageable} of the parameters, if available. Returns {@code null} otherwise.
	 */
	Pageable getPageable() {
		return accessor.getPageable();
	}

	/**
	 * Returns the sort instance to be used for query creation. Will use a {@link Sort} parameter if available or the
	 * {@link Sort} contained in a {@link Pageable} if available. Returns {@code null} if no {@link Sort} can be found.
	 */
	Sort getSort() {
		return accessor.getSort();
	}

	/**
	 * Binds the parameters to the given {@link Query}.
	 * 
	 * @param query must not be {@literal null}.
	 */
	protected <T extends Query> T bind(T query) {

		Assert.notNull(query, "Query must not be null!");

		int bindableParameterIndex = 0;

		for (JpaParameter parameter : parameters) {

			if (parameter.isBindable()) {

				Object value = accessor.getBindableValue(bindableParameterIndex);

				bind(parameter, value, bindableParameterIndex + 1).setParameter(query);

				bindableParameterIndex++;
			}
		}

		return query;
	}

	/**
	 * Creates {@link QueryParameterSetter} for the given {@link JpaParameter}. This implementation uses the name or index
	 * of the passed in parameter. This method is intended to be overwritten by subclasses in order to implement
	 * alternative binding strategies.
	 *
	 * @param parameter Method parameter from which to create a {@link QueryParameterSetter}
	 * @param value The value of the method parameter.
	 * @param position the index of the query parameter. Note that there is no simple relationg between index of the
	 *          method parameter and the position of the bind parameter due to unbindable parameters.
	 * @return guaranteed not to be {@literal null}.
	 */
	protected QueryParameterSetter bind(JpaParameter parameter, Object value, Integer position) {

		return QueryParameterSetter.fromJpaParameter(parameter, position, value);
	}

	/**
	 * Binds the parameters to the given query and applies special parameter types (e.g. pagination).
	 * 
	 * @param query must not be {@literal null}.
	 */
	Query bindAndPrepare(Query query) {

		Assert.notNull(query, "Query must not be null!");

		return bindAndPrepare(query, parameters);
	}

	private Query bindAndPrepare(Query query, Parameters<?, ?> parameters) {

		Query result = bind(query);

		if (!parameters.hasPageableParameter() || getPageable().isUnpaged()) {
			return result;
		}

		result.setFirstResult((int) getPageable().getOffset());
		result.setMaxResults(getPageable().getPageSize());

		return result;
	}
}
