/*
 * Copyright 2021 the original author or authors.
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

import java.util.Set;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;

import org.springframework.data.mapping.PropertyPath;

/**
 * Turns a collection of property paths to an {@link EntityGraph} and applies it to a query abstraction
 *
 * @param <Q> the type of the query abstraction.
 * @author Jens Schauder
 * @since 2.6
 */
abstract class Projector<Q> {

	private final EntityManager entityManager;

	protected Projector(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void apply(Class<?> domainType, Q query, Set<String> properties) {

		if (!properties.isEmpty()) {

			final javax.persistence.EntityGraph<?> entityGraph = entityManager.createEntityGraph(domainType);

			for (String property : properties) {

				Subgraph<Object> subgraph = null;

				for (PropertyPath path : PropertyPath.from(property, domainType)) {

					if (path.hasNext()) {
						subgraph = subgraph == null ? entityGraph.addSubgraph(path.getSegment())
								: subgraph.addSubgraph(path.getSegment());
					} else {

						if (subgraph == null) {
							entityGraph.addAttributeNodes(path.getSegment());
						} else {
							subgraph.addAttributeNodes(path.getSegment());
						}
					}
				}
			}

			applyEntityGraph(query, entityGraph);
		}
	}

	abstract void applyEntityGraph(Q query, EntityGraph<?> entityGraph);
}
