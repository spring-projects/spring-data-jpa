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
package org.springframework.data.jpa.repository.support;

import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.projection.ProjectionFactory;

/**
 * Interface to be implemented by classes that want to be aware of their configuration in a JPA repository context.
 *
 * @author Mark Paluch
 * @since 3.3
 */
public interface JpaRepositoryConfigurationAware {

	/**
	 * Configures the {@link EscapeCharacter} to be used with the repository.
	 *
	 * @param escapeCharacter must not be {@literal null}.
	 */
	default void setEscapeCharacter(EscapeCharacter escapeCharacter) {

	}

	/**
	 * Configures the {@link ProjectionFactory} to be used with the repository.
	 *
	 * @param projectionFactory must not be {@literal null}.
	 */
	default void setProjectionFactory(ProjectionFactory projectionFactory) {

	}

	/**
	 * Configures the {@link CrudMethodMetadata} to be used with the repository.
	 *
	 * @param metadata must not be {@literal null}.
	 */
	default void setRepositoryMethodMetadata(CrudMethodMetadata metadata) {

	}
}
