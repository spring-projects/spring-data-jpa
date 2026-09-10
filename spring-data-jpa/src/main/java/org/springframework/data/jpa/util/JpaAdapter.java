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

import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Creates {@link Query queries} from an {@link EntityManager} in a way that stays binary-compatible across Jakarta
 * Persistence API generations.
 * <p>
 * Jakarta Persistence 4 changes the return type of the untyped query factory methods, so code compiled against 3.2
 * fails with a {@link NoSuchMethodError} when running against 4 and therefore we're falling back tom reflection.
 *
 * @author Oscar Fanchin
 * @author Christoph Strobl
 * @since 4.2
 */
public abstract class JpaAdapter {

	private static final ReflectiveMethod CREATE_QUERY = ReflectiveMethod.get(EntityManager.class, "createQuery",
			String.class);
	private static final ReflectiveMethod CREATE_NAMED_QUERY = ReflectiveMethod.get(EntityManager.class,
			"createNamedQuery", String.class);
	private static final ReflectiveMethod CREATE_NATIVE_QUERY = ReflectiveMethod.get(EntityManager.class,
			"createNativeQuery", String.class);
	private static final ReflectiveMethod CREATE_NATIVE_QUERY_WITH_TYPE = ReflectiveMethod.get(EntityManager.class,
			"createNativeQuery", String.class, Class.class);
	private static final ReflectiveMethod CREATE_NATIVE_QUERY_WITH_MAPPING = ReflectiveMethod.get(EntityManager.class,
			"createNativeQuery", String.class, String.class);
	private static final ReflectiveMethod CREATE_UPDATE_QUERY = resolveCriteriaStatementMethod(CriteriaUpdate.class);
	private static final ReflectiveMethod CREATE_DELETE_QUERY = resolveCriteriaStatementMethod(CriteriaDelete.class);

	// Method references are resolved on first execution, so the direct calls below are never linked under JPA 4.
	private static final boolean JPA_32 = CREATE_QUERY.getReturnType() == Query.class;

	private static final List<ReflectiveMethod> REFLECTIVE_METHODS = List.of(CREATE_QUERY, CREATE_NAMED_QUERY,
			CREATE_NATIVE_QUERY, CREATE_NATIVE_QUERY_WITH_TYPE, CREATE_NATIVE_QUERY_WITH_MAPPING, CREATE_UPDATE_QUERY,
			CREATE_DELETE_QUERY);

	private JpaAdapter() {}

	public static List<ReflectiveMethod> getReflectiveMethods() {
		return REFLECTIVE_METHODS;
	}

	/**
	 * Create an untyped query using the runtime Jakarta Persistence API.
	 */
	public static Query createQuery(EntityManager entityManager, String queryString) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createQuery(queryString) //
				: (Query) CREATE_QUERY.invoke(entityManager, queryString);
	}

	/**
	 * Create an untyped named query using the runtime Jakarta Persistence API.
	 */
	public static Query createNamedQuery(EntityManager entityManager, String queryName) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createNamedQuery(queryName) //
				: (Query) CREATE_NAMED_QUERY.invoke(entityManager, queryName);
	}

	/**
	 * Create an untyped native query using the runtime Jakarta Persistence API.
	 */
	public static Query createNativeQuery(EntityManager entityManager, String queryString) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createNativeQuery(queryString) //
				: (Query) CREATE_NATIVE_QUERY.invoke(entityManager, queryString);
	}

	/**
	 * Create a native query for the given result type using the runtime Jakarta Persistence API.
	 */
	public static Query createNativeQuery(EntityManager entityManager, String queryString, Class<?> resultClass) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createNativeQuery(queryString, resultClass) //
				: (Query) CREATE_NATIVE_QUERY_WITH_TYPE.invoke(entityManager, queryString, resultClass);
	}

	/**
	 * Create a native query for the given SQL result set mapping using the runtime Jakarta Persistence API.
	 */
	public static Query createNativeQuery(EntityManager entityManager, String queryString, String resultSetMapping) {

		Assert.notNull(entityManager, "EntityManager must not be null");

		return JPA_32 //
				? entityManager.createNativeQuery(queryString, resultSetMapping) //
				: (Query) CREATE_NATIVE_QUERY_WITH_MAPPING.invoke(entityManager, queryString, resultSetMapping);
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
	@SuppressWarnings("removal")
	public static Query createQuery(EntityManager entityManager, CriteriaUpdate<?> criteriaUpdate) {

		Assert.notNull(entityManager, "EntityManager must not be null");
		Assert.notNull(criteriaUpdate, "CriteriaUpdate must not be null");

		return JPA_32 //
				? entityManager.createQuery(criteriaUpdate) //
				: (Query) CREATE_UPDATE_QUERY.invoke(entityManager, criteriaUpdate);
	}

	/**
	 * Create a delete query using the runtime Jakarta Persistence API.
	 */
	@SuppressWarnings("removal")
	public static Query createQuery(EntityManager entityManager, CriteriaDelete<?> criteriaDelete) {

		Assert.notNull(entityManager, "EntityManager must not be null");
		Assert.notNull(criteriaDelete, "CriteriaDelete must not be null");

		return JPA_32 //
				? entityManager.createQuery(criteriaDelete) //
				: (Query) CREATE_DELETE_QUERY.invoke(entityManager, criteriaDelete);
	}

	/**
	 * Resolve the factory method for criteria mutations.
	 * <p>
	 * JPA 4.0 deprecated {@code createQuery(CriteriaStatement)} so we prefer {@code createStatement(CriteriaStatement)}
	 * where possible.
	 *
	 * @param criteriaType the criteria type to resolve the method for.
	 * @return the most appropriate method to use.
	 */
	private static ReflectiveMethod resolveCriteriaStatementMethod(Class<?> criteriaType) {

		ReflectionUtils.MethodFilter filter = method -> method.getParameterCount() == 1
				&& method.getParameterTypes()[0].isAssignableFrom(criteriaType)
				&& Query.class.isAssignableFrom(method.getReturnType());

		ReflectiveMethod method = ReflectiveMethod.find(EntityManager.class, "createStatement", filter);

		if (method == null) {
			method = ReflectiveMethod.find(EntityManager.class, "createQuery", filter);
		}

		if (method == null) {
			throw new IllegalStateException(
					"Cannot resolve EntityManager.createStatement(%1$s) nor EntityManager.createQuery(%1$s)"
							.formatted(criteriaType.getSimpleName()));
		}

		return method;
	}

}
