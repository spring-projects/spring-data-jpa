/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.ELEMENT_COLLECTION;
import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_MANY;
import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_ONE;
import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_MANY;
import static jakarta.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_ONE;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Mark Paluch
 */
class JpqlUtils {

	private static final Map<PersistentAttributeType, Class<? extends Annotation>> ASSOCIATION_TYPES;

	static {
		Map<PersistentAttributeType, Class<? extends Annotation>> persistentAttributeTypes = new HashMap<>();
		persistentAttributeTypes.put(ONE_TO_ONE, OneToOne.class);
		persistentAttributeTypes.put(ONE_TO_MANY, null);
		persistentAttributeTypes.put(MANY_TO_ONE, ManyToOne.class);
		persistentAttributeTypes.put(MANY_TO_MANY, null);
		persistentAttributeTypes.put(ELEMENT_COLLECTION, null);

		ASSOCIATION_TYPES = Collections.unmodifiableMap(persistentAttributeTypes);
	}

	static JpqlQueryBuilder.PathAndOrigin toExpressionRecursively(Metamodel metamodel, JpqlQueryBuilder.Origin source,
			Bindable<?> from, PropertyPath property) {
		return toExpressionRecursively(metamodel, source, from, property, false);
	}

	static JpqlQueryBuilder.PathAndOrigin toExpressionRecursively(Metamodel metamodel, JpqlQueryBuilder.Origin source,
			Bindable<?> from, PropertyPath property, boolean isForSelection) {
		return toExpressionRecursively(metamodel, source, from, property, isForSelection, false);
	}

	/**
	 * Creates an expression with proper inner and left joins by recursively navigating the path
	 *
	 * @param from the {@link From}
	 * @param property the property path
	 * @param isForSelection is the property navigated for the selection or ordering part of the query?
	 * @param hasRequiredOuterJoin has a parent already required an outer join?
	 * @return the expression
	 */
	@SuppressWarnings("unchecked")
	static JpqlQueryBuilder.PathAndOrigin toExpressionRecursively(Metamodel metamodel, JpqlQueryBuilder.Origin source,
			Bindable<?> from, PropertyPath property, boolean isForSelection, boolean hasRequiredOuterJoin) {

		String segment = property.getSegment();

		boolean isLeafProperty = !property.hasNext();

		boolean requiresOuterJoin = requiresOuterJoin(metamodel, source, from, property, isForSelection,
				hasRequiredOuterJoin);

		// if it does not require an outer join and is a leaf, simply get the segment
		if (!requiresOuterJoin && isLeafProperty) {
			return new JpqlQueryBuilder.PathAndOrigin(property, source, false);
		}

		// get or create the join
		JpqlQueryBuilder.Join joinSource = requiresOuterJoin ? JpqlQueryBuilder.leftJoin(source, segment)
				: JpqlQueryBuilder.innerJoin(source, segment);
//		JoinType joinType = requiresOuterJoin ? JoinType.LEFT : JoinType.INNER;
//		Join<?, ?> join = QueryUtils.getOrCreateJoin(from, segment, joinType);

//
		// if it's a leaf, return the join
		if (isLeafProperty) {
			return new JpqlQueryBuilder.PathAndOrigin(property, joinSource, true);
		}

		PropertyPath nextProperty = Objects.requireNonNull(property.next(), "An element of the property path is null");

//		ManagedType<?> managedType = ;
		Bindable<?> managedTypeForModel = (Bindable<?>) getManagedTypeForModel(from);
//		Attribute<?, ?> joinAttribute = getModelForPath(metamodel, property, getManagedTypeForModel(from), null);
		// recurse with the next property
		return toExpressionRecursively(metamodel, joinSource, managedTypeForModel, nextProperty, isForSelection, requiresOuterJoin);
	}

	/**
	 * Checks if this attribute requires an outer join. This is the case e.g. if it hadn't already been fetched with an
	 * inner join and if it's an optional association, and if previous paths has already required outer joins. It also
	 * ensures outer joins are used even when Hibernate defaults to inner joins (HHH-12712 and HHH-12999)
	 *
	 * @param metamodel
	 * @param source
	 * @param bindable
	 * @param propertyPath
	 * @param isForSelection
	 * @param hasRequiredOuterJoin
	 * @return
	 */
	static boolean requiresOuterJoin(Metamodel metamodel, JpqlQueryBuilder.Origin source, Bindable<?> bindable,
			PropertyPath propertyPath, boolean isForSelection, boolean hasRequiredOuterJoin) {

		ManagedType<?> managedType = getManagedTypeForModel(bindable);
		Attribute<?, ?> attribute = getModelForPath(metamodel, propertyPath, managedType, bindable);

		boolean isPluralAttribute = bindable instanceof PluralAttribute;
		if (attribute == null) {
			return isPluralAttribute;
		}

		if (!ASSOCIATION_TYPES.containsKey(attribute.getPersistentAttributeType())) {
			return false;
		}

		boolean isCollection = attribute.isCollection();

		// if this path is an optional one to one attribute navigated from the not owning side we also need an
		// explicit outer join to avoid https://hibernate.atlassian.net/browse/HHH-12712
		// and https://github.com/eclipse-ee4j/jpa-api/issues/170
		boolean isInverseOptionalOneToOne = PersistentAttributeType.ONE_TO_ONE == attribute.getPersistentAttributeType()
				&& StringUtils.hasText(getAnnotationProperty(attribute, "mappedBy", ""));

		boolean isLeafProperty = !propertyPath.hasNext();
		if (isLeafProperty && !isForSelection && !isCollection && !isInverseOptionalOneToOne && !hasRequiredOuterJoin) {
			return false;
		}

		return hasRequiredOuterJoin || getAnnotationProperty(attribute, "optional", true);
	}

	@Nullable
	private static <T> T getAnnotationProperty(Attribute<?, ?> attribute, String propertyName, T defaultValue) {

		Class<? extends Annotation> associationAnnotation = ASSOCIATION_TYPES.get(attribute.getPersistentAttributeType());

		if (associationAnnotation == null) {
			return defaultValue;
		}

		Member member = attribute.getJavaMember();

		if (!(member instanceof AnnotatedElement annotatedMember)) {
			return defaultValue;
		}

		Annotation annotation = AnnotationUtils.getAnnotation(annotatedMember, associationAnnotation);
		return annotation == null ? defaultValue : (T) AnnotationUtils.getValue(annotation, propertyName);
	}

	@Nullable
	private static ManagedType<?> getManagedTypeForModel(Bindable<?> model) {

		if (model instanceof ManagedType<?> managedType) {
			return managedType;
		}

		if (!(model instanceof SingularAttribute<?, ?> singularAttribute)) {
			return null;
		}

		return singularAttribute.getType() instanceof ManagedType<?> managedType ? managedType : null;
	}

	@Nullable
	private static Attribute<?, ?> getModelForPath(Metamodel metamodel, PropertyPath path,
			@Nullable ManagedType<?> managedType, Bindable<?> fallback) {

		String segment = path.getSegment();
		if (managedType != null) {
			try {
				return managedType.getAttribute(segment);
			} catch (IllegalArgumentException ex) {
				// ManagedType may be erased for some vendor if the attribute is declared as generic
			}
		}

		Class<?> fallbackType = fallback.getBindableJavaType();
		try {
			return metamodel.managedType(fallbackType).getAttribute(segment);
		} catch (IllegalArgumentException e) {

		}

		return null;
	}
}
