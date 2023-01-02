/*
 * Copyright 2012-2023 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * A factory interface for creating {@link JpaQueryMethodFactory} instances. This may be implemented by extensions to
 * Spring Data JPA in order create instances of custom subclasses.
 *
 * @author Réda Housni Alaoui
 * @since 2.3
 */
public interface JpaQueryMethodFactory {

	/**
	 * Creates a {@link JpaQueryMethod}.
	 *
	 * @param method must not be {@literal null}
	 * @param metadata must not be {@literal null}
	 * @param factory must not be {@literal null}
	 */
	JpaQueryMethod build(Method method, RepositoryMetadata metadata, ProjectionFactory factory);

}
