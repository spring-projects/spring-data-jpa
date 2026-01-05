/*
 * Copyright 2024-present the original author or authors.
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

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.data.core.PropertyPath;
import org.springframework.util.Assert;

/**
 * Utilities to create JPQL expressions, derived from {@link QueryUtils}.
 *
 * @author Mark Paluch
 */
class JpqlUtils {

	static JpqlQueryBuilder.PathExpression toExpressionRecursively(Metamodel metamodel, JpqlQueryBuilder.Origin source,
			Bindable<?> from, PropertyPath property) {
		return toExpressionRecursively(metamodel, source, from, property, false);
	}

	static JpqlQueryBuilder.PathExpression toExpressionRecursively(Metamodel metamodel, JpqlQueryBuilder.Origin source,
			Bindable<?> from, PropertyPath property, boolean isForSelection) {
		return JpqlExpressionFactory.INSTANCE.toExpressionRecursively(metamodel, source, from, property, isForSelection,
				false);
	}

	/**
	 * Expression Factory for JPQL queries that operate on String-based queries.
	 */
	static class JpqlExpressionFactory extends ExpressionFactorySupport {

		private static final JpqlExpressionFactory INSTANCE = new JpqlExpressionFactory();

		/**
		 * Creates an expression with proper inner and left joins by recursively navigating the path
		 *
		 * @param metamodel the JPA {@link Metamodel} used to resolve attribute types to {@link ManagedType}.
		 * @param source the {@link org.springframework.data.jpa.repository.query.JpqlQueryBuilder.Origin}
		 * @param from bindable from which the property is navigated.
		 * @param property the property path
		 * @param isForSelection is the property navigated for the selection or ordering part of the query?
		 * @param hasRequiredOuterJoin has a parent already required an outer join?
		 * @return the expression
		 */
		public JpqlQueryBuilder.PathExpression toExpressionRecursively(Metamodel metamodel, JpqlQueryBuilder.Origin source,
				Bindable<?> from, PropertyPath property, boolean isForSelection, boolean hasRequiredOuterJoin) {

			String segment = property.getSegment();

			boolean isLeafProperty = !property.hasNext();
			BindablePathResolver resolver = new BindablePathResolver(metamodel, from);
			boolean isRelationshipId = isRelationshipId(resolver, property);
			boolean requiresOuterJoin = requiresOuterJoin(resolver, property, isForSelection, hasRequiredOuterJoin,
					isLeafProperty, isRelationshipId);

			// if it does not require an outer join and is a leaf, simply get the segment
			if (!requiresOuterJoin && (isLeafProperty || isRelationshipId)) {
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

			ManagedType<?> managedTypeForModel = getManagedTypeForModel(from);
			Attribute<?, ?> nextAttribute = getModelForPath(metamodel, property, managedTypeForModel, from);

			if (nextAttribute == null) {
				throw new IllegalStateException("Binding property is null");
			}

			return toExpressionRecursively(metamodel, joinSource, (Bindable<?>) nextAttribute, nextProperty, isForSelection,
					requiresOuterJoin);
		}

		private static @Nullable Attribute<?, ?> getModelForPath(@Nullable Metamodel metamodel, PropertyPath path,
				@Nullable ManagedType<?> managedType, @Nullable Bindable<?> fallback) {

			String segment = path.getSegment();
			if (managedType != null) {
				try {
					return managedType.getAttribute(segment);
				} catch (IllegalArgumentException ex) {
					// ManagedType may be erased for some vendor if the attribute is declared as generic
				}
			}

			if (metamodel != null && fallback != null) {

				Class<?> fallbackType = fallback.getBindableJavaType();
				try {
					return metamodel.managedType(fallbackType).getAttribute(segment);
				} catch (IllegalArgumentException e) {
					// nothing to do here
				}
			}

			return null;
		}

		record BindablePathResolver(Metamodel metamodel,
				Bindable<?> bindable) implements ExpressionFactorySupport.ModelPathResolver {

			@Override
			public @Nullable Bindable<?> resolve(PropertyPath propertyPath) {

				Attribute<?, ?> attribute = resolveAttribute(propertyPath);
				return attribute instanceof Bindable<?> b ? b : null;
			}

			private @Nullable Attribute<?, ?> resolveAttribute(PropertyPath propertyPath) {
				ManagedType<?> managedType = getManagedTypeForModel(bindable);
				return getModelForPath(metamodel, propertyPath, managedType, bindable);
			}

			@Override
			@SuppressWarnings("NullAway")
			public @Nullable Bindable<?> resolveNext(PropertyPath propertyPath) {

				Assert.state(propertyPath.hasNext(), "PropertyPath must contain at least one element");

				Attribute<?, ?> propertyPathModel = resolveAttribute(propertyPath);
				ManagedType<?> propertyPathManagedType = getManagedTypeForModel(propertyPathModel);
				Attribute<?, ?> next = getModelForPath(metamodel, Objects.requireNonNull(propertyPath.next()),
						propertyPathManagedType, null);

				return next instanceof Bindable<?> b ? b : null;
			}

		}

	}

}
