/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * This interface describes the API for enhancing a given Query.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @since 2.7.0
 */
public interface QueryEnhancer {

	/**
	 * Adds {@literal order by} clause to the JPQL query. Uses the first alias to bind the sorting property to.
	 *
	 * @param sort the sort specification to apply.
	 * @return the modified query string.
	 */
	default String applySorting(Sort sort) {
		return applySorting(sort, detectAlias());
	}

	/**
	 * Adds {@literal order by} clause to the JPQL query.
	 *
	 * @param sort the sort specification to apply.
	 * @param alias the alias to be used in the order by clause. May be {@literal null} or empty.
	 * @return the modified query string.
	 */
	String applySorting(Sort sort, @Nullable String alias);

	/**
	 * Resolves the alias for the entity to be retrieved from the given JPA query.
	 *
	 * @return Might return {@literal null}.
	 */
	@Nullable
	String detectAlias();

	/**
	 * Creates a count projected query from the given original query.
	 *
	 * @return Guaranteed to be not {@literal null}.
	 */
	default String createCountQueryFor() {
		return createCountQueryFor(null);
	}

	/**
	 * Creates a count projected query from the given original query using the provided <code>countProjection</code>.
	 *
	 * @param countProjection may be {@literal null}.
	 * @return a query String to be used a count query for pagination. Guaranteed to be not {@literal null}.
	 */
	String createCountQueryFor(@Nullable String countProjection);

	/**
	 * Returns whether the given JPQL query contains a constructor expression.
	 *
	 * @return whether the given JPQL query contains a constructor expression.
	 */
	default boolean hasConstructorExpression() {
		return QueryUtils.hasConstructorExpression(getQuery().getQueryString());
	}

	/**
	 * Returns the projection part of the query, i.e. everything between {@code select} and {@code from}.
	 *
	 * @return the projection part of the query.
	 */
	String getProjection();

	Set<String> getJoinAliases();

	/**
	 * Gets the query we want to use for enhancements.
	 *
	 * @return non-null {@link DeclaredQuery} that wraps the query
	 */
	DeclaredQuery getQuery();
}
