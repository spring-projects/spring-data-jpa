/*
 * Copyright 2021-2023 the original author or authors.
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

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Subgraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.mapping.PropertyPath;

/**
 * Factory class to create an {@link EntityGraph} from a collection of property paths.
 *
 * @author Jens Schauder
 * @author Petr Strnad
 * @since 2.6
 */
abstract class EntityGraphFactory {

	public static final String HINT = "javax.persistence.fetchgraph";

	/**
	 * Create an {@link EntityGraph} from a collection of properties.
	 *
	 * @param domainType
	 * @param properties
	 */
	public static <T> EntityGraph<T> create(EntityManager entityManager, Class<T> domainType, Set<String> properties) {

		EntityGraph<T> entityGraph = entityManager.createEntityGraph(domainType);
		Map<String, Subgraph<Object>> existingSubgraphs = new HashMap<>();

		for (String property : properties) {

			Subgraph<Object> current = null;
			String currentFullPath = "";

			for (PropertyPath path : PropertyPath.from(property, domainType)) {

				currentFullPath += path.getSegment() + ".";

				if (path.hasNext()) {
					final Subgraph<Object> finalCurrent = current;
					current = current == null
							? existingSubgraphs.computeIfAbsent(currentFullPath, k -> entityGraph.addSubgraph(path.getSegment()))
							: existingSubgraphs.computeIfAbsent(currentFullPath, k -> finalCurrent.addSubgraph(path.getSegment()));
					continue;
				}

				if (current == null) {
					entityGraph.addAttributeNodes(path.getSegment());
				} else {
					current.addAttributeNodes(path.getSegment());

				}
			}
		}

		return entityGraph;
	}

}
