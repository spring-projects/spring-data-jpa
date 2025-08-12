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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.PropertyPath;

/**
 * Basic tests for {@link PathOptimizationStrategy} and {@link JpaMetamodelContext}.
 * 
 * @author Hyunjoon Kim
 * @since 3.5
 */
class PathOptimizationStrategyBasicTests {

	@Test // GH-3349
	void defaultStrategyHandlesNullContext() {
		// Given
		DefaultPathOptimizationStrategy strategy = new DefaultPathOptimizationStrategy();
		JpaMetamodelContext nullContext = new JpaMetamodelContext(null);
		PropertyPath simplePath = PropertyPath.from("name", TestEntity.class);
		
		// When/Then - should not throw exceptions
		boolean canOptimize = strategy.canOptimizeForeignKeyAccess(simplePath, null, nullContext);
		boolean isAssociationId = strategy.isAssociationId(simplePath, nullContext);
		
		assertThat(canOptimize).isFalse();
		assertThat(isAssociationId).isFalse();
	}

	@Test // GH-3349
	void nullMetamodelContextHandlesGracefully() {
		// Given
		JpaMetamodelContext context = new JpaMetamodelContext(null);
		
		// When/Then - should handle null metamodel gracefully
		assertThat(context.isEntityType(TestEntity.class)).isFalse();
		assertThat(context.isIdProperty(TestEntity.class, "id")).isFalse();
		assertThat(context.getPropertyType(TestEntity.class, "name")).isNull();
	}

	@Test // GH-3349
	void strategyRejectsSimpleProperties() {
		// Given - simple property without association traversal
		DefaultPathOptimizationStrategy strategy = new DefaultPathOptimizationStrategy();
		PropertyPath simplePath = PropertyPath.from("name", TestEntity.class);
		
		// When
		boolean canOptimize = strategy.canOptimizeForeignKeyAccess(simplePath, null, 
			new JpaMetamodelContext(null));
		
		// Then - should not optimize simple properties
		assertThat(canOptimize).isFalse();
	}

	@Test // GH-3349
	void abstractionProvidesTotalCoverage() {
		// This test verifies that our abstraction provides the interface
		// needed by both QueryUtils and JpqlUtils
		
		PathOptimizationStrategy strategy = new DefaultPathOptimizationStrategy();
		PathOptimizationStrategy.MetamodelContext context = new JpaMetamodelContext(null);
		
		// Should implement all required methods without throwing
		assertThat(strategy).isNotNull();
		assertThat(context).isNotNull();
		assertThat(context.isEntityType(Object.class)).isFalse();
		assertThat(context.isIdProperty(Object.class, "id")).isFalse();
		assertThat(context.getPropertyType(Object.class, "id")).isNull();
	}

	// Simple test entity
	static class TestEntity {
		private Long id;
		private String name;
		
		public Long getId() { return id; }
		public String getName() { return name; }
	}
}
