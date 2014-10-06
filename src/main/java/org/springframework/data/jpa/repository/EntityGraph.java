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
package org.springframework.data.jpa.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure the JPA 2.1 {@link javax.persistence.EntityGraph}s that should be used on repository methods.
 * 
 * @author Thomas Darimont
 * @since 1.6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface EntityGraph {

	/**
	 * The name of the EntityGraph to use.
	 * 
	 * @return
	 */
	String value();

	/**
	 * The {@link Type} of the EntityGraph to use, defaults to {@link Type#FETCH}.
	 * 
	 * @return
	 */
	EntityGraphType type() default EntityGraphType.FETCH;

	/**
	 * Enum for JPA 2.1 {@link javax.persistence.EntityGraph} types.
	 * 
	 * @author Thomas Darimont
	 * @since 1.6
	 */
	public enum EntityGraphType {

		/**
		 * When the javax.persistence.loadgraph property is used to specify an entity graph, attributes that are specified
		 * by attribute nodes of the entity graph are treated as FetchType.EAGER and attributes that are not specified are
		 * treated according to their specified or default FetchType.
		 * 
		 * @see JPA 2.1 Specification: 3.7.4.2 Load Graph Semantics
		 */
		LOAD("javax.persistence.loadgraph"),

		/**
		 * When the javax.persistence.fetchgraph property is used to specify an entity graph, attributes that are specified
		 * by attribute nodes of the entity graph are treated as FetchType.EAGER and attributes that are not specified are
		 * treated as FetchType.LAZY
		 * 
		 * @see JPA 2.1 Specification: 3.7.4.1 Fetch Graph Semantics
		 */
		FETCH("javax.persistence.fetchgraph");

		private final String key;

		private EntityGraphType(String value) {
			this.key = value;
		}

		public String getKey() {
			return key;
		}
	}

	/**
	 * Contains information about and the {@link EntityGraphType} and name of the {@link javax.persistence.EntityGraph} to
	 * use.
	 * 
	 * @author Thomas Darimont
	 * @since 1.8
	 */
	public static class EntityGraphHint {

		private final EntityGraphType graphType;
		private final String graphName;

		/**
		 * Creates a new {@link EntityGraphHint}.
		 * 
		 * @param graphType
		 * @param graphName
		 */
		public EntityGraphHint(EntityGraphType graphType, String graphName) {
			this.graphType = graphType;
			this.graphName = graphName;
		}

		public EntityGraphType getGraphType() {
			return graphType;
		}

		public String getGraphName() {
			return graphName;
		}
	}
}
