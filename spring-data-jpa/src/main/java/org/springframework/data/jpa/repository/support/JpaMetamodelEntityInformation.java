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
import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.jpa.mapping.JpaPersistentEntity;
import org.springframework.data.jpa.mapping.JpaPersistentProperty;
import org.springframework.data.jpa.mapping.PersistentEntityIdentifier;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.mapping.PersistentPropertyAccessor;
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

	private final JpaPersistentEntity<T> persistentEntity;
	private final PersistenceProvider persistenceProvider;
	private final @Nullable String entityName;
	private final PersistenceUnitUtil persistenceUnitUtil;
	private final JpaMetamodelMappingContext context;
	private final PersistentEntityIdentifier<T> identifier;

	/**
	 * Creates a new {@link JpaMetamodelEntityInformation} for the given domain class and {@link Metamodel}.
	 *
	 * @param domainClass must not be {@literal null}.
	 * @param metamodel must not be {@literal null}.
	 * @param persistenceUnitUtil must not be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	public JpaMetamodelEntityInformation(Class<T> domainClass, Metamodel metamodel,
			PersistenceUnitUtil persistenceUnitUtil) {

		super(domainClass);

		Assert.notNull(metamodel, "Metamodel must not be null");

		ManagedType<T> type = metamodel.managedType(domainClass);

		if (type == null) {
			throw new IllegalArgumentException("The given domain class can not be found in the given Metamodel");
		}

		this.entityName = type instanceof EntityType ? ((EntityType<?>) type).getName() : null;

		if (!(type instanceof IdentifiableType<T>)) {
			throw new IllegalArgumentException("The given domain class does not contain an id attribute");
		}

		this.context = new JpaMetamodelMappingContext(Collections.singleton(metamodel));
		this.persistentEntity = (JpaPersistentEntity<T>) context.getRequiredPersistentEntity(domainClass);
		this.identifier = persistentEntity.getIdentifier();
		this.persistenceProvider = PersistenceProvider.fromMetamodel(metamodel);

		Assert.notNull(persistenceUnitUtil, "PersistenceUnitUtil must not be null");
		this.persistenceUnitUtil = persistenceUnitUtil;
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

		this(entityType.getJavaType(), metamodel, persistenceUnitUtil);
	}

	@Override
	public String getEntityName() {
		return entityName != null ? entityName : super.getEntityName();
	}

	protected JpaPersistentEntity<T> getPersistentEntity() {
		return persistentEntity;
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable ID getId(T entity) {

		if (persistenceProvider.shouldUseAccessorFor(entity) || entity instanceof Tuple
				|| !getJavaType().isInstance(entity)) {
			return (ID) persistentEntity.getIdentifierAccessor(entity).getIdentifier();
		}

		if (!identifier.isComposite()) {
			return (ID) persistenceUnitUtil.getIdentifier(entity);
		}

		return (ID) persistenceUnitUtil.getIdentifier(entity);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		return (Class<ID>) identifier.getType();
	}

	@Override
	public @Nullable SingularAttribute<? super T, ?> getIdAttribute() {
		return identifier.getAttribute();
	}

	@Override
	public boolean hasCompositeId() {
		return identifier.isComposite();
	}

	@Override
	public Collection<String> getIdAttributePaths() {
		return identifier.getPropertyPaths();
	}

	@Override
	public @Nullable Object getCompositeIdAttributeValue(Object id, String idAttribute) {
		JpaPersistentProperty property = getIdentifierProperties().stream()
				.filter(candidate -> candidate.getName().equals(idAttribute)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No composite identifier property " + idAttribute));
		return getIdentifierPropertyAccessor(id).getProperty(property);
	}

	@Override
	public List<JpaPersistentProperty> getIdentifierProperties() {

		List<JpaPersistentProperty> properties = new ArrayList<>();
		JpaPersistentEntity<?> compositeId = context.getRequiredPersistentEntity(identifier.getType());
		for (JpaPersistentProperty jpaPersistentProperty : compositeId) {
			properties.add(jpaPersistentProperty);
		}
		return properties;
	}

	@Override
	public PersistentPropertyAccessor<?> getIdentifierPropertyAccessor(Object id) {

		if (hasCompositeId()) {
			JpaPersistentEntity<?> compositeId = context.getRequiredPersistentEntity(identifier.getType());
			return compositeId.getPropertyAccessor(id);
		}

		return persistentEntity.getPropertyAccessor(id);
	}

	@Override
	public boolean isNew(T entity) {
		return persistentEntity.isNew(entity);
	}

	@Override
	public Map<String, Object> getKeyset(Iterable<String> propertyPaths, T entity) {

		Function<String, Object> getter = getPropertyValueFunction(entity);

		Map<String, Object> keyset = new LinkedHashMap<>();

		if (hasCompositeId()) {
			for (String idAttributeName : getIdAttributePaths()) {
				keyset.put(idAttributeName, getter.apply(idAttributeName));
			}
		} else {
			keyset.put(getRequiredIdAttribute().getName(), getId(entity));
		}

		for (String propertyPath : propertyPaths) {
			keyset.put(propertyPath, getter.apply(propertyPath));
		}

		return keyset;
	}

	private Function<String, Object> getPropertyValueFunction(Object entity) {

		if (entity instanceof Tuple t) {
			return t::get;
		}

		DirectFieldAccessFallbackBeanWrapper wrapper = new DirectFieldAccessFallbackBeanWrapper(entity);
		return wrapper::getPropertyValue;
	}

}
