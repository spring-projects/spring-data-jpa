/*
 * Copyright 2023 the original author or authors.
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

import jakarta.persistence.EntityManager;

import java.util.Collection;

import org.springframework.data.jpa.repository.query.AbstractQueryEngine.*;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Defines the engine to execute a {@link QueryContext}, be it collection-based, page-based, single entity-based, or any
 * other style.
 * 
 * @author Greg Turnquist
 */
public interface QueryEngine extends RepositoryQuery {

	/**
	 * Execute the query with no arguments.
	 */
	Object execute();

	/**
	 * Execute a {@link QueryContext}'s count-based query with no arguments.
	 */
	long executeCount();

	/**
	 * Create a {@link QueryEngine} based upon a {@link JpaQueryMethod}'s return type and potentially the
	 * {@link QueryContext}.
	 *
	 * @param method
	 * @param entityManager
	 * @param queryContext
	 * @return
	 */
	static QueryEngine engineFor(JpaQueryMethod method, EntityManager entityManager, QueryContext queryContext) {

		if (queryContext instanceof CustomFinderQueryContext customFinderQueryContext) {

			if (method.isScrollQuery()) {
				return new ScrollQueryEngine(entityManager, customFinderQueryContext,
						new ScrollDelegate<>(customFinderQueryContext.getEntityInformation()));
			} else if (customFinderQueryContext.getTree().isDelete()) {
				return new DeleteQueryEngine(entityManager, customFinderQueryContext);
			} else if (customFinderQueryContext.getTree().isExistsProjection()) {
				return new ExistsQueryEngine(entityManager, customFinderQueryContext);
			}
		}

		if (method.isStreamQuery()) {
			return new StreamQueryEngine(entityManager, queryContext);
		} else if (method.isProcedureQuery()) {
			return new ProcedureQueryEngine(entityManager, queryContext);
		} else if (method.isCollectionQuery()) {
			return new CollectionQueryEngine(entityManager, queryContext);
		} else if (method.isSliceQuery()) {
			return new SlicedQueryEngine(entityManager, queryContext);
		} else if (method.isPageQuery()) {
			return new PagedQueryEngine(entityManager, queryContext);
		} else if (method.isModifyingQuery()) {
			return new ModifyingQueryEngine(entityManager, queryContext);
		}

		return new SingleEntityQueryEngine(entityManager, queryContext);
	}

	/**
	 * Create an {@link QueryEngine} for single-valued results.
	 *
	 * @param entityManager
	 * @param queryContext
	 * @return
	 */
	static QueryEngine singleEntityEngine(EntityManager entityManager, QueryContext queryContext) {
		return new SingleEntityQueryEngine(entityManager, queryContext);
	}

	/**
	 * Create an {@link QueryEngine} for {@link Collection}-based results.
	 *
	 * @param entityManager
	 * @param queryContext
	 * @return
	 */
	static QueryEngine collectionEngine(EntityManager entityManager, QueryContext queryContext) {
		return new CollectionQueryEngine(entityManager, queryContext);
	}
}
