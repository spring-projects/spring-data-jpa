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
import java.util.Map;

import org.hibernate.query.SelectionQuery;
import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * Reflective support shared by the version-specific {@link HibernateAdapter} implementations.
 *
 * @author Oscar Fanchin
 * @since 4.2
 */
abstract class AbstractHibernateAdapter implements HibernateAdapter {

	private final @Nullable Class<?> sqmQuery;
	private final @Nullable Class<?> namedSqmQuery;
	private final @Nullable Class<?> namedNativeQuery;

	/**
	 * Methods are resolved against the implementation class rather than against the marker interfaces: Hibernate 8's
	 * {@code SqmStatementAccess} does not declare {@code getQueryString()}, so there is no stable interface to anchor
	 * the lookup to.
	 */
	private final Map<MethodKey, Method> methodCache = new ConcurrentReferenceHashMap<>();

	AbstractHibernateAdapter(ClassLoader classLoader, String sqmQuery, String namedSqmQuery, String namedNativeQuery) {

		this.sqmQuery = loadClass(sqmQuery, classLoader);
		this.namedSqmQuery = loadClass(namedSqmQuery, classLoader);
		this.namedNativeQuery = loadClass(namedNativeQuery, classLoader);
	}

	@Override
	public boolean isSqmQuery(Object query) {
		return isInstance(sqmQuery, query);
	}

	@Override
	public boolean isNamedSqmQuery(Object query) {
		return isInstance(namedSqmQuery, query);
	}

	@Override
	public boolean isNamedNativeQuery(Object query) {
		return isInstance(namedNativeQuery, query);
	}

	@Override
	public String getQueryString(Object query) {
		return invokeStringMethod(query, "getQueryString");
	}

	@Override
	public String getHqlString(Object query) {
		return invokeStringMethod(query, "getHqlString");
	}

	@Override
	public String getSqlString(Object query) {
		return invokeStringMethod(query, "getSqlString");
	}

	@Override
	public String getSqmStatement(Object query) {
		return invokeStringMethod(invokeMethod(query, "getSqmStatement"), "toHqlString");
	}

	@Override
	public @Nullable SelectionQuery<?> asSelectionQuery(Query query) {
		return null;
	}

	private String invokeStringMethod(Object target, String methodName) {
		return (String) invokeMethod(target, methodName);
	}

	private Object invokeMethod(Object target, String methodName) {

		Method method = methodCache.computeIfAbsent(new MethodKey(target.getClass(), methodName), key -> {

			Method resolved = ReflectionUtils.findMethod(key.type(), key.name());

			if (resolved == null) {
				throw new IllegalStateException("Cannot resolve method %s on %s".formatted(key.name(), key.type()));
			}

			// The implementation classes live in internal packages and are not necessarily public.
			ReflectionUtils.makeAccessible(resolved);

			return resolved;
		});

		return ReflectionUtils.invokeMethod(method, target);
	}

	private static boolean isInstance(@Nullable Class<?> type, Object query) {
		return type != null && type.isInstance(query);
	}

	/**
	 * Resolve the given type, returning {@literal null} when absent so that callers degrade to their legacy path instead
	 * of failing during class initialization.
	 */
	static @Nullable Class<?> loadClass(String className, ClassLoader classLoader) {
		return ClassUtils.isPresent(className, classLoader) ? ClassUtils.resolveClassName(className, classLoader) : null;
	}

	private record MethodKey(Class<?> type, String name) {}
}
