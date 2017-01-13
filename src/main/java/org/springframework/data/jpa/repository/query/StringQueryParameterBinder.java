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

import javax.persistence.Query;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.StringQuery.LikeParameterBinding;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.util.Assert;

/**
 * {@link ParameterBinder} that takes {@link LikeParameterBinding}s encapsulated in a {@link StringQuery} into account.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class StringQueryParameterBinder extends ParameterBinder {

	private final StringQuery query;

	/**
	 * Creates a new {@link StringQueryParameterBinder} from the given {@link Parameters}, method arguments and
	 * {@link StringQuery}.
	 * 
	 * @param parameters must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 */
	public StringQueryParameterBinder(JpaParameters parameters, Object[] values, StringQuery query) {

		super(parameters, values);

		Assert.notNull(query, "StringQuery must not be null!");
		this.query = query;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.ParameterBinder#bind(javax.persistence.Query, org.springframework.data.repository.query.Parameter, java.lang.Object, int)
	 */
	@Override
	protected void bind(Query jpaQuery, JpaParameter methodParameter, Object value, int position) {

		ParameterBinding binding = getBindingFor(jpaQuery, position, methodParameter);
		super.bind(jpaQuery, methodParameter, binding.prepare(value), position);
	}

	/**
	 * Finds the {@link LikeParameterBinding} to be applied before binding a parameter value to the query.
	 * 
	 * @param jpaQuery must not be {@literal null}.
	 * @param position
	 * @param methodParameter must not be {@literal null}.
	 * @return the {@link ParameterBinding} for the given parameters or {@literal null} if none available.
	 */
	private ParameterBinding getBindingFor(Query jpaQuery, int position, Parameter methodParameter) {

		if (hasNamedParameter(jpaQuery)) {
			return query.getBindingFor(methodParameter.getName());
		}

		try {

			jpaQuery.getParameter(position);
			return query.getBindingFor(position);

		} catch (IllegalArgumentException o_O) {

			// We should actually reject parameters unavailable, but as EclipseLink doesn't implement ….getParameter(int) for
			// native queries correctly we need to fall back to an indexed parameter
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=427892

			return new ParameterBinding(position);
		}
	}
}
