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
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.Version;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.ProxyIdAccessor;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.TargetAwareIdentifierAccessor;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

/**
 * Implementation of {@link JpaPersistentEntity}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Michael J. Simons
 * @since 1.3
 */
class JpaPersistentEntityImpl<T> extends BasicPersistentEntity<T, JpaPersistentProperty>
		implements JpaPersistentEntity<T> {

	private static final String INVALID_VERSION_ANNOTATION = "%s is annotated with "
			+ org.springframework.data.annotation.Version.class.getName() + " but needs to use "
			+ jakarta.persistence.Version.class.getName() + " to trigger optimistic locking correctly";

	private final ProxyIdAccessor proxyIdAccessor;
	private final JpaMetamodel metamodel;
	private final JpaPersistentEntityIdentifier entityIdentifier;
	private final Lazy<SingularAttribute<? super T, ?>> versionAttribute;

	/**
	 * Creates JPA mapping metadata with provider-aware identifier access.
	 *
	 * @param information must not be {@literal null}.
	 * @param proxyIdAccessor must not be {@literal null}.
	 * @param metamodel must not be {@literal null}.
	 */
	public JpaPersistentEntityImpl(TypeInformation<T> information, ProxyIdAccessor proxyIdAccessor,
			JpaMetamodel metamodel) {

		super(information, null);

		this.proxyIdAccessor = proxyIdAccessor;
		this.metamodel = metamodel;
		this.entityIdentifier = new JpaPersistentEntityIdentifier(information.getType());
		this.versionAttribute = Lazy.of(() -> findVersionAttribute(metamodel.getManagedType(getType())));
	}

	@Override
	protected @Nullable JpaPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(JpaPersistentProperty property) {
		return property.isIdProperty() && !getIdentifier().isComposite() ? property : null;
	}

	@Override
	public IdentifierAccessor getIdentifierAccessor(Object bean) {
		Assert.notNull(bean, "Bean must not be null");
		return new JpaProxyAwareIdentifierAccessor(bean);
	}

	@Override
	public PersistentEntityIdentifier<T> getIdentifier() {
		return entityIdentifier;
	}

	@Override
	protected IsNewStrategy getFallbackIsNewStrategy() {

		SingularAttribute<? super T, ?> version = versionAttribute.getNullable();
		if (version != null && !version.getJavaType().isPrimitive()) {
			return bean -> {

				JpaAttributeAccessor accessor = new JpaAttributeAccessor(bean);

				return (proxyIdAccessor.shouldUseAccessorFor(bean) ? accessor.getAttribute(version.getName()) == null
						: accessor.getAttribute(version) == null);
			};
		}

		JpaPersistentProperty property = getVersionProperty();
		if (version == null && property != null && !property.getType().isPrimitive()) {
			return bean -> getPropertyAccessor(bean).getProperty(property) == null;
		}

		return bean -> {
			Object id = getIdentifierAccessor(bean).getIdentifier();
			if (id == null || !getIdentifier().getType().isPrimitive()) {
				return id == null;
			}
			if (id instanceof Number number) {
				return number.longValue() == 0;
			}
			throw new IllegalArgumentException("Unsupported primitive id type " + getIdentifier().getType());
		};
	}

	private static <T> @Nullable SingularAttribute<? super T, ?> findVersionAttribute(@Nullable ManagedType<T> type) {

		if (!(type instanceof IdentifiableType<T> identifiableType)) {
			return null;
		}

		try {
			SingularAttribute<? super T, ?> version = identifiableType.getVersion(Object.class);
			if (version != null) {
				return version;
			}
		} catch (IllegalArgumentException e) {
			// Some providers require the exact version type.
		}

		for (SingularAttribute<? super T, ?> attribute : identifiableType.getSingularAttributes()) {
			if (attribute.isVersion()) {
				return attribute;
			}
		}

		return findVersionAttribute(identifiableType.getSupertype());
	}

	@Override
	public void verify() {

		super.verify();

		JpaPersistentProperty versionProperty = getVersionProperty();
		Assert.state(versionProperty == null || !versionProperty.isAnnotationPresent(Version.class),
				() -> String.format(INVALID_VERSION_ANNOTATION, versionProperty));
	}

	JpaMetamodel getMetamodel() {
		return metamodel;
	}

	private class JpaProxyAwareIdentifierAccessor extends TargetAwareIdentifierAccessor {

		private final Object bean;

		JpaProxyAwareIdentifierAccessor(Object bean) {

			super(bean);
			this.bean = bean;
		}

		@Override
		public @Nullable Object getIdentifier() {

			if (bean instanceof Persistable<?> persistable) {
				return persistable.getId();
			}
			if (proxyIdAccessor.shouldUseAccessorFor(bean)) {
				return proxyIdAccessor.getIdentifierFrom(bean);
			}
			IdMetadata<T> metadata = entityIdentifier.idMetadata.getNullable();
			return metadata == null ? JpaPersistentEntityImpl.super.getIdentifierAccessor(bean).getIdentifier()
					: metadata.getIdentifier(bean);
		}
	}

	/**
	 * JPA Identifier metadata accessor.
	 */
	private class JpaPersistentEntityIdentifier implements PersistentEntityIdentifier<T> {

		private final Lazy<IdMetadata<T>> idMetadata;

		public JpaPersistentEntityIdentifier(Class<T> domainType) {

			this.idMetadata = Lazy.of(() -> {
				ManagedType<T> type = metamodel.getManagedType(domainType);
				return type instanceof IdentifiableType<T> identifiableType
						? new IdMetadata<>(identifiableType, proxyIdAccessor)
						: null;
			});
		}

		@Override
		public Class<?> getType() {
			IdMetadata<T> metadata = idMetadata.getNullable();
			return metadata != null ? metadata.getType() : getRequiredIdProperty().getType();
		}

		@Override
		public @Nullable SingularAttribute<? super T, ?> getAttribute() {
			IdMetadata<T> metadata = idMetadata.getNullable();
			return metadata == null || metadata.attributes.isEmpty() ? null : metadata.attributes.iterator().next();
		}

		@Override
		public Collection<String> getPropertyPaths() {
			IdMetadata<T> metadata = idMetadata.getNullable();
			return metadata == null ? Collections.emptyList() : metadata.attributePaths;
		}

		@Override
		public boolean isComposite() {
			IdMetadata<T> metadata = idMetadata.getNullable();
			return metadata != null && !metadata.attributes.isEmpty() && !metadata.hasSimpleId();
		}

	}

	/**
	 * Identifier shape and access, including provider differences in ID-class metadata.
	 */
	private static class IdMetadata<T> {

		private final IdentifiableType<T> type;
		private final ProxyIdAccessor proxyIdAccessor;
		private final Set<SingularAttribute<? super T, ?>> idClassAttributes;
		private final Set<SingularAttribute<? super T, ?>> attributes;
		private final List<String> attributePaths;
		private @Nullable Class<?> idType;

		IdMetadata(IdentifiableType<T> type, ProxyIdAccessor proxyIdAccessor) {

			this.type = type;
			this.proxyIdAccessor = proxyIdAccessor;
			PersistenceProvider provider = proxyIdAccessor instanceof PersistenceProvider persistenceProvider
					? persistenceProvider
					: PersistenceProvider.GENERIC_JPA;
			this.idClassAttributes = provider.getIdClassAttributes(type);
			this.attributes = findAttributes(type);
			this.attributePaths = findAttributePaths(null, type);
		}

		private static <X> Set<SingularAttribute<? super X, ?>> findAttributes(IdentifiableType<X> source) {
			if (source.hasSingleIdAttribute()) {
				for (SingularAttribute<? super X, ?> attribute : source.getSingularAttributes()) {
					if (attribute.isId()) {
						return Collections.singleton(attribute);
					}
				}
			}
			try {
				return source.getIdClassAttributes();
			} catch (IllegalArgumentException e) {
				return source.getSingularAttributes().stream().filter(SingularAttribute::isId).collect(Collectors.toSet());
			}
		}

		private static <X> List<String> findAttributePaths(@Nullable String prefix, IdentifiableType<X> source) {

			List<String> paths = new ArrayList<>();
			for (SingularAttribute<? super X, ?> attribute : findAttributes(source)) {
				String name = prefix == null ? attribute.getName() : prefix + "." + attribute.getName();
				if (attribute.isAssociation() && attribute.getType() instanceof IdentifiableType<?> associatedType) {
					paths.addAll(findAttributePaths(name, associatedType));
				} else {
					paths.add(name);
				}
			}
			return paths;
		}

		boolean hasSimpleId() {
			return idClassAttributes.isEmpty() && attributes.size() == 1;
		}

		Class<?> getType() {

			if (idType != null) {
				return idType;
			}
			this.idType = tryExtractIdType();
			Assert.state(this.idType != null, () -> "Cannot resolve Id type from " + type);
			return this.idType;
		}

		private @Nullable Class<?> tryExtractIdType() {

			try {
				IdClass annotation = type.getJavaType() != null
						? AnnotationUtils.findAnnotation(type.getJavaType(), IdClass.class)
						: null;
				if (annotation != null) {
					return annotation.value();
				}
				Type<?> idType = type.getIdType();
				return idType == null ? attributes.size() > 1 ? type.getJavaType() : null : idType.getJavaType();
			} catch (IllegalStateException e) {
				// see https://hibernate.onjira.com/browse/HHH-6951
				return null;
			}
		}

		@Nullable
		Object getIdentifier(Object bean) {
			return getIdentifier(bean, false);
		}

		private @Nullable Object getIdentifier(Object bean, boolean preserveEmptyId) {

			JpaAttributeAccessor accessor = new JpaAttributeAccessor(bean);

			if (!hasSimpleId() && getType().isInstance(bean)) {
				return preserveEmptyId || hasIdValue(accessor) ? bean : null;
			}

			boolean idValueFound = preserveEmptyId;
			Map<String, Object> values = new LinkedHashMap<>();
			for (SingularAttribute<? super T, ?> attribute : attributes) {
				Object value = accessor.getAttribute(attribute);

				if (value != null) {
					idValueFound = true;

					if (attribute.isAssociation() && attribute.getType() instanceof IdentifiableType<?> associatedType
							&& requiresAssociatedId(attribute.getName(), value)) {
						value = proxyIdAccessor.shouldUseAccessorFor(value) ? proxyIdAccessor.getIdentifierFrom(value)
								: new IdMetadata<>(associatedType, proxyIdAccessor).getIdentifier(value, true);
					}
				}

				if (hasSimpleId()) {
					return value;
				}
				values.put(attribute.getName(), value);
			}

			if (!idValueFound) {
				return null;
			}

			if (getType().isRecord()) {
				Object[] arguments = Arrays.stream(getType().getRecordComponents()).map(it -> values.get(it.getName()))
						.toArray();
				return BeanUtils.instantiateClass(BeanUtils.getResolvableConstructor(getType()), arguments);
			}

			Object id = BeanUtils.instantiateClass(getType());
			DirectFieldAccessFallbackBeanWrapper wrapper = new DirectFieldAccessFallbackBeanWrapper(id);
			values.forEach((name, value) -> wrapper.setPropertyValue(name, value));
			return id;
		}

		boolean hasIdValue(JpaAttributeAccessor accessor) {
			return attributes.stream().anyMatch(it -> accessor.getAttribute(it) != null);
		}

		private boolean requiresAssociatedId(String name, Object value) {
			if (hasSimpleId()) {
				return true;
			}
			TypeInformation<?> property = TypeInformation.of(getType()).getProperty(name);
			return property == null || !property.getType().isInstance(value);
		}

	}

}
