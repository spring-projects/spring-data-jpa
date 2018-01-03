/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.data.jpa.provider;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Query;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility functions to work with Hibernate. Mostly using reflection to make sure common functionality can be executed
 * against all the Hibernate version we support.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.10.2
 * @soundtrack Benny Greb - Soulfood (Live, https://www.youtube.com/watch?v=9_ErMa_CtSw)
 */
public abstract class HibernateUtils {

	private static final List<String> TYPES = Arrays.asList("org.hibernate.jpa.HibernateQuery",
			"org.hibernate.ejb.HibernateQuery");
	private static final @Nullable Method GET_HIBERNATE_QUERY;
	private static final @Nullable Class<?> HIBERNATE_QUERY_INTERFACE;
	private static final @Nullable Method QUERY_STRING_METHOD;

	private HibernateUtils() {}

	static {

		Class<?> type = null;
		Method method = null;
		ClassLoader classLoader = HibernateUtils.class.getClassLoader();

		for (String typeName : TYPES) {
			try {
				type = ClassUtils.forName(typeName, classLoader);
				method = type.getMethod("getHibernateQuery");
			} catch (Exception o_O) {}
		}

		GET_HIBERNATE_QUERY = method;

		Class<?> queryInterface = null;

		try {
			queryInterface = ClassUtils.forName("org.hibernate.query.Query", classLoader);
		} catch (Exception o_O) {}

		HIBERNATE_QUERY_INTERFACE = queryInterface == null ? type : queryInterface;
		QUERY_STRING_METHOD = HIBERNATE_QUERY_INTERFACE == null ? null
				: ReflectionUtils.findMethod(HIBERNATE_QUERY_INTERFACE, "getQueryString");
	}

	/**
	 * Return the query string of the underlying native Hibernate query.
	 *
	 * @param query
	 * @return
	 */
	@Nullable
	public static String getHibernateQuery(Object query) {

		if (HIBERNATE_QUERY_INTERFACE != null && QUERY_STRING_METHOD != null
				&& HIBERNATE_QUERY_INTERFACE.isInstance(query)) {
			return String.class.cast(ReflectionUtils.invokeMethod(QUERY_STRING_METHOD, query));
		}

		if (HIBERNATE_QUERY_INTERFACE != null && !HIBERNATE_QUERY_INTERFACE.isInstance(query)) {
			query = ((javax.persistence.Query) query).unwrap(HIBERNATE_QUERY_INTERFACE);
		}

		if (GET_HIBERNATE_QUERY == null) {
			throw new IllegalStateException(
					"Cannot invoke getHibernateQuery(…). No underlying method for a reflective call found.");
		}

		Query q = (Query) ReflectionUtils.invokeMethod(GET_HIBERNATE_QUERY, query);
		return q == null ? "" : q.getQueryString();
	}
}
