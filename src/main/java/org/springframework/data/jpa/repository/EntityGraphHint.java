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

import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;

/**
 * Contains information about and the {@link EntityGraphType} and name of the {@link javax.persistence.EntityGraph} to
 * use.
 * 
 * @author Thomas Darimont
 * @since 1.8
 */
public class EntityGraphHint {

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