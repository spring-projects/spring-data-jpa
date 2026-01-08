/*
 * Copyright 2011-present the original author or authors.
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

import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.data.jpa.mapping.JpaIdentifierResolver;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.JpaMetamodelEntityMetadata;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.data.repository.core.EntityInformation} that uses JPA {@link Metamodel}
 * to find the domain class' id field.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Greg Turnquist
 */
public class JpaMetamodelEntityInformation<T, ID> extends JpaEntityInformationSupport<T, ID> {

	private final Optional<SingularAttribute<? super T, ?>> versionAttribute;
	private final @Nullable String entityName;
	private final JpaIdentifierResolver<T> identifierResolver;

	/**
	 * Creates a new {@link JpaMetamodelEntityInformation} for the given domain class and {@link Metamodel}.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @param metamodel must not be {@literal null}.
	 * @param persistenceUnitUtil must not be {@literal null}.
	 */
	public JpaMetamodelEntityInformation(Class<T> domainClass, Metamodel metamodel,
			PersistenceUnitUtil persistenceUnitUtil) {

		super(domainClass);

		Assert.notNull(metamodel, "Metamodel must not be null");
		PersistenceProvider persistenceProvider = PersistenceProvider.fromMetamodel(metamodel);

		ManagedType<T> type = metamodel.managedType(domainClass);

		if (type == null) {
			throw new IllegalArgumentException("The given domain class can not be found in the given Metamodel");
		}

		this.entityName = type instanceof EntityType ? ((EntityType<?>) type).getName() : null;

		if (!(type instanceof IdentifiableType<T> identifiableType)) {
			throw new IllegalArgumentException("The given domain class does not contain an id attribute");
		}

		this.identifierResolver = new JpaIdentifierResolver<>(identifiableType, persistenceProvider, persistenceUnitUtil);
		this.versionAttribute = findVersionAttribute(identifiableType, metamodel);

		Assert.notNull(persistenceUnitUtil, "PersistenceUnitUtil must not be null");
	}

	/**
	 * Creates a new {@link JpaMetamodelEntityInformation} for the given {@link Metamodel}.
	 *
	 * @param entityType must not be {@literal null}.
	 * @param metamodel must not be {@literal null}.
	 * @param persistenceUnitUtil must not be {@literal null}.
	 * @since 4.0
	 */
	JpaMetamodelEntityInformation(EntityType<T> entityType, Metamodel metamodel,
			PersistenceUnitUtil persistenceUnitUtil) {

		super(new JpaMetamodelEntityMetadata<>(entityType));

		PersistenceProvider persistenceProvider = PersistenceProvider.fromMetamodel(metamodel);
		this.entityName = entityType.getName();
		this.identifierResolver = new JpaIdentifierResolver<>(entityType, persistenceProvider, persistenceUnitUtil);
		this.versionAttribute = findVersionAttribute(entityType, metamodel);

		Assert.notNull(persistenceUnitUtil, "PersistenceUnitUtil must not be null");
	}

	@Override
	public String getEntityName() {
		return entityName != null ? entityName : super.getEntityName();
	}

	/**
	 * Returns the version attribute of the given {@link ManagedType} or {@literal null} if none available.
	 *
	 * @param type must not be {@literal null}.
	 * @param metamodel must not be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	private static <T> Optional<SingularAttribute<? super T, ?>> findVersionAttribute(IdentifiableType<T> type,
			Metamodel metamodel) {

		try {
			return Optional.ofNullable(type.getVersion(Object.class));
		} catch (IllegalArgumentException o_O) {
			// Needs workarounds as the method is implemented with a strict type check on e.g. Hibernate < 4.3
		}

		Set<SingularAttribute<? super T, ?>> attributes = type.getSingularAttributes();

		for (SingularAttribute<? super T, ?> attribute : attributes) {
			if (attribute.isVersion()) {
				return Optional.of(attribute);
			}
		}

		Class<?> superType = type.getJavaType().getSuperclass();

		if (!JpaMetamodel.of(metamodel).isJpaManaged(superType)) {
			return Optional.empty();
		}

		ManagedType<?> managedSuperType = metamodel.managedType(superType);

		if (!(managedSuperType instanceof IdentifiableType)) {
			return Optional.empty();
		}

		return findVersionAttribute((IdentifiableType<T>) managedSuperType, metamodel);
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable ID getId(T entity) {
		return (ID) identifierResolver.getId(entity);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		return (Class<ID>) identifierResolver.getIdType();
	}

	@Override
	public SingularAttribute<? super T, ?> getIdAttribute() {
		return identifierResolver.getIdAttribute();
	}

	@Override
	public boolean hasCompositeId() {
		return identifierResolver.hasCompositeId();
	}

	@Override
	public Collection<String> getIdAttributeNames() {
		return identifierResolver.getIdAttributeNames();
	}

	@Override
	public @Nullable Object getCompositeIdAttributeValue(Object id, String idAttribute) {
		Assert.isTrue(hasCompositeId(), "Model must have a composite Id");
		return identifierResolver.getCompositeIdAttributeValue(id, idAttribute);
	}

	@Override
	public boolean isNew(T entity) {

		if (versionAttribute.isEmpty()
				|| versionAttribute.map(Attribute::getJavaType).map(Class::isPrimitive).orElse(false)) {
			return super.isNew(entity);
		}

		BeanWrapper wrapper = new DirectFieldAccessFallbackBeanWrapper(entity);

		return versionAttribute.map(it -> wrapper.getPropertyValue(it.getName()) == null).orElse(true);
	}

	@Override
	public Map<String, Object> getKeyset(Iterable<String> propertyPaths, T entity) {
		return identifierResolver.getKeyset(propertyPaths, entity);
	}
}
