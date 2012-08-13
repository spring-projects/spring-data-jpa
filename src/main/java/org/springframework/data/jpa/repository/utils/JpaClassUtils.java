/*
 * Copyright 2008-2011 the original author or authors.
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
package org.springframework.data.jpa.repository.utils;

import javax.persistence.EntityManager;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility class to work with classes.
 * 
 * @author Oliver Gierke
 */
public abstract class JpaClassUtils {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private JpaClassUtils() {

	}

	/**
	 * Returns whether the given {@link EntityManager} is of the given type.
	 * 
	 * @param em must not be {@literal null}.
	 * @param type the fully qualified expected {@link EntityManager} type, must not be {@literal null} or empty.
	 * @return
	 */
	public static boolean isEntityManagerOfType(EntityManager em, String type) {

		Assert.notNull(em, "EntityManager must not be null!");
		Assert.hasText(type, "EntityManager type must not be null!");

		try {

			ClassLoader loader = em.getDelegate().getClass().getClassLoader();
			Class<?> emType = ClassUtils.forName(type, loader);

			emType.cast(em);
			return true;

		} catch (Exception e) {
			return false;
		}
	}
}
