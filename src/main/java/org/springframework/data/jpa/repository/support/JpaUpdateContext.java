/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import javax.persistence.EntityManager;

import org.springframework.data.repository.augment.UpdateContext;
import org.springframework.util.Assert;

/**
 * JPA-specific extension of {@link UpdateContext} adding an {@link EntityManager} to it.
 * 
 * @author Oliver Gierke
 */
public class JpaUpdateContext<T> extends UpdateContext<T> {

	private final EntityManager em;

	/**
	 * Creates a new {@link JpaUpdateContext} from the given entity and {@link EntityManager}.
	 * 
	 * @param entity
	 * @param em must not be {@literal null}.
	 */
	public JpaUpdateContext(T entity, UpdateMode mode, EntityManager em) {

		super(entity, mode);

		Assert.notNull(em, "EntityManager must not be null!");
		this.em = em;
	}

	/**
	 * Returns the {@link EntityManager} to be used for this update.
	 * 
	 * @return the em will never be {@literal null}.
	 */
	public EntityManager getEntityManager() {
		return em;
	}
}
