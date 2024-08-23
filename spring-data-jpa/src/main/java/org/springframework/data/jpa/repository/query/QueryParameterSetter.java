/*
 * Copyright 2017-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import static org.springframework.data.jpa.repository.query.QueryParameterSetter.ErrorHandling.*;

import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import jakarta.persistence.criteria.ParameterExpression;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * The interface encapsulates the setting of query parameters which might use a significant number of variations of
 * {@literal Query.setParameter}.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @since 2.0
 */
interface QueryParameterSetter {

	/** Noop implementation */
	QueryParameterSetter NOOP = (query, values, errorHandler) -> {};

	/**
	 * Creates a new {@link QueryParameterSetter} for the given value extractor, JPA parameter and potentially the
	 * temporal type.
	 *
	 * @param valueExtractor
	 * @param parameter
	 * @param temporalType
	 * @return
	 */
	static QueryParameterSetter create(Function<JpaParametersParameterAccessor, Object> valueExtractor,
			Parameter<?> parameter, @Nullable TemporalType temporalType) {

		return temporalType == null ? new NamedOrIndexedQueryParameterSetter(valueExtractor, parameter)
				: new TemporalParameterSetter(valueExtractor, parameter, temporalType);
	}

	void setParameter(BindableQuery query, JpaParametersParameterAccessor accessor, ErrorHandler errorHandler);

	/**
	 * {@link QueryParameterSetter} for named or indexed parameters.
	 */
	class NamedOrIndexedQueryParameterSetter implements QueryParameterSetter {

		private final Function<JpaParametersParameterAccessor, Object> valueExtractor;
		private final Parameter<?> parameter;

		/**
		 * @param valueExtractor must not be {@literal null}.
		 * @param parameter must not be {@literal null}.
		 */
		private NamedOrIndexedQueryParameterSetter(Function<JpaParametersParameterAccessor, Object> valueExtractor,
				Parameter<?> parameter) {

			Assert.notNull(valueExtractor, "ValueExtractor must not be null");

			this.valueExtractor = valueExtractor;
			this.parameter = parameter;
		}

		@Override
		public void setParameter(BindableQuery query, JpaParametersParameterAccessor accessor, ErrorHandler errorHandler) {

			Object value = valueExtractor.apply(accessor);

			try {
				setParameter(query, value, errorHandler);
			} catch (RuntimeException e) {
				errorHandler.handleError(e);
			}
		}

		@SuppressWarnings("unchecked")
		private void setParameter(BindableQuery query, Object value, ErrorHandler errorHandler) {

			if (parameter instanceof ParameterExpression) {
				query.setParameter((Parameter<Object>) parameter, value);
			} else if (query.hasNamedParameters() && parameter.getName() != null) {
				query.setParameter(parameter.getName(), value);

			} else {

				Integer position = parameter.getPosition();

				if (position != null //
						&& (query.getParameters().size() >= position //
								|| errorHandler == LENIENT //
								|| query.registerExcessParameters())) {
					query.setParameter(position, value);
				}
			}
		}
	}

	/**
	 * {@link QueryParameterSetter} for named or indexed parameters that have a {@link TemporalType} specified.
	 */
	class TemporalParameterSetter implements QueryParameterSetter {

		private final Function<JpaParametersParameterAccessor, Object> valueExtractor;
		private final Parameter<?> parameter;
		private final TemporalType temporalType;

		private TemporalParameterSetter(Function<JpaParametersParameterAccessor, Object> valueExtractor,
				Parameter<?> parameter, TemporalType temporalType) {
			this.valueExtractor = valueExtractor;
			this.parameter = parameter;
			this.temporalType = temporalType;
		}

		@Override
		public void setParameter(BindableQuery query, JpaParametersParameterAccessor accessor, ErrorHandler errorHandler) {

			Date value = (Date) accessor.potentiallyUnwrap(valueExtractor.apply(accessor));

			try {
				setParameter(query, value, errorHandler);
			} catch (RuntimeException e) {
				errorHandler.handleError(e);
			}
		}

		@SuppressWarnings("unchecked")
		private void setParameter(BindableQuery query, Date date, ErrorHandler errorHandler) {

			// One would think we can simply use parameter to identify the parameter we want to set.
			// But that does not work with list valued parameters. At least Hibernate tries to bind them by name.
			// TODO: move to using setParameter(Parameter, value) when https://hibernate.atlassian.net/browse/HHH-11870 is
			// fixed.

			if (parameter instanceof ParameterExpression) {
				query.setParameter((Parameter<Date>) parameter, date, temporalType);
			} else if (query.hasNamedParameters() && parameter.getName() != null) {
				query.setParameter(parameter.getName(), date, temporalType);
			} else {

				Integer position = parameter.getPosition();

				if (position != null //
						&& (query.getParameters().size() >= parameter.getPosition() //
								|| query.registerExcessParameters() //
								|| errorHandler == LENIENT)) {

					query.setParameter(parameter.getPosition(), date, temporalType);
				}
			}
		}
	}

	enum ErrorHandling implements ErrorHandler {

		STRICT {

			@Override
			public void handleError(Throwable t) {
				if (t instanceof RuntimeException rx) {
					throw rx;
				}
				throw new RuntimeException(t);
			}
		},

		LENIENT {

			@Override
			public void handleError(Throwable t) {
				LOG.info("Silently ignoring", t);
			}
		};

		private static final Log LOG = LogFactory.getLog(ErrorHandling.class);
	}

	/**
	 * Metadata for a JPA {@link Query}.
	 */
	class QueryMetadata {

		private final boolean namedParameters;
		private final Set<Parameter<?>> parameters;
		private final boolean registerExcessParameters;

		QueryMetadata(Query query) {

			this.namedParameters = QueryUtils.hasNamedParameter(query);
			this.parameters = query.getParameters();

			// DATAJPA-1172
			// Since EclipseLink doesn't reliably report whether a query has parameters
			// we simply try to set the parameters and ignore possible failures.
			// this is relevant for native queries with SpEL expressions, where the method parameters don't have to match the
			// parameters in the query.
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=521915

			this.registerExcessParameters = query.getParameters().size() == 0
					&& unwrapClass(query).getName().startsWith("org.eclipse");
		}

		/**
		 * @return
		 */
		public Set<Parameter<?>> getParameters() {
			return parameters;
		}

		/**
		 * @return {@literal true} if the underlying query uses named parameters.
		 */
		public boolean hasNamedParameters() {
			return this.namedParameters;
		}

		public boolean registerExcessParameters() {
			return this.registerExcessParameters;
		}

		/**
		 * Returns the actual target {@link Query} instance, even if the provided query is a {@link Proxy} based on
		 * {@link org.springframework.orm.jpa.SharedEntityManagerCreator.DeferredQueryInvocationHandler}.
		 *
		 * @param query a {@link Query} instance, possibly a Proxy.
		 * @return the class of the actual underlying class if it can be determined, the class of the passed in instance
		 *         otherwise.
		 */
		private static Class<?> unwrapClass(Query query) {

			Class<? extends Query> queryType = query.getClass();

			try {

				return Proxy.isProxyClass(queryType) //
						? query.unwrap(null).getClass() //
						: queryType;

			} catch (RuntimeException e) {

				LogFactory.getLog(QueryMetadata.class).warn("Failed to unwrap actual class for Query proxy", e);

				return queryType;
			}
		}
	}

	/**
	 * A bindable {@link Query}.
	 */
	class BindableQuery extends QueryMetadata {

		private final Query query;
		private final Query unwrapped;

		BindableQuery(Query query) {
			super(query);
			this.query = query;
			this.unwrapped = Proxy.isProxyClass(query.getClass()) ? query.unwrap(null) : query;
		}

		public static BindableQuery from(Query query) {
			return new BindableQuery(query);
		}

		public Query getQuery() {
			return query;
		}

		public <T> Query setParameter(Parameter<T> param, T value) {
			return unwrapped.setParameter(param, value);
		}

		public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
			return unwrapped.setParameter(param, value, temporalType);
		}

		public Query setParameter(String name, Object value) {
			return unwrapped.setParameter(name, value);
		}

		public Query setParameter(String name, Date value, TemporalType temporalType) {
			return query.setParameter(name, value, temporalType);
		}

		public Query setParameter(int position, Object value) {
			return unwrapped.setParameter(position, value);
		}

		public Query setParameter(int position, Date value, TemporalType temporalType) {
			return unwrapped.setParameter(position, value, temporalType);
		}
	}
}
