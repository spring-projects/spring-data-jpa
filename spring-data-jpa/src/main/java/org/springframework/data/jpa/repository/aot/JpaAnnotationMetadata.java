/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.util.Assert;

/**
 * JPA mapping metadata collected from annotations without initializing an {@link jakarta.persistence.EntityManagerFactory}.
 * <p>
 * Provides access to {@link NamedQuery}, {@link NamedNativeQuery}, and {@link NamedEntityGraph} declarations on entity
 * types for AOT repository generation. This is a first step toward GH-4166: AOT still bootstraps an
 * {@code EntityManagerFactory} for metamodel access and query derivation. Annotation metadata supplements named query
 * and entity graph resolution ahead of {@code EntityManagerFactory} lookups.
 * <p>
 * Not in scope for this type: full domain model mapping metadata, {@code orm.xml} declarations, Hibernate-specific
 * annotations, or eliminating the {@code EntityManagerFactory} bootstrap entirely.
 *
 * @author LordKay-sudo
 * @since 4.2
 * @see <a href="https://github.com/spring-projects/spring-data-jpa/issues/4166">GH-4166</a>
 */
public final class JpaAnnotationMetadata {

	/**
	 * Named query definition collected from {@link NamedQuery} or {@link NamedNativeQuery}.
	 *
	 * @param name query name
	 * @param query query string
	 * @param nativeQuery whether the query is a native SQL query
	 */
	public record NamedQueryDefinition(String name, String query, boolean nativeQuery) {
	}

	private final Map<String, NamedQueryDefinition> namedQueries;

	private final Map<String, Set<String>> namedEntityGraphsByClassName;

	private JpaAnnotationMetadata(Map<String, NamedQueryDefinition> namedQueries,
			Map<String, Set<String>> namedEntityGraphsByClassName) {

		this.namedQueries = Map.copyOf(namedQueries);
		this.namedEntityGraphsByClassName = namedEntityGraphsByClassName.entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
	}

	/**
	 * Collect annotation metadata from the given entity types.
	 */
	public static JpaAnnotationMetadata from(Collection<Class<?>> entityTypes) {

		Assert.notNull(entityTypes, "Entity types must not be null");

		Map<String, NamedQueryDefinition> namedQueries = new HashMap<>();
		Map<String, Set<String>> namedEntityGraphs = new HashMap<>();

		for (Class<?> entityType : entityTypes) {
			collect(entityType, namedQueries, namedEntityGraphs);
		}

		return new JpaAnnotationMetadata(namedQueries, namedEntityGraphs);
	}

	/**
	 * Collect annotation metadata from Jakarta Persistence-annotated types in the given {@link AotRepositoryContext}.
	 */
	public static JpaAnnotationMetadata from(AotRepositoryContext repositoryContext) {

		Assert.notNull(repositoryContext, "AotRepositoryContext must not be null");

		return from(repositoryContext.getResolvedTypes().stream().filter(JpaAnnotationMetadata::isJakartaAnnotated).toList());
	}

	public static JpaAnnotationMetadata empty() {
		return new JpaAnnotationMetadata(Map.of(), Map.of());
	}

	public Optional<NamedQueryDefinition> findNamedQuery(String name) {
		return Optional.ofNullable(namedQueries.get(name));
	}

	public boolean hasNamedEntityGraph(Class<?> entityType, String name) {

		Set<String> graphNames = namedEntityGraphsByClassName.get(entityType.getName());
		return graphNames != null && graphNames.contains(name);
	}

	private static void collect(Class<?> entityType, Map<String, NamedQueryDefinition> namedQueries,
			Map<String, Set<String>> namedEntityGraphs) {

		MergedAnnotations metadata = MergedAnnotations.from(entityType);

		metadata.stream(NamedQuery.class).distinct().forEach(annotation -> {
			registerNamedQuery(namedQueries, annotation.getString("name"), annotation.getString("query"), false);
		});

		metadata.stream(NamedNativeQuery.class).distinct().forEach(annotation -> {
			registerNamedQuery(namedQueries, annotation.getString("name"), annotation.getString("query"), true);
		});

		Set<String> graphNames = new HashSet<>();
		metadata.stream(NamedEntityGraph.class).distinct().forEach(annotation -> {
			graphNames.add(annotation.getString("name"));
		});

		if (!graphNames.isEmpty()) {
			namedEntityGraphs.put(entityType.getName(), graphNames);
		}
	}

	private static void registerNamedQuery(Map<String, NamedQueryDefinition> namedQueries, String name, String query,
			boolean nativeQuery) {

		Assert.hasText(name, "Named query name must not be empty");
		Assert.hasText(query, () -> "Named query [%s] must declare a query string".formatted(name));
		namedQueries.put(name, new NamedQueryDefinition(name, query, nativeQuery));
	}

	private static boolean isJakartaAnnotated(Class<?> type) {

		return type.isAnnotationPresent(Entity.class) //
				|| type.isAnnotationPresent(Embeddable.class) //
				|| type.isAnnotationPresent(MappedSuperclass.class) //
				|| type.isAnnotationPresent(Converter.class);
	}

}
