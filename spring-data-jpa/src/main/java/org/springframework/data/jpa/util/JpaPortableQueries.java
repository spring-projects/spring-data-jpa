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
package org.springframework.data.jpa.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaUpdate;

import java.lang.reflect.Method;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Creates {@link Query queries} from an {@link EntityManager} in a way that stays binary-compatible across Jakarta
 * Persistence API generations.
 * <p>
 * Jakarta Persistence 4 changes the return type of the untyped query factory methods, so code compiled against 3.2
 * fails with a {@link NoSuchMethodError} when running against 4. Only that case pays for reflection. See GH-4197.
 *
 * @author Oscar Fanchin
 * @since 4.2
 */
public abstract class JpaPortableQueries {

	private static final Method CREATE_QUERY = resolveMethod("createQuery", String.class);
	private static final Method CREATE_NAMED_QUERY = resolveMethod("createNamedQuery", String.class);
	private static final Method CREATE_NATIVE_QUERY = resolveMethod("createNativeQuery", String.class);
	private static final Method CREATE_NATIVE_QUERY_WITH_TYPE = resolveMethod("createNativeQuery", String.class,
			Class.class);
	private static final Method CREATE_NATIVE_QUERY_WITH_MAPPING = resolveMethod("createNativeQuery", String.class,
			String.class);
	private static final Method CREATE_UPDATE_QUERY = resolveCriteriaQueryMethod(CriteriaUpdate.class);
	private static final Method CREATE_DELETE_QUERY = resolveCriteriaQueryMethod(CriteriaDelete.class);

	// Method references are resolved on first execution, so the direct calls below are never linked under JPA 4.
	private static final boolean JPA_32 = CREATE_QUERY.getReturnType() == Query.class;

	private JpaPortableQueries() {}

	/**
	 * Create an untyped query using the runtime Jakarta Persistence API.
	 */
	public static Query createQuery(EntityManager entityManager, String queryString) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createQuery(queryString) //
				: (Query) ReflectionUtils.invokeMethod(CREATE_QUERY, entityManager, queryString);
	}

	/**
	 * Create an untyped named query using the runtime Jakarta Persistence API.
	 */
	public static Query createNamedQuery(EntityManager entityManager, String queryName) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createNamedQuery(queryName) //
				: (Query) ReflectionUtils.invokeMethod(CREATE_NAMED_QUERY, entityManager, queryName);
	}

	/**
	 * Create an untyped native query using the runtime Jakarta Persistence API.
	 */
	public static Query createNativeQuery(EntityManager entityManager, String queryString) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createNativeQuery(queryString) //
				: (Query) ReflectionUtils.invokeMethod(CREATE_NATIVE_QUERY, entityManager, queryString);
	}

	/**
	 * Create a native query for the given result type using the runtime Jakarta Persistence API.
	 */
	public static Query createNativeQuery(EntityManager entityManager, String queryString, Class<?> resultClass) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createNativeQuery(queryString, resultClass) //
				: (Query) ReflectionUtils.invokeMethod(CREATE_NATIVE_QUERY_WITH_TYPE, entityManager, queryString,
						resultClass);
	}

	/**
	 * Create a native query for the given SQL result set mapping using the runtime Jakarta Persistence API.
	 */
	public static Query createNativeQuery(EntityManager entityManager, String queryString, String resultSetMapping) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createNativeQuery(queryString, resultSetMapping) //
				: (Query) ReflectionUtils.invokeMethod(CREATE_NATIVE_QUERY_WITH_MAPPING, entityManager, queryString,
						resultSetMapping);
	}

	/**
	 * Create a typed query through the {@link CriteriaSelect} overload common to Jakarta Persistence API generations.
	 */
	public static <T> TypedQuery<T> createQuery(EntityManager entityManager, CriteriaQuery<T> criteriaQuery) {

		Assert.notNull(entityManager, "EntityManager must not be null");
		Assert.notNull(criteriaQuery, "CriteriaQuery must not be null");

		return entityManager.createQuery((CriteriaSelect<T>) criteriaQuery);
	}

	/**
	 * Create an update query using the runtime Jakarta Persistence API.
	 */
	public static Query createQuery(EntityManager entityManager, CriteriaUpdate<?> criteriaUpdate) {

		Assert.notNull(entityManager, "EntityManager must not be null");
		Assert.notNull(criteriaUpdate, "CriteriaUpdate must not be null");

		return JPA_32 //
				? entityManager.createQuery(criteriaUpdate) //
				: (Query) ReflectionUtils.invokeMethod(CREATE_UPDATE_QUERY, entityManager, criteriaUpdate);
	}

	/**
	 * Create a delete query using the runtime Jakarta Persistence API.
	 */
	public static Query createQuery(EntityManager entityManager, CriteriaDelete<?> criteriaDelete) {

		Assert.notNull(entityManager, "EntityManager must not be null");
		Assert.notNull(criteriaDelete, "CriteriaDelete must not be null");

		return JPA_32 //
				? entityManager.createQuery(criteriaDelete) //
				: (Query) ReflectionUtils.invokeMethod(CREATE_DELETE_QUERY, entityManager, criteriaDelete);
	}

	private static Method resolveMethod(String methodName, Class<?>... parameterTypes) {

		Method method = ReflectionUtils.findMethod(EntityManager.class, methodName, parameterTypes);

		Assert.state(method != null, () -> "Cannot resolve EntityManager.%s".formatted(methodName));

		return method;
	}

	private static Method resolveCriteriaQueryMethod(Class<?> criteriaType) {

		for (Method method : EntityManager.class.getMethods()) {
			if (method.getName().equals("createQuery") && method.getParameterCount() == 1
					&& method.getParameterTypes()[0].isAssignableFrom(criteriaType)
					&& Query.class.isAssignableFrom(method.getReturnType())) {
				return method;
			}
		}

		throw new IllegalStateException("Cannot resolve EntityManager.createQuery(%s)".formatted(criteriaType.getSimpleName()));
	}
}
