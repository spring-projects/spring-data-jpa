/*
 * Copyright 2008-2011 the original author or authors.
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
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.util.Assert;

/**
 * {@link ParameterBinder} is used to bind method parameters to a {@link Query}. This is usually done whenever an
 * {@link AbstractJpaQuery} is executed.
 * 
 * @author Oliver Gierke
 */
public class ParameterBinder {

	private final Parameters parameters;
	private final Object[] values;

	/**
	 * Creates a new {@link ParameterBinder}.
	 * 
	 * @param parameters
	 * @param values
	 */
	public ParameterBinder(Parameters parameters, Object[] values) {

		Assert.notNull(parameters);
		Assert.notNull(values);

		Assert.isTrue(parameters.getNumberOfParameters() == values.length, "Invalid number of parameters given!");

		this.parameters = parameters;
		this.values = values.clone();
	}

	ParameterBinder(Parameters parameters) {

		this(parameters, new Object[0]);
	}

	/**
	 * Returns the {@link Pageable} of the parameters, if available. Returns {@code null} otherwise.
	 * 
	 * @return
	 */
	public Pageable getPageable() {

		if (!parameters.hasPageableParameter()) {
			return null;
		}

		return (Pageable) values[parameters.getPageableIndex()];
	}

	/**
	 * Returns the sort instance to be used for query creation. Will use a {@link Sort} parameter if available or the
	 * {@link Sort} contained in a {@link Pageable} if available. Returns {@code null} if no {@link Sort} can be found.
	 * 
	 * @return
	 */
	public Sort getSort() {

		if (parameters.hasSortParameter()) {
			return (Sort) values[parameters.getSortIndex()];
		}

		if (parameters.hasPageableParameter() && getPageable() != null) {
			return getPageable().getSort();
		}

		return null;
	}

	/**
	 * Binds the parameters to the given {@link Query}.
	 * 
	 * @param query
	 * @return
	 */
	public <T extends Query> T bind(T query) {

		int methodParameterPosition = 0;
		int queryParameterPosition = 1;

		for (Parameter parameter : parameters) {

			if (parameter.isBindable()) {

				Object value = values[methodParameterPosition];
				bind(query, parameter, value, queryParameterPosition++);
			}

			methodParameterPosition++;
		}

		return query;
	}

	protected void bind(Query query, Parameter parameter, Object value, int position) {

		if (hasNamedParameter(query) && parameter.isNamedParameter()) {
			query.setParameter(parameter.getName(), value);
		} else {
			query.setParameter(position, value);
		}
	}

	/**
	 * Binds the parameters to the given query and applies special parameter types (e.g. pagination).
	 * 
	 * @param query
	 * @return
	 */
	public Query bindAndPrepare(Query query) {

		return bindAndPrepare(query, parameters);
	}

	private Query bindAndPrepare(Query query, Parameters parameters) {

		Query result = bind(query);

		if (!parameters.hasPageableParameter() || getPageable() == null) {
			return result;
		}

		result.setFirstResult(getPageable().getOffset());
		result.setMaxResults(getPageable().getPageSize());

		return result;
	}

	boolean hasNamedParameter(Query query) {

		return QueryUtils.hasNamedParameter(query);
	}
}
