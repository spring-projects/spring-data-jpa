/*
 * Copyright 2025-present the original author or authors.
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
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.ProxyIdAccessor;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.Assert;

/**
 * Shared utility to resolve identifier metadata and values in a consistent way.
 *
 * @author Taehyun Choi
 * @since 4.1
 */
public final class JpaIdentifierResolver<T> {

	private final IdMetadata<T> idMetadata;
	private final PersistenceUnitUtil persistenceUnitUtil;
	private final ProxyIdAccessor proxyIdAccessor;

	public JpaIdentifierResolver(IdentifiableType<T> type, PersistenceProvider persistenceProvider,
			PersistenceUnitUtil persistenceUnitUtil) {

		Assert.notNull(type, "IdentifiableType must not be null");
		Assert.notNull(persistenceProvider, "PersistenceProvider must not be null");
		Assert.notNull(persistenceUnitUtil, "PersistenceUnitUtil must not be null");

		this.idMetadata = new IdMetadata<>(type, persistenceProvider);
		this.persistenceUnitUtil = persistenceUnitUtil;
		this.proxyIdAccessor = persistenceProvider;
	}

	public @Nullable Object getId(T entity) {

		return getProxyAwareIdentifier(entity, proxyIdAccessor, () -> getIdInternal(entity));
	}

	public Class<?> getIdType() {
		return idMetadata.getType();
	}

	public SingularAttribute<? super T, ?> getIdAttribute() {
		return idMetadata.getSimpleIdAttribute();
	}

	public boolean hasCompositeId() {
		return !idMetadata.hasSimpleId();
	}

	public Collection<String> getIdAttributeNames() {

		List<String> attributeNames = new ArrayList<>(idMetadata.attributes.size());

		for (SingularAttribute<? super T, ?> attribute : idMetadata.attributes) {
			attributeNames.add(attribute.getName());
		}

		Collections.sort(attributeNames);

		return attributeNames;
	}

	public @Nullable Object getCompositeIdAttributeValue(Object id, String idAttribute) {
		Assert.isTrue(hasCompositeId(), "Model must have a composite Id");
		return new DirectFieldAccessFallbackBeanWrapper(id).getPropertyValue(idAttribute);
	}

	public Map<String, Object> getKeyset(Iterable<String> propertyPaths, T entity) {

		Function<String, Object> getter = getPropertyValueFunction(entity);

		Map<String, Object> keyset = new LinkedHashMap<>();

		if (hasCompositeId()) {
			for (String idAttributeName : getIdAttributeNames()) {
				keyset.put(idAttributeName, getter.apply(idAttributeName));
			}
		} else {
			keyset.put(getIdAttribute().getName(), getId(entity));
		}

		for (String propertyPath : propertyPaths) {
			keyset.put(propertyPath, getter.apply(propertyPath));
		}

		return keyset;
	}

	public static @Nullable Object getProxyAwareIdentifier(Object entity, ProxyIdAccessor proxyIdAccessor,
			Supplier<Object> fallback) {

		return proxyIdAccessor.shouldUseAccessorFor(entity) ? proxyIdAccessor.getIdentifierFrom(entity) : fallback.get();
	}

	private @Nullable Object getIdInternal(T entity) {

		// If it's a simple type, then immediately delegate to the provider
		if (idMetadata.hasSimpleId()) {

			if (entity instanceof Tuple t) {
				return t.get(idMetadata.getSimpleIdAttribute().getName());
			}

			if (idMetadata.getJavaType().isInstance(entity)) {
				return persistenceUnitUtil.getIdentifier(entity);
			}
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

		return partialIdValueFound ? persistenceUnitUtil.getIdentifier(entity) : null;
	}

	private Function<String, Object> getPropertyValueFunction(Object entity) {

		if (entity instanceof Tuple t) {
			return t::get;
		}

		// TODO: Proxy handling requires more elaborate refactoring, see
		// https://github.com/spring-projects/spring-data-jpa/issues/2784
		BeanWrapper entityWrapper = new DirectFieldAccessFallbackBeanWrapper(entity);

		return entityWrapper::getPropertyValue;
	}

	private static class IdMetadata<T> implements Iterable<SingularAttribute<? super T, ?>> {

		private final IdentifiableType<T> type;
		private final Set<SingularAttribute<? super T, ?>> idClassAttributes;
		private final Set<SingularAttribute<? super T, ?>> attributes;
		private @Nullable Class<?> idType;

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

		private @Nullable Class<?> tryExtractIdTypeWithFallbackToIdTypeLookup() {

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

		private static @Nullable Class<?> lookupIdClass(IdentifiableType<?> type) {

			IdClass annotation = type.getJavaType() != null
					? AnnotationUtils.findAnnotation(type.getJavaType(), IdClass.class)
					: null;
			return annotation == null ? null : annotation.value();
		}

		Class<?> getJavaType() {
			return type.getJavaType();
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
