/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.data.jpa.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.springframework.util.Assert;

/**
 * Wrapper around the JPA {@link Metamodel} to be able to apply some fixes against bugs in provider implementations.
 * 
 * @author Oliver Gierke
 */
public class JpaMetamodel {

	private final Metamodel metamodel;

	private Collection<Class<?>> managedTypes;

	/**
	 * Creates a new {@link JpaMetamodel} for the given JPA {@link Metamodel}.
	 * 
	 * @param metamodel must not be {@literal null}.
	 */
	public JpaMetamodel(Metamodel metamodel) {

		Assert.notNull(metamodel, "Metamodel must not be null!");

		this.metamodel = metamodel;
	}

	/**
	 * Returns whether the given type is managed by the backing JPA {@link Metamodel}.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public boolean isJpaManaged(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		return getManagedTypes().contains(type);
	}

	/**
	 * Returns all types managed by the backing {@link Metamodel}. Skips {@link ManagedType} instances that return
	 * {@literal null} for calls to {@link ManagedType#getJavaType()}.
	 * 
	 * @return all managed types.
	 * @see <a href="https://hibernate.atlassian.net/browse/HHH-10968">HHH-10968</a>
	 */
	private Collection<Class<?>> getManagedTypes() {

		if (managedTypes != null) {
			return managedTypes;
		}

		Set<ManagedType<?>> managedTypes = metamodel.getManagedTypes();
		Set<Class<?>> types = new HashSet<Class<?>>(managedTypes.size());

		for (ManagedType<?> managedType : metamodel.getManagedTypes()) {

			Class<?> type = managedType.getJavaType();

			if (type != null) {
				types.add(type);
			}
		}

		this.managedTypes = Collections.unmodifiableSet(types);

		return this.managedTypes;
	}
}
