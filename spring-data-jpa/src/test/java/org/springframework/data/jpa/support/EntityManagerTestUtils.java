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

import org.mockito.Mockito;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class with {@link EntityManager} related helper methods.
 *
 * @author Thomas Darimont
 * @author Oscar Fanchin
 */
public abstract class EntityManagerTestUtils {

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
	 * <p>
	 * Jakarta Persistence 4 changes the return type from {@link Query} to {@code StatementOrTypedQuery}. Resolving the
	 * method at runtime keeps these test sources compilable against both API generations.
	 *
	 * @return a query mock compatible with the Jakarta Persistence API on the classpath.
	 */
	@SuppressWarnings("unchecked")
	public static Query mockQuery() {

		var createQuery = ReflectionUtils.findMethod(EntityManager.class, "createQuery", String.class);

		Assert.state(createQuery != null, "Cannot resolve EntityManager.createQuery(String)");

		return Mockito.mock((Class<? extends Query>) createQuery.getReturnType());
	}
}
