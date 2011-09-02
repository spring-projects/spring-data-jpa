/*
 * Copyright 2011 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.persistence.Query;
import javax.persistence.criteria.ParameterExpression;

import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Special {@link ParameterBinder} that uses {@link ParameterExpression}s to bind query parameters.
 * 
 * @author Oliver Gierke
 */
class CriteriaQueryParameterBinder extends ParameterBinder {

	private final Iterator<ParameterExpression<?>> expressions;

	/**
	 * Creates a new {@link CriteriaQueryParameterBinder} for the given {@link Parameters}, values and some
	 * {@link ParameterExpression}.
	 * 
	 * @param parameters
	 */
	CriteriaQueryParameterBinder(Parameters parameters, Object[] values, Iterable<ParameterExpression<?>> expressions) {

		super(parameters, values);
		Assert.notNull(expressions);
		this.expressions = expressions.iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.jpa.repository.query.ParameterBinder#bind(javax
	 * .persistence.Query, org.springframework.data.repository.query.Parameter,
	 * java.lang.Object, int)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void bind(Query query, Parameter parameter, Object value, int position) {

		ParameterExpression<Object> expression = (ParameterExpression<Object>) expressions.next();

		Object valueToBind = Collection.class.equals(expression.getJavaType()) ? toCollection(value) : value;

		query.setParameter(expression, valueToBind);
	}

	/**
	 * Return sthe given argument as {@link Collection} which means it will return it as is if it's a {@link Collections},
	 * turn an array into an {@link ArrayList} or simply wrap any other value into a single element {@link Collections}.
	 * 
	 * @param value
	 * @return
	 */
	private static Collection<?> toCollection(Object value) {

		if (value == null) {
			return null;
		}

		if (value instanceof Collection) {
			return (Collection<?>) value;
		}

		if (ObjectUtils.isArray(value)) {
			return Arrays.asList(ObjectUtils.toObjectArray(value));
		}

		return Collections.singleton(value);
	}
}
