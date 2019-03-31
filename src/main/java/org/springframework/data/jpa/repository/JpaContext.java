/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.data.jpa.repository;

import javax.persistence.EntityManager;

/**
 * Interface for components to provide useful information about the current JPA setup within the current
 * {@link org.springframework.context.ApplicationContext}.
 *
 * @author Oliver Gierke
 * @soundtrack Marcus Miller - Water Dancer (Afrodeezia)
 * @since 1.9
 */
public interface JpaContext {

	/**
	 * Returns the {@link EntityManager} managing the given domain type.
	 *
	 * @param managedType must not be {@literal null}.
	 * @return the {@link EntityManager} that manages the given type, will never be {@literal null}.
	 * @throws IllegalArgumentException if the given type is not a JPA managed one no unique {@link EntityManager} managing this type can be resolved.
	 */
	EntityManager getEntityManagerByManagedType(Class<?> managedType);
}
