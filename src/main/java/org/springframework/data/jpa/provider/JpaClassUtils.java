/*
 * Copyright 2008-2019 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Metamodel;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility class to work with classes.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 */
abstract class JpaClassUtils {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private JpaClassUtils() {}

	/**
	 * Returns whether the given {@link EntityManager} is of the given type.
	 *
	 * @param em must not be {@literal null}.
	 * @param type the fully qualified expected {@link EntityManager} type, must not be {@literal null} or empty.
	 * @return whether the given {@code EntityManager} is of the given type.
	 */
	public static boolean isEntityManagerOfType(EntityManager em, String type) {

		EntityManager entityManagerToUse = em;
		Object delegate = em.getDelegate();

		if (delegate instanceof EntityManager) {
			entityManagerToUse = (EntityManager) delegate;
		}

		return isOfType(entityManagerToUse, type, entityManagerToUse.getClass().getClassLoader());
	}

	public static boolean isMetamodelOfType(Metamodel metamodel, String type) {
		return isOfType(metamodel, type, metamodel.getClass().getClassLoader());
	}

	private static boolean isOfType(Object source, String typeName, @Nullable ClassLoader classLoader) {

		Assert.notNull(source, "Source instance must not be null!");
		Assert.hasText(typeName, "Target type name must not be null or empty!");

		try {
			return ClassUtils.forName(typeName, classLoader).isInstance(source);
		} catch (Exception e) {
			return false;
		}
	}
}
