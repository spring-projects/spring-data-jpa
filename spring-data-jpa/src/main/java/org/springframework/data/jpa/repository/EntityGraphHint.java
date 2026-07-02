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
package org.springframework.data.jpa.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.util.Assert;

/**
 * Query method argument to configure the JPA {@link jakarta.persistence.EntityGraph} used for a single repository
 * invocation.
 *
 * @author YeongJae Min
 * @since 4.2
 */
public final class EntityGraphHint<T> {

	private final EntityGraphType type;
	private final @Nullable String name;
	private final jakarta.persistence.@Nullable EntityGraph<?> entityGraph;
	private final List<String> attributePaths;

	private EntityGraphHint(EntityGraphType type, @Nullable String name,
			jakarta.persistence.@Nullable EntityGraph<?> entityGraph, List<String> attributePaths) {

		Assert.notNull(type, "EntityGraphType must not be null");
		Assert.notNull(attributePaths, "Attribute paths must not be null");

		this.type = type;
		this.name = name;
		this.entityGraph = entityGraph;
		this.attributePaths = List.copyOf(attributePaths);
	}

	/**
	 * Create a fetch graph hint from type-safe property paths.
	 *
	 * @param propertyPaths must not be {@literal null} or empty.
	 * @return a new {@link EntityGraphHint}.
	 */
	@SafeVarargs
	public static <T> EntityGraphHint<T> fetch(TypedPropertyPath<T, ?>... propertyPaths) {
		return from(EntityGraphType.FETCH, propertyPaths);
	}

	/**
	 * Create a load graph hint from type-safe property paths.
	 *
	 * @param propertyPaths must not be {@literal null} or empty.
	 * @return a new {@link EntityGraphHint}.
	 */
	@SafeVarargs
	public static <T> EntityGraphHint<T> load(TypedPropertyPath<T, ?>... propertyPaths) {
		return from(EntityGraphType.LOAD, propertyPaths);
	}

	/**
	 * Create a fetch graph hint from a named entity graph.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @return a new {@link EntityGraphHint}.
	 */
	public static <T> EntityGraphHint<T> fetch(String name) {
		return named(EntityGraphType.FETCH, name);
	}

	/**
	 * Create a load graph hint from a named entity graph.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @return a new {@link EntityGraphHint}.
	 */
	public static <T> EntityGraphHint<T> load(String name) {
		return named(EntityGraphType.LOAD, name);
	}

	/**
	 * Create a fetch graph hint from a JPA {@link jakarta.persistence.EntityGraph}.
	 *
	 * @param entityGraph must not be {@literal null}.
	 * @return a new {@link EntityGraphHint}.
	 */
	public static <T> EntityGraphHint<T> fetch(jakarta.persistence.EntityGraph<T> entityGraph) {
		return from(EntityGraphType.FETCH, entityGraph);
	}

	/**
	 * Create a load graph hint from a JPA {@link jakarta.persistence.EntityGraph}.
	 *
	 * @param entityGraph must not be {@literal null}.
	 * @return a new {@link EntityGraphHint}.
	 */
	public static <T> EntityGraphHint<T> load(jakarta.persistence.EntityGraph<T> entityGraph) {
		return from(EntityGraphType.LOAD, entityGraph);
	}

	private static <T> EntityGraphHint<T> named(EntityGraphType type, String name) {

		Assert.hasText(name, "Entity graph name must not be null or empty");

		return new EntityGraphHint<>(type, name, null, List.of());
	}

	private static <T> EntityGraphHint<T> from(EntityGraphType type, jakarta.persistence.EntityGraph<T> entityGraph) {

		Assert.notNull(entityGraph, "EntityGraph must not be null");

		return new EntityGraphHint<>(type, null, entityGraph, List.of());
	}

	private static <T> EntityGraphHint<T> from(EntityGraphType type, TypedPropertyPath<T, ?>[] propertyPaths) {

		Assert.notEmpty(propertyPaths, "Property paths must not be null or empty");
		Assert.noNullElements(propertyPaths, "Property paths must not contain null values");

		List<String> attributePaths = Arrays.stream(propertyPaths) //
				.map(it -> TypedPropertyPath.of(it).toDotPath()) //
				.toList();

		return new EntityGraphHint<>(type, null, null, attributePaths);
	}

	public EntityGraphType getType() {
		return type;
	}

	public @Nullable String getName() {
		return name;
	}

	public jakarta.persistence.@Nullable EntityGraph<?> getEntityGraph() {
		return entityGraph;
	}

	public List<String> getAttributePaths() {
		return attributePaths;
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof EntityGraphHint<?> that)) {
			return false;
		}

		return type == that.type && Objects.equals(name, that.name) && Objects.equals(entityGraph, that.entityGraph)
				&& attributePaths.equals(that.attributePaths);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, name, entityGraph, attributePaths);
	}

	@Override
	public String toString() {
		return "EntityGraphHint [type=" + type + ", name=" + name + ", entityGraph=" + entityGraph + ", attributePaths="
				+ attributePaths + "]";
	}
}
