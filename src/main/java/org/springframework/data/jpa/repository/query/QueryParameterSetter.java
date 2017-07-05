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

import java.util.Date;

import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.criteria.ParameterExpression;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;

/**
 * The interface encapsulates the setting of query parameters which might use a significant number of variations of
 * {@literal Query.setParameter}.
 *
 * @author Jens Schauder
 * @since 2.0
 */
interface QueryParameterSetter {

	void setParameter(Query query);

	/** Noop implementation */
	QueryParameterSetter NOOP = query -> {};

	/**
	 * Creates a {@link QueryParameterSetter} from a {@link JpaParameter}. Handles named and indexed parameters and
	 * TemporalType annotations.
	 *
	 * @param parameter the method parameter to bind
	 * @param position position of the bind parameter, might be {@literal null} only if parameters are named.
	 * @param value the value passed to the method parameter.
	 */
	static QueryParameterSetter fromJpaParameter(JpaParameter parameter, Integer position, Object value) {

		TemporalType temporalType = parameter.isTemporalParameter() ? parameter.getTemporalType() : null;
		String name = parameter.isNamedParameter()
				? parameter.getName().orElseThrow(() -> new IllegalArgumentException("o_O parameter needs to have a name!"))
				: null;
		return new NamedOrIndexedQueryParameterSetter(name, position, temporalType, value);
	}

	/**
	 * Creates a {@link QueryParameterSetter} which ignores {@link IllegalArgumentException}s when calling
	 * {@literal setParameter}. Useful because certain JPA implementations do not correctly report the presence of
	 * parameters in a query.
	 *
	 * @param name the name of the parameter to bind. Might be {@literal null}.
	 * @param position position of the bind parameter, might be {@literal null} only if parameters are named.
	 * @param value the value passed to the method parameter.
	 */
	static QueryParameterSetter lenientQueryParameterSetter(String name, Integer position, Object value) {

		return (Query q) -> {
			try {
				if (name != null) {
					q.setParameter(name, value);
				} else {
					q.setParameter(position, value);
				}
			} catch (IllegalArgumentException iae) {

				// Since Eclipse doesn't reliably report whether a query has parameters
				// we simply try to set the parameters and ignore possible failures.
			}
		};
	}

	/**
	 * {@link QueryParameterSetter} for named or indexed parameters that might have a {@link TemporalType} specified.
	 */
	class NamedOrIndexedQueryParameterSetter implements QueryParameterSetter {

		private final boolean useParameterAccessByName;
		private final String name;
		private final Integer index;
		private final TemporalType temporalType;
		private final Object value;

		/**
		 * @param name of the parameter, if {@literal null} index based parameter access will be used.
		 * @param index index of the parameter. Only used when {@literal name) is {@literal null}
		 * @param temporalType can be {@literal null}
		 * @param value the value to be set, might be {@literal null}
		 */
		NamedOrIndexedQueryParameterSetter(String name, Integer index, TemporalType temporalType, Object value) {

			this.useParameterAccessByName = name != null;
			this.name = name;
			this.index = index;
			this.temporalType = temporalType;
			this.value = value;
		}

		public void setParameter(Query query) {

			if (temporalType != null) {

				if (useParameterAccessByName && QueryUtils.hasNamedParameter(query)) {
					query.setParameter(name, (Date) value, temporalType);
				} else {
					query.setParameter(index, (Date) value, temporalType);
				}
			} else {

				if (useParameterAccessByName && QueryUtils.hasNamedParameter(query)) {
					query.setParameter(name, value);
				} else {
					query.setParameter(index, value);
				}
			}
		}
	}

	/**
	 * {@link QueryParameterSetter} identifying parameters by {@link ParameterExpression}
	 */
	class ParameterExpressionQueryParameterSetter implements QueryParameterSetter {

		private final ParameterExpression parameterExpression;
		private final TemporalType temporalType;
		private final Object value;

		ParameterExpressionQueryParameterSetter(ParameterExpression parameterExpression, TemporalType temporalType,
				Object value) {

			this.temporalType = temporalType;
			this.value = value;
			this.parameterExpression = parameterExpression;
		}

		@SuppressWarnings("unchecked")
		public void setParameter(Query query) {
			if (temporalType != null) {

				query.setParameter(parameterExpression, (Date) value, temporalType);
			} else {

				query.setParameter(parameterExpression, value);
			}
		}
	}
}
