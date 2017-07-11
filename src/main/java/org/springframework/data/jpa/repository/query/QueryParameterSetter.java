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
import java.util.function.Function;

import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.criteria.ParameterExpression;

import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;

import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * The interface encapsulates the setting of query parameters which might use a significant number of variations of
 * {@literal Query.setParameter}.
 *
 * @author Jens Schauder
 * @since 2.0
 */
interface QueryParameterSetter {

	void setParameter(Query query, Object[] values);

	/** Noop implementation */
	QueryParameterSetter NOOP = (query, values) -> {};

	/**
	 * Creates a {@link QueryParameterSetter} which ignores {@link IllegalArgumentException}s when calling
	 * {@literal setParameter}. Useful because certain JPA implementations do not correctly report the presence of
	 * parameters in a query.
	 *
	 * @param valueExtractor function that converts the list of all method parameters to the value used for setting the
	 *          query parameter.
	 * @param binding the binding of the query parameter to set.
	 * @return QueryParameterSetter that can set the appropriate query parameter given a list of values for the method
	 *         parameters.
	 */
	static QueryParameterSetter createLenient(Function<Object[], Object> valueExtractor, ParameterBinding binding) {
		return create(valueExtractor, binding, null, true);
	}

	/**
	 * Creates a {@link QueryParameterSetter} which uses information based on the {@link JpaParameter} passed as an
	 * argument in order to fine tune the way the parameter is set.
	 *
	 * @param valueExtractor function that converts the list of all method parameters to the value used for setting the
	 *          query parameter.
	 * @param binding The binding of the query parameter to set.
	 * @param parameter The name of this method parameter is used for setting the query parameter and also TemporalType
	 *          annotations on that parameter is used when setting the query parameter.
	 * @return QueryParameterSetter that can set the appropriate query parameter given a list of values for the method
	 *         parameters.
	 */
	static QueryParameterSetter create(Function<Object[], Object> valueExtractor, ParameterBinding binding,
			JpaParameter parameter) {
		return create(valueExtractor, binding, parameter, false);
	}

	static String getName(JpaParameter parameter, ParameterBinding binding) {

		if (parameter != null)
			return parameter.isNamedParameter()
					? parameter.getName().orElseThrow(() -> new IllegalArgumentException("o_O parameter needs to have a name!"))
					: null;

		return binding.getName();
	}

	/**
	 * Creates a {@link QueryParameterSetter} from a {@link JpaParameter}. Handles named and indexed parameters,
	 * TemporalType annotations and might ignore certain exception when requested to do so.
	 *
	 * @param valueExtractor extracts the relevant value from an array of method parameter values.
	 * @param binding the binding of the query parameter to be set.
	 * @param parameter the method parameter to bind.
	 * @param lenient when true certain exceptions thrown when setting the query parameters get ignored.
	 */
	static QueryParameterSetter create(Function<Object[], Object> valueExtractor, ParameterBinding binding,
			JpaParameter parameter, boolean lenient) {

		TemporalType temporalType = parameter != null && parameter.isTemporalParameter() ? parameter.getTemporalType()
				: null;

		return new NamedOrIndexedQueryParameterSetter(valueExtractor.andThen(binding::prepare),
				createParameter(binding, parameter), temporalType, lenient);
	}

	static Parameter createParameter(ParameterBinding binding, JpaParameter parameter) {
		return new ParameterImpl(parameter, binding);
	}

	/**
	 * {@link QueryParameterSetter} for named or indexed parameters that might have a {@link TemporalType} specified.
	 */
	@Value
	class NamedOrIndexedQueryParameterSetter implements QueryParameterSetter {

		private final Function<Object[], Object> valueExtractor;
		private final Parameter parameter;
		private final TemporalType temporalType;
		private final boolean lenient;

		@SuppressWarnings("unchecked")
		public void setParameter(Query query, Object[] values) {

			Object value = valueExtractor.apply(values);

			try {
				if (temporalType != null) {
					// one would think we can simply use parameter to identify the parameter we want to set.
					// But that does not work with list valued parameters. At least Hibernate tries to bind them by name.
					// TODO: move to using setParameter(Parameter, value) when https://hibernate.atlassian.net/browse/HHH-11870 is
					// fixed.
					if (parameter instanceof ParameterExpression) {
						query.setParameter(parameter, (Date) value, temporalType);
					} else if (parameter.getName() != null && QueryUtils.hasNamedParameter(query)) {
						query.setParameter(parameter.getName(), (Date) value, temporalType);
					} else {
						query.setParameter(parameter.getPosition(), (Date) value, temporalType);
					}
				} else {

					if (parameter instanceof ParameterExpression) {
						query.setParameter(parameter, value);
					} else if (parameter.getName() != null && QueryUtils.hasNamedParameter(query)) {
						query.setParameter(parameter.getName(), value);
					} else {
						query.setParameter(parameter.getPosition(), value);
					}
				}
			} catch (IllegalArgumentException iae) {
				if (!lenient) {
					throw iae;
				}
				// Since Eclipse doesn't reliably report whether a query has parameters
				// we simply try to set the parameters and ignore possible failures.
				// this is relevant for queries with SpEL expressions, where the method parameters don't have to match the
				// parameters in the query.
			}
		}
	}

	@Value
	@RequiredArgsConstructor
	class ParameterImpl implements Parameter {

		private final String name;
		private final Integer position;
		private final Class parameterType;

		ParameterImpl(JpaParameter parameter, ParameterBinding binding) {

			this( //
					QueryParameterSetter.getName(parameter, binding), //
					binding.getPosition(), //
					parameter == null ? Object.class : parameter.getType()); //
		}
	}
}
