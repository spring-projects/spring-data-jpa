/*
 * Copyright 2012-present the original author or authors.
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

import jakarta.persistence.IdClass;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.mapping.PersistentPropertyPaths;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.Lazy;
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

	/**
	 * Creates a new JPA {@link Metamodel} based {@link MappingContext}.
	 *
	 * @param models must not be {@literal null} or empty.
	 */
	public JpaMetamodelMappingContext(Set<Metamodel> models) {

		Assert.notEmpty(models, "JPA metamodel must not be empty");

		this.models = new Metamodels(models);
	}

	@Override
	protected <T> JpaPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {

		Metamodel metamodel = models.getMetamodelFor(typeInformation.getType());
		if (metamodel == null) {
			metamodel = models.getIdentifierMetamodel(typeInformation.getType());
		}
		Assert.notNull(metamodel, () -> String.format("Required JpaMetamodel not found for %s", typeInformation));

		return new JpaPersistentEntityImpl<>(typeInformation, PersistenceProvider.fromMetamodel(metamodel),
				JpaMetamodel.of(metamodel));
	}

	@Override
	protected JpaPersistentProperty createPersistentProperty(Property property, JpaPersistentEntityImpl<?> owner,
			SimpleTypeHolder simpleTypeHolder) {
		return new JpaPersistentPropertyImpl(owner.getMetamodel(), property, owner, simpleTypeHolder);
	}

	@Override
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
		return models.isMetamodelManagedType(type.getUserTypeInformation())
				|| models.getIdentifierMetamodel(type.getType()) != null;
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
		return doFindPersistentPropertyPaths(type, predicate, JpaPersistentProperty::isEmbeddable);
	}

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
		private final Lazy<Map<Class<?>, Metamodel>> identifierMetamodels;

		private Metamodels(Set<Metamodel> metamodels) {
			this.metamodels = metamodels;
			this.identifierMetamodels = Lazy.of(this::findIdentifierMetamodels);
		}

		// Eclipselink 🙄
		public @Nullable Metamodel getIdentifierMetamodel(Class<?> type) {
			return identifierMetamodels.get().get(type);
		}

		private Map<Class<?>, Metamodel> findIdentifierMetamodels() {

			Map<Class<?>, Metamodel> result = new HashMap<>();

			for (Metamodel model : metamodels) {
				for (ManagedType<?> managedType : model.getManagedTypes()) {
					if (!(managedType instanceof IdentifiableType<?> identifiable)) {
						continue;
					}
					IdClass idClass = AnnotationUtils.findAnnotation(managedType.getJavaType(), IdClass.class);
					if (idClass != null) {
						result.putIfAbsent(idClass.value(), model);
					}
					if (!PersistenceProvider.fromMetamodel(model).getIdClassAttributes(identifiable).isEmpty()) {
						try {
							var idType = identifiable.getIdType();
							if (idType != null && idType.getJavaType() != null) {
								result.putIfAbsent(idType.getJavaType(), model);
							}
						} catch (IllegalStateException e) {
							// Some providers cannot expose an identifier type for multiple identifier attributes.
						}
					}
				}
			}

			return Collections.unmodifiableMap(result);
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
		private @Nullable Metamodel getMetamodelFor(Class<?> type) {

			for (Metamodel model : metamodels) {
				if (JpaMetamodel.of(model).isJpaManaged(type)) {
					return model;
				}
			}

			return null;
		}
	}
}
