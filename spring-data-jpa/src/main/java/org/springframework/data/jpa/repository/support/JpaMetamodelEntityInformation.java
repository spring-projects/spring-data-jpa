/*
 * Copyright 2011-2023 the original author or authors.
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

import jakarta.persistence.IdClass;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.lang.Nullable;
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

	private final IdMetadata<T> idMetadata;
	private final Optional<SingularAttribute<? super T, ?>> versionAttribute;
	private final Metamodel metamodel;
	private final @Nullable String entityName;
	private final PersistenceUnitUtil persistenceUnitUtil;

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
		this.metamodel = metamodel;

		ManagedType<T> type = metamodel.managedType(domainClass);

		if (type == null) {
			throw new IllegalArgumentException("The given domain class can not be found in the given Metamodel");
		}

		this.entityName = type instanceof EntityType ? ((EntityType<?>) type).getName() : null;

		if (!(type instanceof IdentifiableType)) {
			throw new IllegalArgumentException("The given domain class does not contain an id attribute");
		}

		IdentifiableType<T> identifiableType = (IdentifiableType<T>) type;

		this.idMetadata = new IdMetadata<>(identifiableType, PersistenceProvider.fromMetamodel(metamodel));
		this.versionAttribute = findVersionAttribute(identifiableType, metamodel);

		Assert.notNull(persistenceUnitUtil, "PersistenceUnitUtil must not be null");
		this.persistenceUnitUtil = persistenceUnitUtil;
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
	 * @return
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
	@Nullable
	@SuppressWarnings("unchecked")
	public ID getId(T entity) {

		// check if this is a proxy. If so use Proxy mechanics to access the id.
		PersistenceProvider persistenceProvider = PersistenceProvider.fromMetamodel(metamodel);

		if (persistenceProvider.shouldUseAccessorFor(entity)) {
			return (ID) persistenceProvider.getIdentifierFrom(entity);
		}

		// If it's a simple type, then immediately delegate to the provider
		if (idMetadata.hasSimpleId()) {
			return (ID) persistenceUnitUtil.getIdentifier(entity);
		}

		// otherwise, check if the complex id type has any partially filled fields
		BeanWrapper entityWrapper = new DirectFieldAccessFallbackBeanWrapper(entity);
		boolean partialIdValueFound = false;

		for (SingularAttribute<? super T, ?> attribute : idMetadata) {

			Object propertyValue = entityWrapper.getPropertyValue(attribute.getName());

			if (propertyValue != null) {
				partialIdValueFound = true;
			}
		}

		return partialIdValueFound ? (ID) persistenceUnitUtil.getIdentifier(entity) : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		return (Class<ID>) idMetadata.getType();
	}

	@Override
	public SingularAttribute<? super T, ?> getIdAttribute() {
		return idMetadata.getSimpleIdAttribute();
	}

	@Override
	public boolean hasCompositeId() {
		return !idMetadata.hasSimpleId();
	}

	@Override
	public Iterable<String> getIdAttributeNames() {

		List<String> attributeNames = new ArrayList<>(idMetadata.attributes.size());

		for (SingularAttribute<? super T, ?> attribute : idMetadata.attributes) {
			attributeNames.add(attribute.getName());
		}

		return attributeNames;
	}

	@Override
	public Object getCompositeIdAttributeValue(Object id, String idAttribute) {

		Assert.isTrue(hasCompositeId(), "Model must have a composite Id");

		return new DirectFieldAccessFallbackBeanWrapper(id).getPropertyValue(idAttribute);
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

	/**
	 * Simple value object to encapsulate id specific metadata.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private static class IdMetadata<T> implements Iterable<SingularAttribute<? super T, ?>> {

		private final IdentifiableType<T> type;
		private final Set<SingularAttribute<? super T, ?>> idClassAttributes;
		private final Set<SingularAttribute<? super T, ?>> attributes;
		private @Nullable Class<?> idType;

		@SuppressWarnings("unchecked")
		IdMetadata(IdentifiableType<T> source, PersistenceProvider persistenceProvider) {

			this.type = source;
			this.idClassAttributes = persistenceProvider.getIdClassAttributes(source);
			this.attributes = source.hasSingleIdAttribute()
					? Collections.singleton(source.getId(source.getIdType().getJavaType()))
					: source.getIdClassAttributes();
		}

		boolean hasSimpleId() {
			return idClassAttributes.isEmpty() && attributes.size() == 1;
		}

		public Class<?> getType() {

			if (idType != null) {
				return idType;
			}

			// lazy initialization of idType field with tolerable benign data-race
			this.idType = tryExtractIdTypeWithFallbackToIdTypeLookup();

			if (this.idType == null) {
				throw new IllegalStateException("Cannot resolve Id type from " + type);
			}

			return this.idType;
		}

		@Nullable
		private Class<?> tryExtractIdTypeWithFallbackToIdTypeLookup() {

			try {

				Class<?> idClassType = lookupIdClass(type);
				if (idClassType != null) {
					return idClassType;
				}

				Type<?> idType = type.getIdType();
				return idType == null ? null : idType.getJavaType();
			} catch (IllegalStateException e) {
				// see https://hibernate.onjira.com/browse/HHH-6951
				return null;
			}
		}

		@Nullable
		private static Class<?> lookupIdClass(IdentifiableType<?> type) {

			IdClass annotation = type.getJavaType() != null
					? AnnotationUtils.findAnnotation(type.getJavaType(), IdClass.class)
					: null;
			return annotation == null ? null : annotation.value();
		}

		SingularAttribute<? super T, ?> getSimpleIdAttribute() {
			return attributes.iterator().next();
		}

		@Override
		public Iterator<SingularAttribute<? super T, ?>> iterator() {
			return attributes.iterator();
		}
	}
}
