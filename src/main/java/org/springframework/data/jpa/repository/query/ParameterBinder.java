/*
 * Copyright 2008-2014 the original author or authors.
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

import java.util.Date;

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
 */
public class ParameterBinder {

	private final JpaParameters parameters;
	private final ParameterAccessor accessor;
	private final Object[] values;

	/**
	 * Creates a new {@link ParameterBinder}.
	 * 
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	public ParameterBinder(JpaParameters parameters, Object[] values) {

		Assert.notNull(parameters);
		Assert.notNull(values);

		Assert.isTrue(parameters.getNumberOfParameters() == values.length, "Invalid number of parameters given!");

		this.parameters = parameters;
		this.values = values.clone();
		this.accessor = new ParametersParameterAccessor(parameters, this.values);
	}

	ParameterBinder(JpaParameters parameters) {
		this(parameters, new Object[0]);
	}

	/**
	 * Returns the {@link Pageable} of the parameters, if available. Returns {@code null} otherwise.
	 * 
	 * @return
	 */
	public Pageable getPageable() {
		return accessor.getPageable();
	}

	/**
	 * Returns the sort instance to be used for query creation. Will use a {@link Sort} parameter if available or the
	 * {@link Sort} contained in a {@link Pageable} if available. Returns {@code null} if no {@link Sort} can be found.
	 * 
	 * @return
	 */
	public Sort getSort() {
		return accessor.getSort();
	}

	/**
	 * Binds the parameters to the given {@link Query}.
	 * 
	 * @param query
	 * @return
	 */
	public <T extends Query> T bind(T query) {

		int bindableParameterIndex = 0;
		int queryParameterPosition = 1;

		for (JpaParameter parameter : parameters) {

			if (canBindParameter(parameter)) {

				Object value = accessor.getBindableValue(bindableParameterIndex);
				bind(query, parameter, value, queryParameterPosition++);
				bindableParameterIndex++;
			}
		}

		return query;
	}

	/**
	 * Returns {@literal true} if the given parameter can be bound.
	 * 
	 * @param parameter
	 * @return
	 */
	protected boolean canBindParameter(JpaParameter parameter) {
		return parameter.isBindable();
	}

	/**
	 * Perform the actual query parameter binding.
	 * 
	 * @param query
	 * @param parameter
	 * @param value
	 * @param position
	 */
	protected void bind(Query query, JpaParameter parameter, Object value, int position) {

		if (parameter.isTemporalParameter()) {
			if (hasNamedParameter(query) && parameter.isNamedParameter()) {
				query.setParameter(parameter.getName(), (Date) value, parameter.getTemporalType());
			} else {
				query.setParameter(position, (Date) value, parameter.getTemporalType());
			}
			return;
		}

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

	boolean hasNamedParameter(Query query) {
		return QueryUtils.hasNamedParameter(query);
	}

	private Query bindAndPrepare(Query query, Parameters<?, ?> parameters) {

		Query result = bind(query);

		if (!parameters.hasPageableParameter() || getPageable() == null) {
			return result;
		}

		result.setFirstResult(getPageable().getOffset());
		result.setMaxResults(getPageable().getPageSize());

		return result;
	}

	/**
	 * Returns the parameters.
	 * 
	 * @return
	 */
	JpaParameters getParameters() {
		return parameters;
	}

	protected Object[] getValues() {
		return values;
	}
}
