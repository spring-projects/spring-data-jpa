/*
 * Copyright 2014-present the original author or authors.
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
package org.springframework.data.jpa.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;

import java.lang.reflect.Method;

import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.mockito.verification.VerificationMode;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class with {@link EntityManager} related helper methods.
 * <p>
 * The query factory methods are resolved here rather than through the production adapter on purpose: stubbing and
 * verifying through the very code under test would make a defect in it undetectable.
 *
 * @author Thomas Darimont
 * @author Oscar Fanchin
 */
public abstract class EntityManagerTestUtils {

	private static final Method CREATE_QUERY = resolveMethod("createQuery", String.class);
	private static final Method CREATE_NAMED_QUERY = resolveMethod("createNamedQuery", String.class);
	private static final Method CREATE_NATIVE_QUERY = resolveMethod("createNativeQuery", String.class);
	private static final Method CREATE_NATIVE_QUERY_WITH_TYPE = resolveMethod("createNativeQuery", String.class,
			Class.class);

	private EntityManagerTestUtils() {}

	public static boolean currentEntityManagerIsAJpa21EntityManager(EntityManager em) {
		return ReflectionUtils.findMethod(((org.springframework.orm.jpa.EntityManagerProxy) em).getTargetEntityManager()
				.getClass(), "getEntityGraph", String.class) != null;
	}

	public static boolean currentEntityManagerIsHibernateEntityManager(EntityManager em) {
		return em.getDelegate().getClass().getName().toLowerCase().contains("hibernate");
	}

	/**
	 * Creates a query mock matching the return type of {@link EntityManager#createQuery(String)}.
	 *
	 * @return a query mock compatible with the Jakarta Persistence API on the classpath.
	 */
	@SuppressWarnings("unchecked")
	public static Query mockQuery() {
		return Mockito.mock((Class<? extends Query>) CREATE_QUERY.getReturnType());
	}

	/**
	 * Stub {@link EntityManager#createQuery(String)} on the given mock.
	 */
	public static OngoingStubbing<Query> whenCreateQuery(EntityManager entityManager, String queryString) {
		return Mockito.when(invoke(CREATE_QUERY, entityManager, queryString));
	}

	/**
	 * Stub {@code EntityManager#createQuery(CriteriaSelect)} on the given mock.
	 * <p>
	 * The cast pins the overload production code uses: it is the one whose signature is unchanged across Jakarta
	 * Persistence 3.2 and 4, whereas {@code createQuery(CriteriaQuery)} exists only in 3.2.
	 */
	public static <T> OngoingStubbing<TypedQuery<T>> whenCreateQuery(EntityManager entityManager,
			CriteriaQuery<T> criteriaQuery) {
		return Mockito.when(entityManager.createQuery((CriteriaSelect<T>) criteriaQuery));
	}

	/**
	 * Stub {@link EntityManager#createNamedQuery(String)} on the given mock.
	 */
	public static OngoingStubbing<Query> whenCreateNamedQuery(EntityManager entityManager, String queryName) {
		return Mockito.when(invoke(CREATE_NAMED_QUERY, entityManager, queryName));
	}

	/**
	 * Stub {@link EntityManager#createNativeQuery(String)} on the given mock.
	 */
	public static OngoingStubbing<Query> whenCreateNativeQuery(EntityManager entityManager, String queryString) {
		return Mockito.when(invoke(CREATE_NATIVE_QUERY, entityManager, queryString));
	}

	/**
	 * Stub {@link EntityManager#createNativeQuery(String, Class)} on the given mock.
	 */
	public static OngoingStubbing<Query> whenCreateNativeQuery(EntityManager entityManager, String queryString,
			Class<?> resultClass) {
		return Mockito.when(invoke(CREATE_NATIVE_QUERY_WITH_TYPE, entityManager, queryString, resultClass));
	}

	/**
	 * Invoke {@link EntityManager#createQuery(String)} on an entity manager already put into verification mode.
	 * <p>
	 * {@code Mockito.verify(…)} stays at the call site on purpose: arguments are evaluated before the call, so an
	 * argument matcher passed to this method would be registered before verification starts and Mockito would reject it
	 * as misplaced.
	 */
	public static void verifyCreateQuery(EntityManager verifiedEntityManager, String queryString) {
		invoke(CREATE_QUERY, verifiedEntityManager, queryString);
	}

	/**
	 * Invoke {@link EntityManager#createNativeQuery(String)} on an entity manager already put into verification mode.
	 */
	public static void verifyCreateNativeQuery(EntityManager verifiedEntityManager, String queryString) {
		invoke(CREATE_NATIVE_QUERY, verifiedEntityManager, queryString);
	}

	/**
	 * Invoke {@link EntityManager#createNativeQuery(String, Class)} on an entity manager already put into verification
	 * mode.
	 */
	public static void verifyCreateNativeQuery(EntityManager verifiedEntityManager, String queryString,
			Class<?> resultClass) {
		invoke(CREATE_NATIVE_QUERY_WITH_TYPE, verifiedEntityManager, queryString, resultClass);
	}

	private static Query invoke(Method method, EntityManager entityManager, Object... arguments) {
		return (Query) ReflectionUtils.invokeMethod(method, entityManager, arguments);
	}

	private static Method resolveMethod(String methodName, Class<?>... parameterTypes) {

		Method method = ReflectionUtils.findMethod(EntityManager.class, methodName, parameterTypes);

		Assert.state(method != null, () -> "Cannot resolve EntityManager.%s".formatted(methodName));

		return method;
	}
}
