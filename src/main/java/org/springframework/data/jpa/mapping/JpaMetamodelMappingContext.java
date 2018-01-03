/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.jpa.mapping;

import java.util.Set;

import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link MappingContext} implementation based on a Jpa {@link Metamodel}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.3
 */
public class JpaMetamodelMappingContext
		extends AbstractMappingContext<JpaPersistentEntityImpl<?>, JpaPersistentProperty> {

	private final Set<Metamodel> models;
	private final PersistenceProvider persistenceProvider;

	/**
	 * Creates a new JPA {@link Metamodel} based {@link MappingContext}.
	 *
	 * @param models must not be {@literal null} or empty.
	 */
	public JpaMetamodelMappingContext(Set<Metamodel> models) {

		Assert.notNull(models, "JPA metamodel must not be null!");
		Assert.notEmpty(models, "At least one JPA metamodel must be present!");

		this.models = models;
		this.persistenceProvider = PersistenceProvider.fromMetamodel(models.iterator().next());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> JpaPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new JpaPersistentEntityImpl<T>(typeInformation, persistenceProvider);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(java.lang.reflect.Field, java.beans.PropertyDescriptor, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected JpaPersistentProperty createPersistentProperty(Property property, JpaPersistentEntityImpl<?> owner,
			SimpleTypeHolder simpleTypeHolder) {

		Metamodel metamodel = getMetamodelFor(owner.getType());

		if (metamodel == null) {
			throw new IllegalStateException(String.format("Metamodel for %s not available!", owner.getType()));
		}

		return new JpaPersistentPropertyImpl(metamodel, property, owner, simpleTypeHolder);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#shouldCreatePersistentEntityFor(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
		return getMetamodelFor(type.getType()) != null;
	}

	/**
	 * Returns the {@link Metamodel} aware of the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return can be {@literal null}.
	 */
	@Nullable
	private Metamodel getMetamodelFor(Class<?> type) {

		for (Metamodel model : models) {

			try {
				model.managedType(type);
				return model;
			} catch (IllegalArgumentException o_O) {

				// Fall back to inspect *all* managed types manually as Metamodel.managedType(…) only
				// returns for entities, embeddables and managed supperclasses.

				for (ManagedType<?> managedType : model.getManagedTypes()) {
					if (type.equals(managedType.getJavaType())) {
						return model;
					}
				}
			}
		}

		return null;
	}
}
