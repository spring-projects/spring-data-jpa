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
import org.hibernate.query.SelectionQuery;
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

	private static final HibernateAdapter HIBERNATE_ADAPTER = HibernateAdapter.create();

	private HibernateUtils() {}

	/**
	 * Return the {@link SelectionQuery} the given query represents, or {@literal null} if it does not represent one.
	 *
	 * @since 4.2
	 */
	static @Nullable SelectionQuery<?> asSelectionQuery(jakarta.persistence.Query query) {
		return HIBERNATE_ADAPTER.asSelectionQuery(query);
	}

	/**
	 * Return the query string of the underlying native Hibernate query.
	 *
	 * @param query
	 * @return
	 */
	public @Nullable static String getHibernateQuery(Object query) {

		try {
			if (HIBERNATE_ADAPTER.isSqmQuery(query)) {

				String hql = HIBERNATE_ADAPTER.getQueryString(query);

				if (!hql.equals("<criteria>")) {
					return hql;
				}

				return HIBERNATE_ADAPTER.getSqmStatement(query);
			}

			if (HIBERNATE_ADAPTER.isNamedSqmQuery(query)) {

				String hql = HIBERNATE_ADAPTER.getHqlString(query);

				if (!hql.equals("<criteria>")) {
					return hql;
				}

				return HIBERNATE_ADAPTER.getSqmStatement(query);
			}

			if (HIBERNATE_ADAPTER.isNamedNativeQuery(query)) {
				return HIBERNATE_ADAPTER.getSqlString(query);
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

		if (HIBERNATE_ADAPTER.isSqmQuery(query)) {
			return false;
		}

		if (query instanceof NativeQuery<?>) {
			return true;
		}

		if (HIBERNATE_ADAPTER.isNamedSqmQuery(query)) {

			return false;
		}

		if (HIBERNATE_ADAPTER.isNamedNativeQuery(query)) {
			return true;
		}

		if (query instanceof jakarta.persistence.Query jpaQuery) {
			try {
				jpaQuery.unwrap(NativeQuery.class);
				return true;
			} catch (RuntimeException o_O) {
				// Not a native Hibernate query.
			}
		}

		return false;
	}
}
