/*
 * Copyright 2012-2017 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.persistence.metamodel.Metamodel;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * {@link JpaPersistentProperty} implementation usind a JPA {@link Metamodel}.
 * 
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @since 1.3
 */
class JpaPersistentPropertyImpl extends AnnotationBasedPersistentProperty<JpaPersistentProperty>
		implements JpaPersistentProperty {

	private static final Collection<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS;
	private static final Collection<Class<? extends Annotation>> ID_ANNOTATIONS;
	private static final Collection<Class<? extends Annotation>> UPDATEABLE_ANNOTATIONS;

	static {

		Set<Class<? extends Annotation>> annotations = new HashSet<Class<? extends Annotation>>();
		annotations.add(OneToMany.class);
		annotations.add(OneToOne.class);
		annotations.add(ManyToMany.class);
		annotations.add(ManyToOne.class);
		annotations.add(Embedded.class);

		ASSOCIATION_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<Class<? extends Annotation>>();
		annotations.add(Id.class);
		annotations.add(EmbeddedId.class);

		ID_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<Class<? extends Annotation>>();
		annotations.add(Column.class);
		annotations.add(OrderColumn.class);

		UPDATEABLE_ANNOTATIONS = Collections.unmodifiableSet(annotations);
	}

	private final Boolean usePropertyAccess;
	private final TypeInformation<?> associationTargetType;
	private final boolean updateable;
	private final JpaMetamodel metamodel;

	/**
	 * Creates a new {@link JpaPersistentPropertyImpl}
	 * 
	 * @param metamodel must not be {@literal null}.
	 * @param property must not be {@literal null}.
	 * @param owner must not be {@literal null}.
	 * @param simpleTypeHolder must not be {@literal null}.
	 */
	public JpaPersistentPropertyImpl(Metamodel metamodel, Property property,
			PersistentEntity<?, JpaPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		Assert.notNull(metamodel, "Metamodel must not be null!");

		this.usePropertyAccess = detectPropertyAccess();
		this.associationTargetType = isAssociation() ? detectAssociationTargetType() : null;
		this.updateable = detectUpdatability();
		this.metamodel = new JpaMetamodel(metamodel);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#getActualType()
	 */
	@Override
	public Class<?> getActualType() {
		return associationTargetType == null ? super.getActualType() : associationTargetType.getType();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#getPersistentEntityType()
	 */
	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityType() {
		return associationTargetType == null ? super.getPersistentEntityType()
				: Collections.singleton(associationTargetType);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#isIdProperty()
	 */
	@Override
	public boolean isIdProperty() {

		for (Class<? extends Annotation> annotation : ID_ANNOTATIONS) {
			if (isAnnotationPresent(annotation)) {
				return true;
			}
		}

		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#isEntity()
	 */
	@Override
	public boolean isEntity() {
		return metamodel.isJpaManaged(getActualType());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#isAssociation()
	 */
	@Override
	public boolean isAssociation() {

		for (Class<? extends Annotation> annotationType : ASSOCIATION_ANNOTATIONS) {
			if (findAnnotation(annotationType) != null) {
				return true;
			}
		}

		if (getType().isAnnotationPresent(Embeddable.class)) {
			return true;
		}

		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#isTransient()
	 */
	@Override
	public boolean isTransient() {
		return isAnnotationPresent(Transient.class) || super.isTransient();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#createAssociation()
	 */
	@Override
	protected Association<JpaPersistentProperty> createAssociation() {
		return new Association<JpaPersistentProperty>(this, null);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#usePropertyAccess()
	 */
	@Override
	public boolean usePropertyAccess() {
		return usePropertyAccess != null ? usePropertyAccess : super.usePropertyAccess();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#isVersionProperty()
	 */
	@Override
	public boolean isVersionProperty() {
		return isAnnotationPresent(Version.class);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return updateable && super.isWritable();
	}

	/**
	 * Looks up both Spring Data's and JPA's access type definition annotations on the property or type level to determine
	 * the access type to be used. Will consider property-level annotations over type-level ones, favoring the Spring Data
	 * ones over the JPA ones if found on the same level. Returns {@literal null} if no explicit annotation can be found
	 * falling back to the defaults implemented in the super class.
	 * 
	 * @return
	 */
	private Boolean detectPropertyAccess() {

		Optional<org.springframework.data.annotation.AccessType> accessType = findAnnotation(
				org.springframework.data.annotation.AccessType.class);

		if (accessType.isPresent()) {
			return Type.PROPERTY.equals(accessType.get().value());
		}

		Optional<Access> access = findAnnotation(Access.class);

		if (access.isPresent()) {
			return AccessType.PROPERTY.equals(access.get().value());
		}

		accessType = findPropertyOrOwnerAnnotation(org.springframework.data.annotation.AccessType.class);

		if (accessType.isPresent()) {
			return Type.PROPERTY.equals(accessType.get().value());
		}

		access = findPropertyOrOwnerAnnotation(Access.class);
		return access.map(t -> AccessType.PROPERTY.equals(t.value())).orElse(null);
	}

	/**
	 * Inspects the association annotations on the property and returns the target entity type if specified.
	 * 
	 * @return
	 */
	private TypeInformation<?> detectAssociationTargetType() {

		for (Class<? extends Annotation> associationAnnotation : ASSOCIATION_ANNOTATIONS) {

			Optional<? extends Annotation> annotation = findAnnotation(associationAnnotation);
			if(annotation.isPresent()) {

				Object targetEntity = AnnotationUtils.getValue(annotation.get(), "targetEntity");

				if (targetEntity != null && !void.class.equals(targetEntity)) {
					return ClassTypeInformation.from((Class<?>) targetEntity);
				}
			}
		}

		return null;
	}

	/**
	 * Checks whether {@code updateable} attribute of any of the {@link #UPDATEABLE_ANNOTATIONS} is configured to
	 * {@literal true}.
	 * 
	 * @return
	 */
	private final boolean detectUpdatability() {

		for (Class<? extends Annotation> annotationType : UPDATEABLE_ANNOTATIONS) {

			Optional<? extends Annotation> annotation = findAnnotation(annotationType);

			if (annotation.isPresent() && AnnotationUtils.getValue(annotation.get(), "updatable").equals(Boolean.FALSE)) {
				return false;
			}
		}

		return true;
	}

}
