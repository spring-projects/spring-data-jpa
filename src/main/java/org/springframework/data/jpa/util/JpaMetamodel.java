/*
 * Copyright 2016-2018 the original author or authors.
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type.PersistenceType;

import org.springframework.util.Assert;

/**
 * Wrapper around the JPA {@link Metamodel} to be able to apply some fixes against bugs in provider implementations.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 */
public class JpaMetamodel {

	private static final Set<PersistenceType> RELEVANT_MANAGED_PERSISTENCE_TYPES = EnumSet.of(PersistenceType.ENTITY,
			PersistenceType.EMBEDDABLE);

	private final Metamodel metamodel;

	private Optional<Collection<Class<?>>> managedTypes = Optional.empty();

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
	 * Returns the {@link SingularAttribute} representing the identifier of the given {@link EntityType} if it contains a
	 * singular one.
	 *
	 * @param entityType must not be {@literal null}.
	 * @return an {@code Optional} containing the singular id or {@code Optional.empty}.
	 */
	private static Optional<? extends SingularAttribute<?, ?>> getSingularIdAttribute(EntityType<?> entityType) {

		if (!entityType.hasSingleIdAttribute()) {
			return Optional.empty();
		}

		return entityType.getSingularAttributes().stream() //
				.filter(SingularAttribute::isId) //
				.findFirst();
	}

	/**
	 * Returns whether the given type is managed by the backing JPA {@link Metamodel}. Only entities and embeddables are
	 * considered managed.
	 *
	 * @param type must not be {@literal null}.
	 * @return whether the given type is managed by the backing JPA {@link Metamodel}.
	 */
	public boolean isJpaManaged(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		return getManagedTypes().contains(type);
	}

	/**
	 * Returns whether the attribute of given name and type is the single identifier attribute of the given entity.
	 *
	 * @param entity must not be {@literal null}.
	 * @param name must not be {@literal null}.
	 * @param attributeType must not be {@literal null}.
	 * @return whether the attribute of given name and type is the single identifier attribute of the given entity.
	 */
	public boolean isSingleIdAttribute(Class<?> entity, String name, Class<?> attributeType) {

		return metamodel.getEntities().stream() //
				.filter(it -> entity.equals(it.getJavaType())) //
				.findFirst() //
				.flatMap(JpaMetamodel::getSingularIdAttribute) //
				.filter(it -> it.getJavaType().equals(attributeType)) //
				.map(it -> it.getName().equals(name)) //
				.orElse(false);
	}

	/**
	 * Returns all types managed by the backing {@link Metamodel}. Skips {@link ManagedType} instances that return
	 * {@literal null} for calls to {@link ManagedType#getJavaType()}.
	 *
	 * @return all managed types.
	 * @see <a href="https://hibernate.atlassian.net/browse/HHH-10968">HHH-10968</a>
	 */
	private Collection<Class<?>> getManagedTypes() {

		if (!managedTypes.isPresent()) {

			Set<ManagedType<?>> managedTypes = metamodel.getManagedTypes();
			Set<Class<?>> types = new HashSet<>(managedTypes.size());

			for (ManagedType<?> managedType : metamodel.getManagedTypes()) {

				Class<?> type = managedType.getJavaType();

				if (type != null && RELEVANT_MANAGED_PERSISTENCE_TYPES.contains(managedType.getPersistenceType())) {
					types.add(type);
				}
			}

			this.managedTypes = Optional.of(Collections.unmodifiableSet(types));
		}

		return this.managedTypes.get();
	}
}
