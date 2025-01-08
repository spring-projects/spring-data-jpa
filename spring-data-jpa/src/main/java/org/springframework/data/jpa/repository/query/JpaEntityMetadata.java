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

import org.springframework.data.repository.core.EntityMetadata;

/**
 * JPA specific extension of {@link EntityMetadata}.
 *
 * @author Oliver Gierke
 */
public interface JpaEntityMetadata<T> extends EntityMetadata<T> {

	/**
	 * Returns the name of the entity.
	 *
	 * @return
	 */
	String getEntityName();
}
