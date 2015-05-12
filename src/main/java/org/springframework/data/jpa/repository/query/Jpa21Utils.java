/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Subgraph;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utils for bridging various JPA 2.1 features.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @since 1.6
 */
public class Jpa21Utils {

	private static final Method GET_ENTITY_GRAPH_METHOD;
	private static final boolean JPA21_AVAILABLE = ClassUtils.isPresent("javax.persistence.NamedEntityGraph",
			Jpa21Utils.class.getClassLoader());

	static {

		if (JPA21_AVAILABLE) {
			GET_ENTITY_GRAPH_METHOD = ReflectionUtils.findMethod(EntityManager.class, "getEntityGraph", String.class);
		} else {
			GET_ENTITY_GRAPH_METHOD = null;
		}
	}

	private Jpa21Utils() {
		// prevent instantiation
	}

	/**
	 * Returns a {@link Map} with hints for a JPA 2.1 fetch-graph or load-graph if running under JPA 2.1.
	 * 
	 * @param em must not be {@literal null}.
	 * @param entityGraph can be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return a {@code Map} with the hints or an empty {@code Map} if no hints were found.
	 * @since 1.8
	 */
	public static Map<String, Object> tryGetFetchGraphHints(EntityManager em, JpaEntityGraph entityGraph,
			Class<?> entityType) {

		if (entityGraph == null) {
			return Collections.emptyMap();
		}

		EntityGraph<?> graph = tryGetFetchGraph(em, entityGraph, entityType);

		if (graph == null) {
			return Collections.emptyMap();
		}

		return Collections.<String, Object> singletonMap(entityGraph.getType().getKey(), graph);
	}

	/**
	 * Adds a JPA 2.1 fetch-graph or load-graph hint to the given {@link Query} if running under JPA 2.1.
	 * 
	 * @see JPA 2.1 Specfication 3.7.4 - Use of Entity Graphs in find and query operations P.117
	 * @param em must not be {@literal null}.
	 * @param jpaEntityGraph must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return the {@link EntityGraph} described by the given {@code entityGraph}.
	 */
	private static EntityGraph<?> tryGetFetchGraph(EntityManager em, JpaEntityGraph jpaEntityGraph, Class<?> entityType) {

		Assert.notNull(em, "EntityManager must not be null!");
		Assert.notNull(jpaEntityGraph, "EntityGraph must not be null!");
		Assert.notNull(entityType, "EntityType must not be null!");

		Assert.isTrue(JPA21_AVAILABLE, "The EntityGraph-Feature requires at least a JPA 2.1 persistence provider!");
		Assert.isTrue(GET_ENTITY_GRAPH_METHOD != null,
				"It seems that you have the JPA 2.1 API but a JPA 2.0 implementation on the classpath!");

		try {
			// first check whether an entityGraph with that name is already registered.
			return em.getEntityGraph(jpaEntityGraph.getName());
		} catch (Exception ex) {
			// try to create and dynamically register the entityGraph
			return createDynamicEntityGraph(em, jpaEntityGraph, entityType);
		}
	}

	/**
	 * Creates a dynamic {@link EntityGraph} from the given {@link JpaEntityGraph} information.
	 * 
	 * @param em must not be {@literal null}.
	 * @param jpaEntityGraph must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return
	 * @since 1.9
	 */
	private static EntityGraph<?> createDynamicEntityGraph(EntityManager em, JpaEntityGraph jpaEntityGraph,
			Class<?> entityType) {

		Assert.notNull(em, "EntityManager must not be null!");
		Assert.notNull(jpaEntityGraph, "JpaEntityGraph must not be null!");
		Assert.notNull(entityType, "Entity type must not be null!");
		Assert.isTrue(jpaEntityGraph.isAdHocEntityGraph(), "The given " + jpaEntityGraph + " is not dynamic!");

		EntityGraph<?> entityGraph = em.createEntityGraph(entityType);
		configureFetchGraphFrom(jpaEntityGraph, entityGraph);

		return entityGraph;
	}

	/**
	 * Configures the given {@link EntityGraph} with the fetch graph information stored in {@link JpaEntityGraph}.
	 * 
	 * @param jpaEntityGraph
	 * @param entityGraph
	 */
	static void configureFetchGraphFrom(JpaEntityGraph jpaEntityGraph, EntityGraph<?> entityGraph) {

		List<String> attributePaths = new ArrayList<String>(jpaEntityGraph.getAttributePaths());

		// Sort to ensure that the intermediate entity subgraphs are created accordingly.
		Collections.sort(attributePaths);
		Collections.reverse(attributePaths);

		// We build the entity graph based on the paths with highest depth first
		for (String path : attributePaths) {

			// Fast path - just single attribute
			if (!path.contains(".")) {
				entityGraph.addAttributeNodes(path);
				continue;
			}

			// We need to build nested sub fetch graphs
			String[] pathComponents = StringUtils.delimitedListToStringArray(path, ".");
			Subgraph<?> parent = null;

			for (int c = 0; c < pathComponents.length - 1; c++) {
				parent = c == 0 ? entityGraph.addSubgraph(pathComponents[c]) : parent.addSubgraph(pathComponents[c]);
			}

			parent.addAttributeNodes(pathComponents[pathComponents.length - 1]);
		}
	}
}
