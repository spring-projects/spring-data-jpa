/*
 * Copyright 2013-2025 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * Metamodel-based implementation for {@link JpaEntityMetadata}.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class JpaMetamodelEntityMetadata<T> implements JpaEntityMetadata<T> {

	private final EntityType<T> entityType;

	/**
	 * Creates a new {@link JpaMetamodelEntityMetadata} for the given domain type.
	 *
	 * @param entityType must not be {@literal null}.
	 */
	public JpaMetamodelEntityMetadata(EntityType<T> entityType) {

		Assert.notNull(entityType, "Entity type must not be null");
		this.entityType = entityType;
	}

	@Override
	public Class<T> getJavaType() {
		return entityType.getJavaType();
	}

	@Override
	public String getEntityName() {
		return entityType.getName();
	}

}
