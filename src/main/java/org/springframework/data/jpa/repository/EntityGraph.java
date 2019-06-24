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
package org.springframework.data.jpa.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.jpa.repository.query.JpaQueryMethod;

/**
 * Annotation to configure the JPA 2.1 {@link javax.persistence.EntityGraph}s that should be used on repository methods.
 * Since 1.9 we support the definition of dynamic {@link EntityGraph}s by allowing to customize the fetch-graph via via
 * {@link #attributePaths()} ad-hoc fetch-graph configuration.
 *
 * @author Christoph Strobl If {@link #attributePaths()} are specified then we ignore the entity-graph name
 *         {@link #value()} and treat this {@link EntityGraph} as dynamic.
 * @author Thomas Darimont
 * @since 1.6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface EntityGraph {

	/**
	 * The name of the EntityGraph to use. If empty we fall-back to {@link JpaQueryMethod#getNamedQueryName()} as the
	 * value.
	 *
	 * @return
	 */
	String value() default "";

	/**
	 * The {@link EntityGraphType} of the EntityGraph to use, defaults to {@link EntityGraphType#FETCH}.
	 *
	 * @return
	 */
	EntityGraphType type() default EntityGraphType.FETCH;

	/**
	 * The paths of attributes of this {@link EntityGraph} to use, empty by default. You can refer to direct properties of
	 * the entity or nested properties via a {@code property.nestedProperty}.
	 *
	 * @return
	 * @since 1.9
	 */
	String[] attributePaths() default {};

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
		 * @see <a href="https://download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf">JPA 2.1
		 *      Specification: 3.7.4.2 Load Graph Semantics</a>
		 */
		LOAD("javax.persistence.loadgraph"),

		/**
		 * When the javax.persistence.fetchgraph property is used to specify an entity graph, attributes that are specified
		 * by attribute nodes of the entity graph are treated as FetchType.EAGER and attributes that are not specified are
		 * treated as FetchType.LAZY
		 *
		 * @see <a href="https://download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf">JPA 2.1
		 *      Specification: 3.7.4.1 Fetch Graph Semantics</a>
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
}
