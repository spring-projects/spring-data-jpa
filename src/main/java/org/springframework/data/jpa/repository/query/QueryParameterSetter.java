/*
 * Copyright 2017-2019 the original author or authors.
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

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.function.Function;

import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.criteria.ParameterExpression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	void setParameter(Query query, Object[] values, ErrorHandling errorHandling);

	/** Noop implementation */
	QueryParameterSetter NOOP = (query, values, errorHandling) -> {};

	/**
	 * {@link QueryParameterSetter} for named or indexed parameters that might have a {@link TemporalType} specified.
	 */
	class NamedOrIndexedQueryParameterSetter implements QueryParameterSetter {

		private static final Logger LOGGER = LoggerFactory.getLogger(NamedOrIndexedQueryParameterSetter.class);

		private final Function<Object[], Object> valueExtractor;
		private final Parameter<?> parameter;
		private final @Nullable TemporalType temporalType;

		/**
		 * @param valueExtractor must not be {@literal null}.
		 * @param parameter must not be {@literal null}.
		 * @param temporalType may be {@literal null}.
		 */
		NamedOrIndexedQueryParameterSetter(Function<Object[], Object> valueExtractor, Parameter<?> parameter,
				@Nullable TemporalType temporalType) {

			Assert.notNull(valueExtractor, "ValueExtractor must not be null!");

			this.valueExtractor = valueExtractor;
			this.parameter = parameter;
			this.temporalType = temporalType;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.QueryParameterSetter#setParameter(javax.persistence.Query, java.lang.Object[])
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void setParameter(Query query, Object[] values, ErrorHandling errorHandling) {

			Object value = valueExtractor.apply(values);

			if (temporalType != null) {

				// One would think we can simply use parameter to identify the parameter we want to set.
				// But that does not work with list valued parameters. At least Hibernate tries to bind them by name.
				// TODO: move to using setParameter(Parameter, value) when https://hibernate.atlassian.net/browse/HHH-11870 is
				// fixed.

				if (parameter instanceof ParameterExpression) {
					errorHandling.execute(() -> query.setParameter((Parameter<Date>) parameter, (Date) value, temporalType));
				} else if (parameter.getName() != null && QueryUtils.hasNamedParameter(query)) {
					errorHandling.execute(() -> query.setParameter(parameter.getName(), (Date) value, temporalType));
				} else {

					Integer position = parameter.getPosition();

					if (position != null //
							&& (query.getParameters().size() >= parameter.getPosition() //
									|| registerExcessParameters(query) //
									|| errorHandling == LENIENT)) {

						errorHandling.execute(() -> query.setParameter(parameter.getPosition(), (Date) value, temporalType));
					}
				}

			} else {

				if (parameter instanceof ParameterExpression) {
					errorHandling.execute(() -> query.setParameter((Parameter<Object>) parameter, value));
				} else if (parameter.getName() != null && QueryUtils.hasNamedParameter(query)) {
					errorHandling.execute(() -> query.setParameter(parameter.getName(), value));

				} else {

					Integer position = parameter.getPosition();

					if (position != null //
							&& (query.getParameters().size() >= position //
									|| errorHandling == LENIENT //
									|| registerExcessParameters(query))) {

						errorHandling.execute(() -> query.setParameter(position, value));
					}
				}
			}
		}

		private boolean registerExcessParameters(Query query) {

			// DATAJPA-1172
			// Since EclipseLink doesn't reliably report whether a query has parameters
			// we simply try to set the parameters and ignore possible failures.
			// this is relevant for native queries with SpEL expressions, where the method parameters don't have to match the
			// parameters in the query.
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=521915

			return query.getParameters().size() == 0 && unwrapClass(query).getName().startsWith("org.eclipse");
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

				LOGGER.warn("Failed to unwrap actual class for Query proxy.", e);

				return queryType;
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

		private static final Logger LOG = LoggerFactory.getLogger(ErrorHandling.class);

		abstract void execute(Runnable block);
	}
}
