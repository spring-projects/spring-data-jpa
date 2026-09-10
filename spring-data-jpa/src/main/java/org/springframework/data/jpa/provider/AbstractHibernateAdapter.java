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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hibernate.query.SelectionQuery;
import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.util.ReflectiveMethod;
import org.springframework.data.jpa.util.ReflectiveMethods;
import org.springframework.data.util.Streamable;
import org.springframework.util.ClassUtils;

/**
 * Reflective support shared by the version-specific {@link HibernateAdapter} implementations.
 *
 * @author Oscar Fanchin
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.2
 */
abstract class AbstractHibernateAdapter implements HibernateAdapter, ReflectiveMethods {

	private final Class<?> sqmQuery;
	private final Class<?> namedSqmQuery;
	private final Class<?> namedNativeQuery;

	private final ReflectiveMethod sqmQueryStatement;
	private final ReflectiveMethod namedSqmQueryHqlString;
	private final ReflectiveMethod namedSqmQueryStatement;
	private final ReflectiveMethod namedNativeQuerySqlString;

	/**
	 * @param sqmQuery candidate names of the contract implemented by SQM queries.
	 * @param namedSqmQuery candidate names of the named SQM query memento.
	 * @param namedNativeQuery candidate names of the named native query memento.
	 */
	AbstractHibernateAdapter(ClassLoader classLoader, ClassNames sqmQuery, ClassNames namedSqmQuery,
			ClassNames namedNativeQuery) {

		this.sqmQuery = sqmQuery.getClass(classLoader);
		this.namedSqmQuery = namedSqmQuery.getClass(classLoader);
		this.namedNativeQuery = namedNativeQuery.getClass(classLoader);

		this.sqmQueryStatement = getMethod(this.sqmQuery, "getSqmStatement");
		this.namedSqmQueryHqlString = getMethod(this.namedSqmQuery, "getHqlString");
		this.namedSqmQueryStatement = getMethod(this.namedSqmQuery, "getSqmStatement");
		this.namedNativeQuerySqlString = getMethod(this.namedNativeQuery, "getSqlString");
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
		return (String) namedSqmQueryHqlString.invoke(query);
	}

	@Override
	public String getSqlString(Object query) {
		return (String) namedNativeQuerySqlString.invoke(query);
	}

	@Override
	public String getSqmStatement(Object query) {

		ReflectiveMethod method = isSqmQuery(query) ? sqmQueryStatement : namedSqmQueryStatement;
		Object statement = method.invoke(query);

		if (statement == null) {
			throw new IllegalStateException("No SQM statement available for %s".formatted(query.getClass()));
		}

		ReflectiveMethod toHqlString = getMethod(statement.getClass(), "toHqlString");
		return (String) toHqlString.invoke(statement);
	}

	@Override
	public @Nullable SelectionQuery<?> asSelectionQuery(Query query) {
		return null;
	}

	@Override
	public List<ReflectiveMethod> getReflectiveMethods() {
		return List.of(sqmQueryStatement, namedSqmQueryHqlString, namedSqmQueryStatement, namedNativeQuerySqlString);
	}

	/**
	 * Resolve the method named {@code methodName} on {@code type}.
	 *
	 * @throws IllegalStateException if no such method exists on {@code type}, which indicates an unsupported Hibernate
	 *           version.
	 */
	static ReflectiveMethod getMethod(Class<?> type, String methodName) {

		ReflectiveMethod method = ReflectiveMethod.find(type, methodName, it -> true);

		if (method == null) {
			throw new IllegalStateException("Cannot resolve %s.%s(). The Hibernate version on the classpath is not supported"
					.formatted(type.getName(), methodName));
		}

		return method;
	}

	/**
	 * Collection of class name candidates.
	 */
	static class ClassNames implements Streamable<String> {

		private final List<String> classNames;

		private ClassNames(List<String> classNames) {
			this.classNames = classNames;
		}

		public static ClassNames of(String... classNames) {
			return new ClassNames(Arrays.asList(classNames));
		}

		/**
		 * Resolve the first of the given candidate types that is present.
		 *
		 * @throws IllegalStateException if none of the classNames is present.
		 */
		Class<?> getClass(ClassLoader classLoader) {

			for (String candidate : classNames) {
				if (ClassUtils.isPresent(candidate, classLoader)) {
					return ClassUtils.resolveClassName(candidate, classLoader);
				}
			}

			throw new IllegalStateException(
					"Cannot resolve any of the required classes: %s. The Hibernate version on the classpath is not supported"
							.formatted(classNames));
		}

		@Override
		public Iterator<String> iterator() {
			return classNames.iterator();
		}

		@Override
		public String toString() {
			return String.join(", ", classNames);
		}

	}
}
