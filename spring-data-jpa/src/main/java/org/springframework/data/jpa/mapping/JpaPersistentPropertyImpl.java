/*
 * Copyright 2012-2023 the original author or authors.
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

import jakarta.persistence.*;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link JpaPersistentProperty} implementation using a JPA {@link Metamodel}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Erik Pellizzon
 * @since 1.3
 */
class JpaPersistentPropertyImpl extends AnnotationBasedPersistentProperty<JpaPersistentProperty>
		implements JpaPersistentProperty {

	private static final Collection<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS;
	private static final Collection<Class<? extends Annotation>> ID_ANNOTATIONS;
	private static final Collection<Class<? extends Annotation>> UPDATEABLE_ANNOTATIONS;

	static {

		Set<Class<? extends Annotation>> annotations = new HashSet<>();
		annotations.add(OneToMany.class);
		annotations.add(OneToOne.class);
		annotations.add(ManyToMany.class);
		annotations.add(ManyToOne.class);

		ASSOCIATION_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<>();
		annotations.add(Id.class);
		annotations.add(EmbeddedId.class);

		ID_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<>();
		annotations.add(Column.class);
		annotations.add(OrderColumn.class);

		UPDATEABLE_ANNOTATIONS = Collections.unmodifiableSet(annotations);
	}

	private final @Nullable Boolean usePropertyAccess;
	private final @Nullable TypeInformation<?> associationTargetType;
	private final boolean updateable;

	private final Lazy<Boolean> isIdProperty;
	private final Lazy<Boolean> isAssociation;
	private final Lazy<Boolean> isEntity;

	/**
	 * Creates a new {@link JpaPersistentPropertyImpl}
	 *
	 * @param metamodel must not be {@literal null}.
	 * @param property must not be {@literal null}.
	 * @param owner must not be {@literal null}.
	 * @param simpleTypeHolder must not be {@literal null}.
	 */
	public JpaPersistentPropertyImpl(JpaMetamodel metamodel, Property property,
			PersistentEntity<?, JpaPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		Assert.notNull(metamodel, "Metamodel must not be null");

		this.isAssociation = Lazy.of(() -> super.isAssociation() //
				|| ASSOCIATION_ANNOTATIONS.stream().anyMatch(this::isAnnotationPresent));
		this.usePropertyAccess = detectPropertyAccess();
		this.associationTargetType = detectAssociationTargetType();
		this.updateable = detectUpdatability();

		this.isIdProperty = Lazy.of(() -> ID_ANNOTATIONS.stream().anyMatch(it -> isAnnotationPresent(it)) //
				|| metamodel.isSingleIdAttribute(getOwner().getType(), getName(), getType()));
		this.isEntity = Lazy.of(() -> metamodel.isMappedType(getActualType()));
	}

	@Override
	public Class<?> getActualType() {

		return associationTargetType != null //
				? associationTargetType.getType() //
				: super.getActualType();
	}

	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityTypeInformation() {

		return associationTargetType != null //
				? Collections.singleton(associationTargetType) //
				: super.getPersistentEntityTypeInformation();
	}

	@Override
	public boolean isIdProperty() {
		return isIdProperty.get();
	}

	@Override
	public boolean isEntity() {
		return isEntity.get();
	}

	@Override
	public boolean isAssociation() {
		return isAssociation.get();
	}

	@Override
	public boolean isTransient() {
		return isAnnotationPresent(Transient.class) || super.isTransient();
	}

	@Override
	protected Association<JpaPersistentProperty> createAssociation() {
		return new Association<>(this, null);
	}

	@Override
	public boolean usePropertyAccess() {
		return usePropertyAccess != null ? usePropertyAccess : super.usePropertyAccess();
	}

	@Override
	public boolean isVersionProperty() {
		return isAnnotationPresent(Version.class);
	}

	@Override
	public boolean isWritable() {
		return updateable && super.isWritable();
	}

	@Override
	public boolean isEmbeddable() {
		return isAnnotationPresent(Embedded.class) || hasActualTypeAnnotation(Embeddable.class);
	}

	@Override
	public TypeInformation<?> getAssociationTargetTypeInformation() {

		if (!isAssociation()) {
			return null;
		}

		if (associationTargetType != null) {
			return associationTargetType;
		}

		TypeInformation<?> targetType = super.getAssociationTargetTypeInformation();

		return targetType != null ? targetType : getActualTypeInformation();
	}

	/**
	 * Looks up both Spring Data's and JPA's access type definition annotations on the property or type level to determine
	 * the access type to be used. Will consider property-level annotations over type-level ones, favoring the Spring Data
	 * ones over the JPA ones if found on the same level. Returns {@literal null} if no explicit annotation can be found
	 * falling back to the defaults implemented in the super class.
	 *
	 * @return
	 */
	@Nullable
	private Boolean detectPropertyAccess() {

		org.springframework.data.annotation.AccessType accessType = findAnnotation(
				org.springframework.data.annotation.AccessType.class);

		if (accessType != null) {
			return Type.PROPERTY.equals(accessType.value());
		}

		Access access = findAnnotation(Access.class);

		if (access != null) {
			return AccessType.PROPERTY.equals(access.value());
		}

		accessType = findPropertyOrOwnerAnnotation(org.springframework.data.annotation.AccessType.class);

		if (accessType != null) {
			return Type.PROPERTY.equals(accessType.value());
		}

		access = findPropertyOrOwnerAnnotation(Access.class);

		if (access != null) {
			return AccessType.PROPERTY.equals(access.value());
		}

		return null;
	}

	/**
	 * Inspects the association annotations on the property and returns the target entity type if specified.
	 *
	 * @return
	 */
	@Nullable
	private TypeInformation<?> detectAssociationTargetType() {

		if (!isAssociation()) {
			return null;
		}

		for (Class<? extends Annotation> annotationType : ASSOCIATION_ANNOTATIONS) {

			Annotation annotation = findAnnotation(annotationType);

			if (annotation == null) {
				continue;
			}

			Object entityValue = AnnotationUtils.getValue(annotation, "targetEntity");

			if (entityValue == null || entityValue.equals(void.class)) {
				continue;
			}

			return TypeInformation.of((Class<?>) entityValue);
		}

		return null;
	}

	/**
	 * Checks whether {@code updatable} attribute of any of the {@link #UPDATEABLE_ANNOTATIONS} is configured to
	 * {@literal true}.
	 *
	 * @return
	 */
	private boolean detectUpdatability() {

		for (Class<? extends Annotation> annotationType : UPDATEABLE_ANNOTATIONS) {

			Annotation annotation = findAnnotation(annotationType);

			if (annotation == null) {
				continue;
			}

			final Object updatable = AnnotationUtils.getValue(annotation, "updatable");
			return (boolean) (updatable == null ? true : updatable);
		}

		return true;
	}
}
