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

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * A wrapper for a String representation of a query offering information about the query.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @since 2.0.3
 */
interface EntityQuery extends IntrospectedQuery {

	/**
	 * Creates a DeclaredQuery for a JPQL query.
	 *
	 * @param query the JPQL query string.
	 * @return
	 */
	static EntityQuery introspectJpql(String query, QueryEnhancerFactory queryEnhancer) {
		return ObjectUtils.isEmpty(query) ? EmptyIntrospectedQuery.EMPTY_QUERY
				: new StringQuery(query, false, queryEnhancer, parameterBindings -> {});
	}

	/**
	 * Creates a DeclaredQuery for a JPQL query.
	 *
	 * @param query the JPQL query string.
	 * @return
	 */
	static EntityQuery introspectJpql(String query, QueryEnhancerSelector selector) {
		return ObjectUtils.isEmpty(query) ? EmptyIntrospectedQuery.EMPTY_QUERY
				: new StringQuery(query, false, selector, parameterBindings -> {});
	}

	/**
	 * Creates a DeclaredQuery for a native query.
	 *
	 * @param query the native query string.
	 * @return
	 */
	static EntityQuery introspectNativeQuery(String query, QueryEnhancerFactory queryEnhancer) {
		return ObjectUtils.isEmpty(query) ? EmptyIntrospectedQuery.EMPTY_QUERY
				: new StringQuery(query, true, queryEnhancer, parameterBindings -> {});
	}

	/**
	 * Creates a DeclaredQuery for a native query.
	 *
	 * @param query the native query string.
	 * @return
	 */
	static EntityQuery introspectNativeQuery(String query, QueryEnhancerSelector selector) {
		return ObjectUtils.isEmpty(query) ? EmptyIntrospectedQuery.EMPTY_QUERY
				: new StringQuery(query, true, selector, parameterBindings -> {});
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
	 * Creates a new {@literal IntrospectedQuery} representing a count query, i.e. a query returning the number of rows to
	 * be expected from the original query, either derived from the query wrapped by this instance or from the information
	 * passed as arguments.
	 *
	 * @param countQueryProjection an optional return type for the query.
	 * @return a new {@literal IntrospectedQuery} instance.
	 */
	IntrospectedQuery deriveCountQuery(@Nullable String countQueryProjection);

	String applySorting(Sort sort);

	/**
	 * @return whether paging is implemented in the query itself, e.g. using SpEL expressions.
	 * @since 2.0.6
	 */
	default boolean usesPaging() {
		return false;
	}

}
