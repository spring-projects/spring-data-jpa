/*
 * Copyright 2017-2019 the original author or authors.
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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * SPI interface to be implemented by {@link JpaRepository} implementations.
 *
 * @author Oliver Gierke
 * @author Stefan Fussenegger
 * @author Jens Schauder
 */
@NoRepositoryBean
public interface JpaRepositoryImplementation<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

	/**
	 * Configures the {@link CrudMethodMetadata} to be used with the repository.
	 *
	 * @param crudMethodMetadata must not be {@literal null}.
	 */
	void setRepositoryMethodMetadata(CrudMethodMetadata crudMethodMetadata);

	/**
	 * Configures the {@link EscapeCharacter} to be used with the repository.
	 *
	 * @param escapeCharacter Must not be {@literal null}.
	 */
	default void setEscapeCharacter(EscapeCharacter escapeCharacter) {

	}
}
