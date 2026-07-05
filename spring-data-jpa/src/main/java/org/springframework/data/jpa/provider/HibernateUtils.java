/*
 * Copyright 2016-present the original author or authors.
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
package org.springframework.data.jpa.provider;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.internal.AbstractSqmQuery;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
import org.jspecify.annotations.Nullable;

/**
 * Utility functions to work with Hibernate. Mostly using reflection to make sure common functionality can be executed
 * against all the Hibernate version we support.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Donghun Shin
 * @author Greg Turnquist
 * @author Oscar Fanchin
 * @since 1.10.2
 * @soundtrack Benny Greb - Soulfood (Live, https://www.youtube.com/watch?v=9_ErMa_CtSw)
 */
public abstract class HibernateUtils {

	private HibernateUtils() {}

	/**
	 * Return the query string of the underlying native Hibernate query.
	 *
	 * @param query
	 * @return
	 */
	public @Nullable static String getHibernateQuery(Object query) {

		try {
			// Try the new Hibernate implementation first
			if (query instanceof AbstractSqmQuery sqmQuery) {

				String hql = sqmQuery.getQueryString();

				if (!hql.equals("<criteria>")) {
					return hql;
				}

				return sqmQuery.getSqmStatement().toHqlString();
			}

			// Try the new Hibernate implementation first
			if (query instanceof NamedSqmQueryMemento<?> sqmQuery) {

				String hql = sqmQuery.getHqlString();

				if (!hql.equals("<criteria>")) {
					return hql;
				}

				return sqmQuery.getSqmStatement().toHqlString();
			}

			if (query instanceof NamedNativeQueryMemento<?> nativeQuery) {
				return nativeQuery.getSqlString();
			}

			// Couple of cases in which this still breaks, see HHH-15389
		} catch (RuntimeException o_O) {}

		// Try the old way, as it still works in some cases (haven't investigated in which exactly)
		if (query instanceof Query<?> hibernateQuery) {
			return hibernateQuery.getQueryString();
		} else {
			throw new IllegalArgumentException("Don't know how to extract the query string from " + query);
		}
	}

	public static boolean isNativeQuery(Object query) {

		// Try the new Hibernate implementation first
		if (query instanceof AbstractSqmQuery<?>) {
			return false;
		}

		if (query instanceof NativeQuery<?>) {
			return true;
		}

		// Try the new Hibernate implementation first
		if (query instanceof NamedSqmQueryMemento<?>) {

			return false;
		}

		if (query instanceof NamedNativeQueryMemento<?>) {
			return true;
		}
		// This change handles the query proxying mechanism introduced in Spring Framework 7.0.9 and 7.1.
		// See https://github.com/spring-projects/spring-framework/issues/36878.
		if (query instanceof jakarta.persistence.Query jpaQuery) {
			try {
				jpaQuery.unwrap(NativeQuery.class);
				return true;
			} catch (Exception ignored) {
				// Not a native Hibernate query; try the general Hibernate query type.
			}

			try {
				jpaQuery.unwrap(Query.class);
				return false;
			} catch (Exception ignored) {
				// Not an unwrap-compatible Hibernate query.
			}
		}

		return false;
	}
}
