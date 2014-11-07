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
package org.springframework.data.jpa.repository.query;

import javax.persistence.EntityGraph;

import org.springframework.data.repository.EntityGraph.EntityGraphType;
import org.springframework.data.repository.query.EntityGraphable;
import org.springframework.util.Assert;

/**
 * EntityGraph configuration for JPA 2.1 {@link EntityGraph}s.
 * 
 * @author Thomas Darimont
 * @since 1.6
 */
public class JpaEntityGraph implements EntityGraphable {

	private final String name;
	private final EntityGraphType type;

	/**
	 * Creates an {@link JpaEntityGraph}.
	 * 
	 * @param name must not be {@null} or empty.
	 * @param type must not be {@null}.
	 */
	public JpaEntityGraph(String name, EntityGraphType type) {

		Assert.hasText(name, "The name of an EntityGraph must not be null or empty!");
		Assert.notNull(type, "FetchGraphType must not be null!");

		this.name = name;
		this.type = type;
	}

	/**
	 * Creates an {@link JpaEntityGraph}.
	 * 
	 * @param name must not be {@null} or empty.
	 */
	public JpaEntityGraph(String name) {
		this(name, EntityGraphType.FETCH);
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
	 * Returns the {@link EntityGraphType} of the {@link EntityGraph} to use.
	 * 
	 * @return
	 */
	public EntityGraphType getType() {
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
}
