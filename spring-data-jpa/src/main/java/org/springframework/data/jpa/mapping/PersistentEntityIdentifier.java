/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.jpa.mapping;

import jakarta.persistence.metamodel.SingularAttribute;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

/**
 * JPA identifier metadata.
 *
 * @author Mark Paluch
 */
public interface PersistentEntityIdentifier<E> {

	/**
	 * Return the identifier type, including the ID class for entities with separate identifier properties.
	 * <p>
	 * An identifier type can be either a simple type (in case of a single attribute identifier), or the type that owns
	 * composite identifier columns (the entity type itself or an {@link jakarta.persistence.IdClass}).
	 *
	 * @since 4.1
	 */
	Class<?> getType();

	/**
	 * Return an identifier attribute, or {@literal null} for a type without an identifier.
	 *
	 * @since 4.1
	 */
	@Nullable
	SingularAttribute<? super E, ?> getAttribute();

	/**
	 * Return identifier property paths, expanding associations into their identifier paths.
	 *
	 * @since 4.1
	 */
	Collection<String> getPropertyPaths();

	/**
	 * Return whether the identifier is represented by separate properties. An embedded identifier is one property.
	 *
	 * @since 4.1
	 */
	boolean isComposite();

}
