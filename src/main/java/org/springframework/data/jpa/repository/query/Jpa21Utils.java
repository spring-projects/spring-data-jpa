/*
 * Copyright 2014-2015 the original author or authors.
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
import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

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
	 * @param em must not be {@literal null}
	 * @param query must not be {@literal null}
	 * @param entityGraph can be {@literal null}
	 * @return a {@code Map} with the hints or an empty {@code Map} if no hints were found
	 * @since 1.8
	 */
	public static Map<String, Object> tryGetFetchGraphHints(EntityManager em, JpaEntityGraph entityGraph) {

		if (entityGraph == null) {
			return Collections.emptyMap();
		}

		EntityGraph<?> graph = tryGetFetchGraph(em, entityGraph);

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
	 * @return the {@link EntityGraph} described by the given {@code entityGraph}.
	 */
	public static EntityGraph<?> tryGetFetchGraph(EntityManager em, JpaEntityGraph jpaEntityGraph) {

		Assert.notNull(em, "EntityManager must not be null!");
		Assert.notNull(jpaEntityGraph, "EntityGraph must not be null!");

		Assert.isTrue(JPA21_AVAILABLE, "The EntityGraph-Feature requires at least a JPA 2.1 persistence provider!");
		Assert.isTrue(GET_ENTITY_GRAPH_METHOD != null,
				"It seems that you have the JPA 2.1 API but a JPA 2.0 implementation on the classpath!");

		return em.getEntityGraph(jpaEntityGraph.getName());
	}
}
