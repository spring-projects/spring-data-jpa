/*
 * Copyright 2016-2019 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Query;
import org.springframework.data.util.Version;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility functions to work with Hibernate. Mostly using reflection to make sure common functionality can be executed
 * against all the Hibernate version we support.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @since 1.10.2
 * @soundtrack Benny Greb - Soulfood (Live, https://www.youtube.com/watch?v=9_ErMa_CtSw)
 */
@SuppressWarnings({ "deprecation", "rawtypes" })
public abstract class HibernateUtils {

	private static final List<String> TYPES = Arrays.asList("org.hibernate.jpa.HibernateQuery",
			"org.hibernate.ejb.HibernateQuery");

	private static final Version HIBERNATE4_VERSION_SUPPORTING_TUPLES = new Version(4, 2, 21);
	private static final Version HIBERNATE5_VERSION = new Version(5, 0, 0);
	private static final Version HIBERNATE5_VERSION_SUPPORTING_TUPLES = new Version(5, 2, 11);

	private static final Method GET_HIBERNATE_QUERY;

	private static final Class<?> HIBERNATE_QUERY_INTERFACE;
	private static final Method QUERY_STRING_METHOD;
	private static final Version HIBERNATE_VERSION;

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

		String versionSource = org.hibernate.Version.getVersionString();

		HIBERNATE_VERSION = Version.parse(versionSource.substring(0, versionSource.lastIndexOf('.')));
	}

	/**
	 * Return the query string of the underlying native Hibernate query.
	 * 
	 * @param query
	 * @return
	 */
	public static String getHibernateQuery(Object query) {

		if (HIBERNATE_QUERY_INTERFACE != null && QUERY_STRING_METHOD != null
				&& HIBERNATE_QUERY_INTERFACE.isInstance(query)) {
			return String.class.cast(ReflectionUtils.invokeMethod(QUERY_STRING_METHOD, query));
		}

		if (HIBERNATE_QUERY_INTERFACE != null && !HIBERNATE_QUERY_INTERFACE.isInstance(query)) {
			query = ((javax.persistence.Query) query).unwrap(HIBERNATE_QUERY_INTERFACE);
		}

		return ((Query) ReflectionUtils.invokeMethod(GET_HIBERNATE_QUERY, query)).getQueryString();
	}

	/**
	 * Returns whether the currently used version of Hibernate is equal to or newer than the given one.
	 *
	 * @param version must not be {@literal null}.
	 * @return
	 */
	public static boolean isVersionOrBetter(Version version) {
		return HIBERNATE_VERSION.isGreaterThanOrEqualTo(version);
	}

	/**
	 * Returns whether the currently used version of Hibernate is in the given interval of versions.
	 *
	 * @param lowerIncluding lower version bound to compare to. The lower version bound is inclusive. Must not be
	 *          {@literal null}.
	 * @param upperExcluding upper version bound to compare to. The upper version bound is exclusive. Must not be
	 *          {@literal null}.
	 * @return whence lowerIncluding <= Hibernate version < upperExcluding.
	 */
	private static boolean isVersionInInterval(Version lowerIncluding, Version upperExcluding) {
		return HIBERNATE_VERSION.isGreaterThanOrEqualTo(lowerIncluding) && HIBERNATE_VERSION.isLessThan(upperExcluding);
	}

	/**
	 * Returns wether the current version of Hibernate supports {@link javax.persistence.Tuple} as a return type for
	 * native queries.
	 */
	public static boolean supportsTuples() {

		return HibernateUtils.isVersionInInterval(HIBERNATE4_VERSION_SUPPORTING_TUPLES, HIBERNATE5_VERSION)
				|| HibernateUtils.isVersionOrBetter(HIBERNATE5_VERSION_SUPPORTING_TUPLES);
	}

	/**
	 * Returns wether the current version of Hibernate supports {@link javax.persistence.Tuple} as a return type for
	 * native queries.
	 */
	public static boolean supportsTuplesForNativeQueries() {

		return HibernateUtils.isVersionOrBetter(HIBERNATE5_VERSION_SUPPORTING_TUPLES);
	}
}
