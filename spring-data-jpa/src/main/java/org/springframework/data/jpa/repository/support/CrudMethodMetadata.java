/*
 * Copyright 2011-2023 the original author or authors.
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

import jakarta.persistence.LockModeType;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.lang.Nullable;

/**
 * Interface to abstract {@link CrudMethodMetadata} that provide the {@link LockModeType} to be used for query
 * execution.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Greg Turnquist
 */
public interface CrudMethodMetadata {

	/**
	 * Returns the {@link LockModeType} to be used.
	 *
	 * @return
	 */
	@Nullable
	LockModeType getLockModeType();

	/**
	 * Returns all query hints in a list to be applied to queries executed for the CRUD method.
	 *
	 * @return
	 * @since 2.4
	 */
	QueryHints getQueryHints();

	/**
	 * Returns all query hints in a list to be applied to queries executed for the CRUD method.
	 *
	 * @return
	 * @since 2.4
	 */
	QueryHints getQueryHintsForCount();

	/**
	 * Returns query comment to be applied to query.
	 *
	 * @return
	 * @since 3.0
	 */
	String getComment();

	/**
	 * Returns the {@link EntityGraph} to be used.
	 *
	 * @return
	 * @since 1.9
	 */
	Optional<EntityGraph> getEntityGraph();

	/**
	 * Returns the {@link Method} to be used.
	 *
	 * @return
	 * @since 1.9
	 */
	Method getMethod();
}
