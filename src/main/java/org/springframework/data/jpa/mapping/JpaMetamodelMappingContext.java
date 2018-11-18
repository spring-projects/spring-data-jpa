/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.jpa.mapping;

import java.util.Set;
import java.util.function.Predicate;

import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.mapping.PersistentPropertyPaths;
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
 * @author David Madden
 * @since 1.3
 */
public class JpaMetamodelMappingContext
		extends AbstractMappingContext<JpaPersistentEntityImpl<?>, JpaPersistentProperty> {

	private final Metamodels models;
	private final PersistenceProvider persistenceProvider;

	/**
	 * Creates a new JPA {@link Metamodel} based {@link MappingContext}.
	 *
	 * @param models must not be {@literal null} or empty.
	 */
	public JpaMetamodelMappingContext(Set<Metamodel> models) {

		Assert.notNull(models, "JPA metamodel must not be null!");
		Assert.notEmpty(models, "JPA metamodel must not be empty!");

		this.models = new Metamodels(models);
		this.persistenceProvider = PersistenceProvider.fromMetamodel(models.iterator().next());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> JpaPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new JpaPersistentEntityImpl<T>(typeInformation, persistenceProvider, models.getMetamodel(typeInformation));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(java.lang.reflect.Field, java.beans.PropertyDescriptor, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected JpaPersistentProperty createPersistentProperty(Property property, JpaPersistentEntityImpl<?> owner,
			SimpleTypeHolder simpleTypeHolder) {
		return new JpaPersistentPropertyImpl(owner.getMetamodel(), property, owner, simpleTypeHolder);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#shouldCreatePersistentEntityFor(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
		return models.isMetamodelManagedType(type);
	}

	/**
	 * We customize the lookup of {@link PersistentPropertyPaths} by also traversing properties that are embeddables.
	 * 
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#findPersistentPropertyPaths(java.lang.Class,
	 *      java.util.function.Predicate)
	 */
	@Override
	public <T> PersistentPropertyPaths<T, JpaPersistentProperty> findPersistentPropertyPaths(Class<T> type,
			Predicate<? super JpaPersistentProperty> predicate) {
		return doFindPersistentPropertyPaths(type, predicate, it -> it.isEmbeddable());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#hasPersistentEntityFor(java.lang.Class)
	 */
	@Override
	public boolean hasPersistentEntityFor(Class<?> type) {
		return super.hasPersistentEntityFor(type) || models.isMetamodelManagedType(type);
	}

	/**
	 * A wrapper for a set of JPA {@link Metamodel} instances to simplify lookups of {@link JpaMetamodel} instances and
	 * managed type checks.
	 *
	 * @author Oliver Gierke
	 */
	private static class Metamodels {

		private final Set<Metamodel> metamodels;

		private Metamodels(Set<Metamodel> metamodels) {
			this.metamodels = metamodels;
		}

		/**
		 * Returns the {@link JpaMetamodel} for the given type.
		 * 
		 * @param type must not be {@literal null}.
		 * @return
		 */
		@Nullable
		public JpaMetamodel getMetamodel(TypeInformation<?> type) {

			Metamodel metamodel = getMetamodelFor(type.getType());

			return metamodel == null ? null : JpaMetamodel.of(metamodel);
		}

		/**
		 * Returns whether the given type is managed by one of the underlying {@link Metamodel} instances.
		 * 
		 * @param type must not be {@literal null}.
		 * @return
		 */
		public boolean isMetamodelManagedType(TypeInformation<?> type) {
			return isMetamodelManagedType(type.getType());
		}

		/**
		 * Returns whether the given type is managed by one of the underlying {@link Metamodel} instances.
		 * 
		 * @param type must not be {@literal null}.
		 * @return
		 */
		public boolean isMetamodelManagedType(Class<?> type) {
			return getMetamodelFor(type) != null;
		}

		/**
		 * Returns the {@link Metamodel} aware of the given type.
		 *
		 * @param type must not be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@Nullable
		private Metamodel getMetamodelFor(Class<?> type) {

			for (Metamodel model : metamodels) {

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
}
