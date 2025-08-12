/*
 * Copyright 2025 the original author or authors.
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

import java.util.function.Supplier;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link PathOptimizationStrategy} that optimizes
 * foreign key access for @ManyToOne and owning side of @OneToOne relationships.
 *
 * @author Hyunjoon Kim
 * @since 3.5
 */
public class DefaultPathOptimizationStrategy implements PathOptimizationStrategy {

	@Override
	public boolean canOptimizeForeignKeyAccess(PropertyPath path, Bindable<?> bindable, MetamodelContext context) {
		return isRelationshipId(path, bindable);
	}

	@Override
	public boolean isAssociationId(PropertyPath path, MetamodelContext context) {
		// For consistency with canOptimizeForeignKeyAccess, delegate to the same logic
		return isRelationshipId(path, null);
	}

	/**
	 * Checks if this property path is referencing to relationship id.
	 * This implementation follows the approach from PR #3922, using 
	 * SingularAttribute.isId() for reliable ID detection.
	 *
	 * @param path the property path
	 * @param bindable the {@link Bindable} to check for attribute model (can be null)
	 * @return whether the path references a relationship id
	 */
	private boolean isRelationshipId(PropertyPath path, Bindable<?> bindable) {
		if (!path.hasNext()) {
			return false;
		}

		// This logic is adapted from PR #3922's QueryUtils.isRelationshipId method
		if (bindable != null) {
			ManagedType<?> managedType = QueryUtils.getManagedTypeForModel(bindable);
			Bindable<?> propertyPathModel = getModelForPath(path, managedType, () -> null);
			if (propertyPathModel != null) {
				ManagedType<?> propertyPathManagedType = QueryUtils.getManagedTypeForModel(propertyPathModel);
				PropertyPath nextPath = path.next();
				if (nextPath != null && propertyPathManagedType != null) {
					Bindable<?> nextPropertyPathModel = getModelForPath(nextPath, propertyPathManagedType, () -> null);
					if (nextPropertyPathModel instanceof SingularAttribute<?, ?> singularAttribute) {
						return singularAttribute.isId();
					}
				}
			}
		}

		return false;
	}

	/**
	 * Gets the model for a path segment. Adapted from QueryUtils.getModelForPath.
	 */
	private Bindable<?> getModelForPath(PropertyPath path, ManagedType<?> managedType, Supplier<Object> fallback) {
		String segment = path.getSegment();
		if (managedType != null) {
			try {
				Attribute<?, ?> attribute = managedType.getAttribute(segment);
				return (Bindable<?>) attribute;
			} catch (IllegalArgumentException e) {
				// Attribute not found in managed type
			}
		}
		return null;
	}
}
