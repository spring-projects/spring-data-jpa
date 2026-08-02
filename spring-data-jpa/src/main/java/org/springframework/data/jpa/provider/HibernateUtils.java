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
import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

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

	private static final HibernateQueryAdapter HIBERNATE_QUERY_ADAPTER = new HibernateQueryAdapter();

	private HibernateUtils() {}

	/**
	 * Return the query string of the underlying native Hibernate query.
	 *
	 * @param query
	 * @return
	 */
	public @Nullable static String getHibernateQuery(Object query) {

		try {
			if (HIBERNATE_QUERY_ADAPTER.isSqmQuery(query)) {

				String hql = HIBERNATE_QUERY_ADAPTER.getQueryString(query);

				if (!hql.equals("<criteria>")) {
					return hql;
				}

				return HIBERNATE_QUERY_ADAPTER.getSqmStatement(query);
			}

			if (HIBERNATE_QUERY_ADAPTER.isNamedSqmQuery(query)) {

				String hql = HIBERNATE_QUERY_ADAPTER.getHqlString(query);

				if (!hql.equals("<criteria>")) {
					return hql;
				}

				return HIBERNATE_QUERY_ADAPTER.getSqmStatement(query);
			}

			if (HIBERNATE_QUERY_ADAPTER.isNamedNativeQuery(query)) {
				return HIBERNATE_QUERY_ADAPTER.getSqlString(query);
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

		if (HIBERNATE_QUERY_ADAPTER.isSqmQuery(query)) {
			return false;
		}

		if (query instanceof NativeQuery<?>) {
			return true;
		}

		if (HIBERNATE_QUERY_ADAPTER.isNamedSqmQuery(query)) {

			return false;
		}

		if (HIBERNATE_QUERY_ADAPTER.isNamedNativeQuery(query)) {
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

	private static final class HibernateQueryAdapter {

		private static final ClassLoader CLASS_LOADER = HibernateUtils.class.getClassLoader();

		private final @Nullable Class<?> sqmQuery = loadClass("org.hibernate.query.sqm.spi.SqmStatementAccess",
				"org.hibernate.query.spi.SqmQuery");
		private final @Nullable Class<?> namedSqmQuery = loadClass("org.hibernate.query.named.spi.NamedSqmQueryMemento",
				"org.hibernate.query.sqm.spi.NamedSqmQueryMemento");
		private final @Nullable Class<?> namedNativeQuery = loadClass(
				"org.hibernate.query.named.spi.NamedNativeQueryMemento",
				"org.hibernate.query.sql.spi.NamedNativeQueryMemento");

		boolean isSqmQuery(Object query) {
			return isInstance(sqmQuery, query);
		}

		boolean isNamedSqmQuery(Object query) {
			return isInstance(namedSqmQuery, query);
		}

		boolean isNamedNativeQuery(Object query) {
			return isInstance(namedNativeQuery, query);
		}

		String getQueryString(Object query) {
			return invokeStringMethod(query, "getQueryString");
		}

		String getHqlString(Object query) {
			return invokeStringMethod(query, "getHqlString");
		}

		String getSqlString(Object query) {
			return invokeStringMethod(query, "getSqlString");
		}

		String getSqmStatement(Object query) {
			return invokeStringMethod(invokeMethod(query, "getSqmStatement"), "toHqlString");
		}

		private static boolean isInstance(@Nullable Class<?> type, Object query) {
			return type != null && type.isInstance(query);
		}

		private static String invokeStringMethod(Object target, String methodName) {
			return (String) invokeMethod(target, methodName);
		}

		private static Object invokeMethod(Object target, String methodName) {

			var method = ReflectionUtils.findMethod(target.getClass(), methodName);

			if (method == null) {
				throw new IllegalStateException("Cannot resolve method %s on %s".formatted(methodName, target.getClass()));
			}

			ReflectionUtils.makeAccessible(method);

			return ReflectionUtils.invokeMethod(method, target);
		}

		private static @Nullable Class<?> loadClass(String... classNames) {

			for (String className : classNames) {
				if (ClassUtils.isPresent(className, CLASS_LOADER)) {
					return ClassUtils.resolveClassName(className, CLASS_LOADER);
				}
			}

			return null;
		}
	}
}
