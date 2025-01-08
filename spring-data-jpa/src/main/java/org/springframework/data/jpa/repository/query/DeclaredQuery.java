/*
 * Copyright 2018-2025 the original author or authors.
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

import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * A wrapper for a String representation of a query offering information about the query.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @since 2.0.3
 */
interface DeclaredQuery {

	/**
	 * Creates a {@literal DeclaredQuery} from a query {@literal String}.
	 *
	 * @param query might be {@literal null} or empty.
	 * @param nativeQuery is a given query is native or not
	 * @return a {@literal DeclaredQuery} instance even for a {@literal null} or empty argument.
	 */
	static DeclaredQuery of(@Nullable String query, boolean nativeQuery) {
		return ObjectUtils.isEmpty(query) ? EmptyDeclaredQuery.EMPTY_QUERY : new StringQuery(query, nativeQuery);
	}

	/**
	 * @return whether the underlying query has at least one named parameter.
	 */
	boolean hasNamedParameter();

	/**
	 * Returns the query string.
	 */
	String getQueryString();

	/**
	 * Returns the main alias used in the query.
	 *
	 * @return the alias
	 */
	@Nullable
	String getAlias();

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
	 * Returns the {@link ParameterBinding}s registered.
	 */
	List<ParameterBinding> getParameterBindings();

	/**
	 * Creates a new {@literal DeclaredQuery} representing a count query, i.e. a query returning the number of rows to be
	 * expected from the original query, either derived from the query wrapped by this instance or from the information
	 * passed as arguments.
	 *
	 * @param countQueryProjection an optional return type for the query.
	 * @return a new {@literal DeclaredQuery} instance.
	 */
	DeclaredQuery deriveCountQuery(@Nullable String countQueryProjection);

	/**
	 * @return whether paging is implemented in the query itself, e.g. using SpEL expressions.
	 * @since 2.0.6
	 */
	default boolean usesPaging() {
		return false;
	}

	/**
	 * Returns whether the query uses JDBC style parameters, i.e. parameters denoted by a simple ? without any index or
	 * name.
	 *
	 * @return Whether the query uses JDBC style parameters.
	 * @since 2.0.6
	 */
	boolean usesJdbcStyleParameters();

	/**
	 * Return whether the query is a native query of not.
	 *
	 * @return <code>true</code> if native query otherwise <code>false</code>
	 */
	default boolean isNativeQuery() {
		return false;
	}
}
