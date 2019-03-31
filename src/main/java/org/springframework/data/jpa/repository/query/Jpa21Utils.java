/*
 * Copyright 2014-2019 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Subgraph;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utils for bridging various JPA 2.1 features.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.6
 */
public class Jpa21Utils {

	private static final @Nullable Method GET_ENTITY_GRAPH_METHOD;
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
	public static Map<String, Object> tryGetFetchGraphHints(EntityManager em, @Nullable JpaEntityGraph entityGraph,
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
	 * @see <a href="download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf">JPA 2.1
	 *      Specfication 3.7.4 - Use of Entity Graphs in find and query operations P.117</a>
	 * @param em must not be {@literal null}.
	 * @param jpaEntityGraph must not be {@literal null}.
	 * @param entityType must not be {@literal null}.
	 * @return the {@link EntityGraph} described by the given {@code entityGraph}.
	 */
	@Nullable
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

		for (String path : attributePaths) {

			String[] pathComponents = StringUtils.delimitedListToStringArray(path, ".");
			createGraph(pathComponents, 0, entityGraph, null);
		}
	}

	private static void createGraph(String[] pathComponents, int offset, EntityGraph<?> root,
			@Nullable Subgraph<?> parent) {

		String attributeName = pathComponents[offset];

		// we found our leaf property, now let's see if it already exists and add it if not
		if (pathComponents.length - 1 == offset) {

			if (parent == null && !exists(attributeName, root.getAttributeNodes())) {
				root.addAttributeNodes(attributeName);
			} else if (parent != null && !exists(attributeName, parent.getAttributeNodes())) {
				parent.addAttributeNodes(attributeName);
			}

			return;
		}

		AttributeNode<?> node = findAttributeNode(attributeName, root, parent);

		if (node != null) {

			Subgraph<?> subgraph = getSubgraph(node);

			if (subgraph == null) {
				subgraph = parent != null ? parent.addSubgraph(attributeName) : root.addSubgraph(attributeName);
			}

			createGraph(pathComponents, offset + 1, root, subgraph);

			return;
		}

		if (parent == null) {
			createGraph(pathComponents, offset + 1, root, root.addSubgraph(attributeName));
		} else {
			createGraph(pathComponents, offset + 1, root, parent.addSubgraph(attributeName));
		}
	}

	/**
	 * Checks the given {@link List} of {@link AttributeNode}s for the existence of an {@link AttributeNode} matching the
	 * given {@literal attributeNodeName}.
	 *
	 * @param attributeNodeName
	 * @param nodes
	 * @return
	 */
	private static boolean exists(String attributeNodeName, List<AttributeNode<?>> nodes) {
		return findAttributeNode(attributeNodeName, nodes) != null;
	}

	/**
	 * Find the {@link AttributeNode} matching the given {@literal attributeNodeName} in given {@link Subgraph} or
	 * {@link EntityGraph} favoring matches {@link Subgraph} over {@link EntityGraph}.
	 *
	 * @param attributeNodeName
	 * @param entityGraph
	 * @param parent
	 * @return {@literal null} if not found.
	 */
	@Nullable
	private static AttributeNode<?> findAttributeNode(String attributeNodeName, EntityGraph<?> entityGraph,
			@Nullable Subgraph<?> parent) {
		return findAttributeNode(attributeNodeName,
				parent != null ? parent.getAttributeNodes() : entityGraph.getAttributeNodes());
	}

	/**
	 * Find the {@link AttributeNode} matching the given {@literal attributeNodeName} in given {@link List} of
	 * {@link AttributeNode}s.
	 *
	 * @param attributeNodeName
	 * @param nodes
	 * @return {@literal null} if not found.
	 */
	@Nullable
	private static AttributeNode<?> findAttributeNode(String attributeNodeName, List<AttributeNode<?>> nodes) {

		for (AttributeNode<?> node : nodes) {
			if (ObjectUtils.nullSafeEquals(node.getAttributeName(), attributeNodeName)) {
				return node;
			}
		}

		return null;
	}

	/**
	 * Extracts the first {@link Subgraph} from the given {@link AttributeNode}. Ignores any potential different
	 * {@link Subgraph}s registered for more concrete {@link Class}es as the dynamically created graph does not
	 * distinguish between those.
	 *
	 * @param node
	 * @return
	 */
	@Nullable
	private static Subgraph<?> getSubgraph(AttributeNode<?> node) {
		return node.getSubgraphs().isEmpty() ? null : node.getSubgraphs().values().iterator().next();
	}
}
