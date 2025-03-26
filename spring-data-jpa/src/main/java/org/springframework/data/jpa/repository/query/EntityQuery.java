/*
 * Copyright 2018-2024 the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * An extension to {@link ParametrizedQuery} exposing query information about its inner structure such as whether
 * constructor expressions (JPQL) are used or the default projection is used.
 * <p>
 * Entity Queries support derivation of {@link #deriveCountQuery(String) count queries} from the original query. They
 * also can be used to rewrite the query using sorting and projection selection.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @since 4.0
 */
public interface EntityQuery extends ParametrizedQuery {

	/**
	 * Create a new {@link EntityQuery} given {@link DeclaredQuery} and {@link QueryEnhancerSelector}.
	 *
	 * @param query must not be {@literal null}.
	 * @param selector must not be {@literal null}.
	 * @return a new {@link EntityQuery}.
	 */
	static EntityQuery create(DeclaredQuery query, QueryEnhancerSelector selector) {

		PreprocessedQuery preparsed = PreprocessedQuery.parse(query);
		QueryEnhancerFactory enhancerFactory = selector.select(preparsed);

		return new DefaultEntityQuery(preparsed, enhancerFactory);
	}

	/**
	 * Returns whether the query is using a constructor expression.
	 *
	 * @since 1.10
	 */
	boolean hasConstructorExpression();

	/**
	 * Returns whether the query uses the default projection, i.e. returns the main alias defined for the query.
	 */
	boolean isDefaultProjection();

	/**
	 * @return whether paging is implemented in the query itself, e.g. using SpEL expressions.
	 * @since 2.0.6
	 */
	default boolean usesPaging() {
		return false;
	}

	/**
	 * Creates a new {@literal IntrospectedQuery} representing a count query, i.e. a query returning the number of rows to
	 * be expected from the original query, either derived from the query wrapped by this instance or from the information
	 * passed as arguments.
	 *
	 * @param countQueryProjection an optional return type for the query.
	 * @return a new {@literal IntrospectedQuery} instance.
	 */
	ParametrizedQuery deriveCountQuery(@Nullable String countQueryProjection);

	/**
	 * Rewrite the query using the given
	 * {@link org.springframework.data.jpa.repository.query.QueryEnhancer.QueryRewriteInformation} into a sorted query or
	 * using a different projection. The rewritten query retains parameter binding characteristics.
	 *
	 * @param rewriteInformation query rewrite information (sorting, projection) to use.
	 * @return the rewritten query.
	 */
	QueryProvider rewrite(QueryEnhancer.QueryRewriteInformation rewriteInformation);

}
