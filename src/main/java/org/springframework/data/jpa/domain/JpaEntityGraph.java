/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.jpa.domain;

import javax.persistence.EntityGraph;

import org.springframework.util.Assert;

/**
 * EntityGraph configuration for JPA 2.1 {@link EntityGraph}s.
 * 
 * @author Thomas Darimont
 * @since 1.6
 */
public class JpaEntityGraph {

	private final String name;
	private final FetchGraphType type;

	/**
	 * Creates an {@link JpaEntityGraph}.
	 * 
	 * @param name must not be {@null}.
	 * @param type must not be {@null}.
	 */
	public JpaEntityGraph(String name, FetchGraphType type) {

		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(type, "FetchGraphType must not be null!");
		Assert.hasText(name, "The name of an EntityGraph must not be empty!");

		this.name = name;
		this.type = type;
	}

	/**
	 * Returns the name of the {@link EntityGraph} configuration to use.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the {@link FetchGraphType} of the {@link EntityGraph} to use.
	 * 
	 * @return
	 */
	public FetchGraphType getType() {
		return type;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "JpaEntityGraph [name=" + name + ", type=" + type + "]";
	}

	/**
	 * Enum for JPA 2.1 {@link javax.persistence.EntityGraph} types.
	 * 
	 * @author Thomas Darimont
	 * @since 1.6
	 */
	public enum FetchGraphType {

		/**
		 * Fetch Graph Semantics
		 * <p>
		 * When the javax.persistence.loadgraph property is used to specify an entity graph, attributes that are specified
		 * by attribute nodes of the entity graph are treated as FetchType.EAGER and attributes that are not specified are
		 * treated according to their specified or default FetchType.
		 * <p>
		 * 
		 * @see JPA 2.1 Specification: 3.7.4.1 Fetch Graph Semantics
		 */
		LOAD("javax.persistence.loadgraph"),

		/**
		 * Fetch Graph Semantics:
		 * <p>
		 * When the javax.persistence.fetchgraph property is used to specify an entity graph, attributes that are specified
		 * by attribute nodes of the entity graph are treated as FetchType.EAGER and attributes that are not specified are
		 * treated as FetchType.LAZY
		 * <p>
		 * 
		 * @see JPA 2.1 Specification: 3.7.4.2 Load Graph Semantics
		 */
		FETCH("javax.persistence.fetchgraph");

		private final String key;

		private FetchGraphType(String value) {
			this.key = value;
		}

		public String getKey() {
			return key;
		}
	}
}
