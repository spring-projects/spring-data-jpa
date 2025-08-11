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

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import org.jspecify.annotations.Nullable;

/**
 * JPA Metamodel-based implementation of {@link PathOptimizationStrategy.MetamodelContext}.
 * This implementation uses the JPA metamodel at runtime but can be replaced with
 * an AOT-friendly implementation during native image compilation.
 *
 * @author Hyunjoon Kim
 * @since 3.5
 */
public class JpaMetamodelContext implements PathOptimizationStrategy.MetamodelContext {

	private final @Nullable Metamodel metamodel;

	public JpaMetamodelContext(@Nullable Metamodel metamodel) {
		this.metamodel = metamodel;
	}

	@Override
	public boolean isEntityType(Class<?> type) {
		if (metamodel == null) {
			return false;
		}

		try {
			metamodel.entity(type);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public boolean isIdProperty(Class<?> entityType, String propertyName) {
		if (metamodel == null) {
			return false;
		}

		try {
			ManagedType<?> managedType = metamodel.managedType(entityType);
			
			if (managedType instanceof EntityType<?> entity) {
				// Check for single ID attribute
				if (entity.hasSingleIdAttribute()) {
					SingularAttribute<?, ?> idAttribute = entity.getId(entity.getIdType().getJavaType());
					return idAttribute.getName().equals(propertyName);
				}
				
				// Check for composite ID
				return entity.getIdClassAttributes().stream()
					.anyMatch(attr -> attr.getName().equals(propertyName));
			}
			
		} catch (IllegalArgumentException e) {
			// Type not found in metamodel
		}
		
		return false;
	}

	@Override
	@Nullable
	public Class<?> getPropertyType(Class<?> entityType, String propertyName) {
		if (metamodel == null) {
			return null;
		}

		try {
			ManagedType<?> managedType = metamodel.managedType(entityType);
			return managedType.getAttribute(propertyName).getJavaType();
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}