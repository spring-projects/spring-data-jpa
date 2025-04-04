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
package org.springframework.data.jpa.repository.aot;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.util.StringUtils;

/**
 * Factory for {@link AotEntityGraph}.
 *
 * @author Mark Paluch
 * @since 4.0
 */
class EntityGraphLookup {

	private final EntityManagerFactory entityManagerFactory;

	public EntityGraphLookup(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@SuppressWarnings("unchecked")
	public @Nullable AotEntityGraph findEntityGraph(MergedAnnotation<EntityGraph> entityGraph,
			RepositoryInformation information, ReturnedType returnedType, JpaQueryMethod queryMethod) {

		if (!entityGraph.isPresent()) {
			return null;
		}

		EntityGraph.EntityGraphType type = entityGraph.getEnum("type", EntityGraph.EntityGraphType.class);
		String[] attributePaths = entityGraph.getStringArray("attributePaths");
		Collection<String> entityGraphNames = getEntityGraphNames(entityGraph, information, queryMethod);
		List<Class<?>> candidates = Arrays.asList(returnedType.getDomainType(), returnedType.getReturnedType(),
				returnedType.getTypeToRead());

		for (Class<?> candidate : candidates) {

			Map<String, jakarta.persistence.EntityGraph<?>> namedEntityGraphs = entityManagerFactory
					.getNamedEntityGraphs(Class.class.cast(candidate));

			if (namedEntityGraphs.isEmpty()) {
				continue;
			}

			for (String entityGraphName : entityGraphNames) {
				if (namedEntityGraphs.containsKey(entityGraphName)) {
					return new AotEntityGraph(entityGraphName, type, Collections.emptyList());
				}
			}
		}

		if (attributePaths.length > 0) {
			return new AotEntityGraph(null, type, Arrays.asList(attributePaths));
		}

		return null;
	}

	private Set<String> getEntityGraphNames(MergedAnnotation<EntityGraph> entityGraph, RepositoryInformation information,
			JpaQueryMethod queryMethod) {

		Set<String> entityGraphNames = new LinkedHashSet<>();
		String value = entityGraph.getString("value");

		if (StringUtils.hasText(value)) {
			entityGraphNames.add(value);
		}
		entityGraphNames.add(queryMethod.getNamedQueryName());
		entityGraphNames.add(getFallbackEntityGraphName(information, queryMethod));
		return entityGraphNames;
	}

	private String getFallbackEntityGraphName(RepositoryInformation information, JpaQueryMethod queryMethod) {

		Class<?> domainType = information.getDomainType();
		Entity entity = AnnotatedElementUtils.findMergedAnnotation(domainType, Entity.class);
		String entityName = entity != null && StringUtils.hasText(entity.name()) ? entity.name()
				: domainType.getSimpleName();

		return entityName + "." + queryMethod.getName();
	}

}
