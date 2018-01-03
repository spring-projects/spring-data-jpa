/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.jpa.support;

import javax.persistence.EntityManager;

import org.springframework.util.ReflectionUtils;

/**
 * Utility class with {@link EntityManager} related helper methods.
 *
 * @author Thomas Darimont
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
}
