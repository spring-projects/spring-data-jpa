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
package org.springframework.data.jpa.repository.support;

import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.ManagedType;

import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Default implementation of {@link JpaContext}.
 *
 * @author Oliver Gierke
 * @soundtrack Marcus Miller - Son Of Macbeth (Afrodeezia)
 * @since 1.9
 */
public class DefaultJpaContext implements JpaContext {

	private final MultiValueMap<Class<?>, EntityManager> entityManagers;

	/**
	 * Creates a new {@link DefaultJpaContext} for the given {@link Set} of {@link EntityManager}s.
	 *
	 * @param entityManagers must not be {@literal null}.
	 */
	public DefaultJpaContext(Set<EntityManager> entityManagers) {

		Assert.notNull(entityManagers, "EntityManagers must not be null!");
		Assert.notEmpty(entityManagers, "EntityManagers must not be empty!");

		this.entityManagers = new LinkedMultiValueMap<Class<?>, EntityManager>();

		for (EntityManager em : entityManagers) {
			for (ManagedType<?> managedType : em.getMetamodel().getManagedTypes()) {
				this.entityManagers.add(managedType.getJavaType(), em);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.JpaContext#getByManagedType(java.lang.Class)
	 */
	@Override
	public EntityManager getEntityManagerByManagedType(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		if (!entityManagers.containsKey(type)) {
			throw new IllegalArgumentException(String.format("%s is not a managed type!", type));
		}

		List<EntityManager> candidates = this.entityManagers.get(type);

		if (candidates.size() == 1) {
			return candidates.get(0);
		}

		throw new IllegalArgumentException(
				String.format("%s managed by more than one EntityManagers: %s!", type.getName(), candidates));
	}
}
