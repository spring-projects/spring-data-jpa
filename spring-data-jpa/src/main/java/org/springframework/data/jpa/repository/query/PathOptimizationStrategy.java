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

import jakarta.persistence.metamodel.Bindable;

import org.springframework.data.mapping.PropertyPath;

/**
 * Strategy interface for optimizing property path traversal in JPA queries.
 * Implementations determine when foreign key columns can be accessed directly
 * without creating unnecessary JOINs.
 *
 * @author Hyunjoon Kim
 * @since 3.5
 */
public interface PathOptimizationStrategy {

	/**
	 * Determines if a property path can be optimized by accessing the foreign key
	 * column directly instead of creating a JOIN.
	 *
	 * @param path the property path to check
	 * @param bindable the JPA bindable containing the property
	 * @param context metadata context for type information
	 * @return true if the path can be optimized
	 */
	boolean canOptimizeForeignKeyAccess(PropertyPath path, Bindable<?> bindable, MetamodelContext context);

	/**
	 * Checks if the given property path represents an association's identifier.
	 * For example, in "author.id", this would return true when "id" is the
	 * identifier of the Author entity.
	 *
	 * @param path the property path to check
	 * @param context metadata context for type information
	 * @return true if the path ends with an association's identifier
	 */
	boolean isAssociationId(PropertyPath path, MetamodelContext context);

	/**
	 * Context interface providing minimal metamodel information needed for
	 * optimization decisions. This abstraction allows the strategy to work
	 * in both runtime and AOT compilation scenarios.
	 */
	interface MetamodelContext {

		/**
		 * Checks if a type is a managed entity type.
		 *
		 * @param type the class to check
		 * @return true if the type is a managed entity
		 */
		boolean isEntityType(Class<?> type);

		/**
		 * Checks if a property is an identifier property of an entity.
		 *
		 * @param entityType the entity class
		 * @param propertyName the property name
		 * @return true if the property is an identifier
		 */
		boolean isIdProperty(Class<?> entityType, String propertyName);

		/**
		 * Gets the type of a property.
		 *
		 * @param entityType the entity class
		 * @param propertyName the property name
		 * @return the property type, or null if not found
		 */
		Class<?> getPropertyType(Class<?> entityType, String propertyName);
	}
}
