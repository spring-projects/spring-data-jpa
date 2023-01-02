/*
 * Copyright 2017-2023 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.QueryParameterSetter.ErrorHandling.LENIENT;

import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import jakarta.persistence.criteria.ParameterExpression;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.query.TypedParameterValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * The interface encapsulates the setting of query parameters which might use a significant number of variations of
 * {@literal Query.setParameter}.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @since 2.0
 */
interface QueryParameterSetter {

	void setParameter(BindableQuery query, JpaParametersParameterAccessor accessor, ErrorHandling errorHandling);

	/** Noop implementation */
	QueryParameterSetter NOOP = (query, values, errorHandling) -> {};

	/**
	 * {@link QueryParameterSetter} for named or indexed parameters that might have a {@link TemporalType} specified.
	 */
	class NamedOrIndexedQueryParameterSetter implements QueryParameterSetter {

		private final Function<JpaParametersParameterAccessor, Object> valueExtractor;
		private final Parameter<?> parameter;
		private final @Nullable TemporalType temporalType;

		/**
		 * @param valueExtractor must not be {@literal null}.
		 * @param parameter must not be {@literal null}.
		 * @param temporalType may be {@literal null}.
		 */
		NamedOrIndexedQueryParameterSetter(Function<JpaParametersParameterAccessor, Object> valueExtractor,
				Parameter<?> parameter, @Nullable TemporalType temporalType) {

			Assert.notNull(valueExtractor, "ValueExtractor must not be null");

			this.valueExtractor = valueExtractor;
			this.parameter = parameter;
			this.temporalType = temporalType;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setParameter(BindableQuery query, JpaParametersParameterAccessor accessor,
				ErrorHandling errorHandling) {

			if (temporalType != null) {

				var extractedValue = valueExtractor.apply(accessor);

				final Date value = (extractedValue instanceof TypedParameterValue<?> typedParameterValue)
						? (Date) typedParameterValue.getValue()
						: (Date) extractedValue;

				// One would think we can simply use parameter to identify the parameter we want to set.
				// But that does not work with list valued parameters. At least Hibernate tries to bind them by name.
				// TODO: move to using setParameter(Parameter, value) when https://hibernate.atlassian.net/browse/HHH-11870 is
				// fixed.

				if (parameter instanceof ParameterExpression) {
					errorHandling.execute(() -> query.setParameter((Parameter<Date>) parameter, value, temporalType));
				} else if (query.hasNamedParameters() && parameter.getName() != null) {
					errorHandling.execute(() -> query.setParameter(parameter.getName(), value, temporalType));
				} else {

					Integer position = parameter.getPosition();

					if (position != null //
							&& (query.getParameters().size() >= parameter.getPosition() //
									|| query.registerExcessParameters() //
									|| errorHandling == LENIENT)) {

						errorHandling.execute(() -> query.setParameter(parameter.getPosition(), value, temporalType));
					}
				}

			} else {

				final Object value = valueExtractor.apply(accessor);

				if (parameter instanceof ParameterExpression) {
					errorHandling.execute(() -> query.setParameter((Parameter<Object>) parameter, value));
				} else if (query.hasNamedParameters() && parameter.getName() != null) {
					errorHandling.execute(() -> query.setParameter(parameter.getName(), value));

				} else {

					Integer position = parameter.getPosition();

					if (position != null //
							&& (query.getParameters().size() >= position //
									|| errorHandling == LENIENT //
									|| query.registerExcessParameters())) {
						errorHandling.execute(() -> query.setParameter(position, value));
					}
				}
			}
		}
	}

	enum ErrorHandling {

		STRICT {

			@Override
			public void execute(Runnable block) {
				block.run();
			}
		},

		LENIENT {

			@Override
			public void execute(Runnable block) {

				try {
					block.run();
				} catch (RuntimeException rex) {
					LOG.info("Silently ignoring", rex);
				}
			}
		};

		private static final Log LOG = LogFactory.getLog(ErrorHandling.class);

		abstract void execute(Runnable block);
	}

	/**
	 * Cache for {@link QueryMetadata}. Optimizes for small cache sizes on a best-effort basis.
	 */
	class QueryMetadataCache {

		private Map<String, QueryMetadata> cache = Collections.emptyMap();

		/**
		 * Retrieve the {@link QueryMetadata} for a given {@code cacheKey}.
		 *
		 * @param cacheKey
		 * @param query
		 * @return
		 */
		public QueryMetadata getMetadata(String cacheKey, Query query) {

			QueryMetadata queryMetadata = cache.get(cacheKey);

			if (queryMetadata == null) {

				queryMetadata = new QueryMetadata(query);

				Map<String, QueryMetadata> cache;

				if (this.cache.isEmpty()) {
					cache = Collections.singletonMap(cacheKey, queryMetadata);
				} else {
					cache = new HashMap<>(this.cache);
					cache.put(cacheKey, queryMetadata);
				}

				synchronized (this) {
					this.cache = cache;
				}
			}

			return queryMetadata;
		}
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

		QueryMetadata(QueryMetadata metadata) {

			this.namedParameters = metadata.namedParameters;
			this.parameters = metadata.parameters;
			this.registerExcessParameters = metadata.registerExcessParameters;
		}

		/**
		 * Create a {@link BindableQuery} for a {@link Query}.
		 *
		 * @param query
		 * @return
		 */
		public BindableQuery withQuery(Query query) {
			return new BindableQuery(this, query);
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

		BindableQuery(QueryMetadata metadata, Query query) {
			super(metadata);
			this.query = query;
			this.unwrapped = Proxy.isProxyClass(query.getClass()) ? query.unwrap(null) : query;
		}

		private BindableQuery(Query query) {
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
