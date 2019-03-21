/*
 * Copyright 2016-2019 the original author or authors.
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
package org.springframework.data.jpa.infrastructure;

import java.util.Arrays;
import java.util.List;

import javax.persistence.spi.PersistenceProvider;

import org.springframework.util.ClassUtils;

/**
 * Testing utilities for Hibernate.
 *
 * @author Oliver Gierke
 * @soundtrack Ron Spielman - Africa's Napoleon (Swimming In The Dark)
 * @since 1.10.2
 */
public class HibernateTestUtils {

	private static final List<String> PROVIDER_TYPES = Arrays.asList("org.hibernate.jpa.HibernatePersistenceProvider",
			"org.hibernate.ejb.HibernatePersistence");

	/**
	 * Returns the Hibernate {@link PersistenceProvider}.
	 *
	 * @return
	 */
	public static PersistenceProvider getPersistenceProvider() {

		ClassLoader classLoader = HibernateTestUtils.class.getClassLoader();

		for (String provider : PROVIDER_TYPES) {

			if (ClassUtils.isPresent(provider, classLoader)) {

				try {
					return (PersistenceProvider) ClassUtils.forName(provider, classLoader).newInstance();
				} catch (Exception o_O) {
					throw new RuntimeException(o_O);
				}
			}
		}

		throw new IllegalStateException("Could not obtain Hibernate PersistenceProvider!");
	}
}
