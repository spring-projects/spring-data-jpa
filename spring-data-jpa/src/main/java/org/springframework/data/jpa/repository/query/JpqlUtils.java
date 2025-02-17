/*
 * Copyright 2024-2025 the original author or authors.
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

import jakarta.persistence.criteria.From;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;

import java.util.Objects;

import org.springframework.data.mapping.PropertyPath;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Mark Paluch
 */
class JpqlUtils {

	static JpqlQueryBuilder.PathExpression toExpressionRecursively(@Nullable Metamodel metamodel, JpqlQueryBuilder.Origin source,
			Bindable<?> from, PropertyPath property) {
		return toExpressionRecursively(metamodel, source, from, property, false);
	}

	static JpqlQueryBuilder.PathExpression toExpressionRecursively(@Nullable Metamodel metamodel, JpqlQueryBuilder.Origin source,
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
	static JpqlQueryBuilder.PathExpression toExpressionRecursively(@Nullable Metamodel metamodel, JpqlQueryBuilder.Origin source,
			Bindable<?> from, PropertyPath property, boolean isForSelection, boolean hasRequiredOuterJoin) {

		String segment = property.getSegment();

		boolean isLeafProperty = !property.hasNext();
		boolean requiresOuterJoin = requiresOuterJoin(metamodel, from, property, isForSelection, hasRequiredOuterJoin);

		// if it does not require an outer join and is a leaf, simply get the segment
		if (!requiresOuterJoin && isLeafProperty) {
			return new JpqlQueryBuilder.PathAndOrigin(property, source, false);
		}

		// get or create the join
		JpqlQueryBuilder.Join joinSource = requiresOuterJoin ? JpqlQueryBuilder.leftJoin(source, segment)
				: JpqlQueryBuilder.innerJoin(source, segment);

		// if it's a leaf, return the join
		if (isLeafProperty) {
			return new JpqlQueryBuilder.PathAndOrigin(property, joinSource, true);
		}

		PropertyPath nextProperty = Objects.requireNonNull(property.next(), "An element of the property path is null");

		ManagedType<?> managedTypeForModel = QueryUtils.getManagedTypeForModel(from);
		Attribute<?, ?> nextAttribute = getModelForPath(metamodel, property, managedTypeForModel, from);

		if(nextAttribute == null) {
			throw new IllegalStateException("Binding property is null");
		}

		return toExpressionRecursively(metamodel, joinSource, (Bindable<?>) nextAttribute, nextProperty, isForSelection,
				requiresOuterJoin);
	}

	/**
	 * Checks if this attribute requires an outer join. This is the case e.g. if it hadn't already been fetched with an
	 * inner join and if it's an optional association, and if previous paths has already required outer joins. It also
	 * ensures outer joins are used even when Hibernate defaults to inner joins (HHH-12712 and HHH-12999)
	 *
	 * @param metamodel
	 * @param bindable
	 * @param propertyPath
	 * @param isForSelection
	 * @param hasRequiredOuterJoin
	 * @return
	 */
	static boolean requiresOuterJoin(@Nullable Metamodel metamodel, Bindable<?> bindable, PropertyPath propertyPath,
			boolean isForSelection, boolean hasRequiredOuterJoin) {

		ManagedType<?> managedType = QueryUtils.getManagedTypeForModel(bindable);
		Attribute<?, ?> attribute = getModelForPath(metamodel, propertyPath, managedType, bindable);

		boolean isPluralAttribute = bindable instanceof PluralAttribute;
		if (attribute == null) {
			return isPluralAttribute;
		}

		if (!QueryUtils.ASSOCIATION_TYPES.containsKey(attribute.getPersistentAttributeType())) {
			return false;
		}

		boolean isCollection = attribute.isCollection();

		// if this path is an optional one to one attribute navigated from the not owning side we also need an
		// explicit outer join to avoid https://hibernate.atlassian.net/browse/HHH-12712
		// and https://github.com/eclipse-ee4j/jpa-api/issues/170
		boolean isInverseOptionalOneToOne = PersistentAttributeType.ONE_TO_ONE == attribute.getPersistentAttributeType()
				&& StringUtils.hasText(QueryUtils.getAnnotationProperty(attribute, "mappedBy", ""));

		boolean isLeafProperty = !propertyPath.hasNext();
		if (isLeafProperty && !isForSelection && !isCollection && !isInverseOptionalOneToOne && !hasRequiredOuterJoin) {
			return false;
		}

		return hasRequiredOuterJoin || QueryUtils.getAnnotationProperty(attribute, "optional", true);
	}

	private static @Nullable Attribute<?, ?> getModelForPath(@Nullable Metamodel metamodel, PropertyPath path,
			@Nullable ManagedType<?> managedType, Bindable<?> fallback) {

		String segment = path.getSegment();
		if (managedType != null) {
			try {
				return managedType.getAttribute(segment);
			} catch (IllegalArgumentException ex) {
				// ManagedType may be erased for some vendor if the attribute is declared as generic
			}
		}

		if(metamodel != null) {

			Class<?> fallbackType = fallback.getBindableJavaType();
			try {
				return metamodel.managedType(fallbackType).getAttribute(segment);
			} catch (IllegalArgumentException e) {
				// nothing to do here
			}
		}

		return null;
	}
}
