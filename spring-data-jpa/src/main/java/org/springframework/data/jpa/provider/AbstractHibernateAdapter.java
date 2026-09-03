/*
 * Copyright 2026-present the original author or authors.
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

import jakarta.persistence.Query;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.hibernate.query.SelectionQuery;
import org.jspecify.annotations.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Reflective support shared by the version-specific {@link HibernateAdapter} implementations.
 *
 * @author Oscar Fanchin
 * @author Christoph Strobl
 * @since 4.2
 */
abstract class AbstractHibernateAdapter implements HibernateAdapter {

	private final Class<?> sqmQuery;
	private final Class<?> namedSqmQuery;
	private final Class<?> namedNativeQuery;

	private final Method sqmQueryStatement;
	private final Method namedSqmQueryHqlString;
	private final Method namedSqmQueryStatement;
	private final Method namedNativeQuerySqlString;

	/**
	 * @param sqmQuery candidate names of the contract implemented by SQM queries.
	 * @param namedSqmQuery candidate names of the named SQM query memento.
	 * @param namedNativeQuery candidate names of the named native query memento.
	 */
	AbstractHibernateAdapter(ClassLoader classLoader, List<String> sqmQuery, List<String> namedSqmQuery,
			List<String> namedNativeQuery) {

		this.sqmQuery = requireClass(sqmQuery, classLoader);
		this.namedSqmQuery = requireClass(namedSqmQuery, classLoader);
		this.namedNativeQuery = requireClass(namedNativeQuery, classLoader);

		this.sqmQueryStatement = requireMethod(this.sqmQuery, "getSqmStatement");
		this.namedSqmQueryHqlString = requireMethod(this.namedSqmQuery, "getHqlString");
		this.namedSqmQueryStatement = requireMethod(this.namedSqmQuery, "getSqmStatement");
		this.namedNativeQuerySqlString = requireMethod(this.namedNativeQuery, "getSqlString");
	}

	@Override
	public boolean isSqmQuery(Object query) {
		return sqmQuery.isInstance(query);
	}

	@Override
	public boolean isNamedSqmQuery(Object query) {
		return namedSqmQuery.isInstance(query);
	}

	@Override
	public boolean isNamedNativeQuery(Object query) {
		return namedNativeQuery.isInstance(query);
	}

	@Override
	public String getQueryString(Object query) {

		// Hibernate 8 dropped the query string from the SQM contract, but every query implementation is a Query
		if (query instanceof org.hibernate.query.Query<?> hibernateQuery) {
			return hibernateQuery.getQueryString();
		}

		throw new IllegalStateException("Cannot obtain the query string from %s".formatted(query.getClass()));
	}

	@Override
	public String getHqlString(Object query) {
		return (String) ReflectionUtils.invokeMethod(namedSqmQueryHqlString, query);
	}

	@Override
	public String getSqlString(Object query) {
		return (String) ReflectionUtils.invokeMethod(namedNativeQuerySqlString, query);
	}

	@Override
	public String getSqmStatement(Object query) {

		Method accessor = isSqmQuery(query) ? sqmQueryStatement : namedSqmQueryStatement;
		Object statement = ReflectionUtils.invokeMethod(accessor, query);

		if (statement == null) {
			throw new IllegalStateException("No SQM statement available for %s".formatted(query.getClass()));
		}

		Method toHqlString = findPublicMethod(statement.getClass(), "toHqlString");

		if (toHqlString == null) {
			throw new IllegalStateException("Cannot resolve toHqlString() for %s".formatted(statement.getClass()));
		}

		return (String) ReflectionUtils.invokeMethod(toHqlString, statement);
	}

	@Override
	public @Nullable SelectionQuery<?> asSelectionQuery(Query query) {
		return null;
	}

	/**
	 * Resolve the given method preferring a public interface as the declaring type so that the member does not have to be
	 * made accessible. Hibernate's implementation types live in internal packages and are not necessarily public.
	 */
	private static @Nullable Method findPublicMethod(Class<?> type, String methodName) {

		for (Class<?> candidate : ClassUtils.getAllInterfacesForClass(type)) {

			Method method = Modifier.isPublic(candidate.getModifiers()) ? ReflectionUtils.findMethod(candidate, methodName)
					: null;

			if (method != null) {
				return method;
			}
		}

		Method method = ReflectionUtils.findMethod(type, methodName);

		if (method != null) {
			ReflectionUtils.makeAccessible(method);
		}

		return method;
	}

	/**
	 * Resolve the first of the given candidate types that is present.
	 *
	 * @throws IllegalStateException if none of the candidates is present.
	 */
	static Class<?> requireClass(List<String> candidates, ClassLoader classLoader) {

		for (String candidate : candidates) {

			if (ClassUtils.isPresent(candidate, classLoader)) {
				return ClassUtils.resolveClassName(candidate, classLoader);
			}
		}

		throw new IllegalStateException(
				"Cannot resolve any of %s. The Hibernate version on the classpath is not supported".formatted(candidates));
	}

	/**
	 * @throws IllegalStateException if the given type does not declare a parameterless method of that name.
	 */
	static Method requireMethod(Class<?> type, String methodName) {

		Method method = ReflectionUtils.findMethod(type, methodName);

		if (method == null) {
			throw new IllegalStateException("Cannot resolve %s.%s(). The Hibernate version on the classpath is not supported"
					.formatted(type.getName(), methodName));
		}

		return method;
	}
}
