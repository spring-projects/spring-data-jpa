/*
 * Copyright 2022-2025 the original author or authors.
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

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ReturnedType;

/**
 * This interface describes the API for enhancing a given Query.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 2.7
 */
public interface QueryEnhancer {

	/**
	 * Creates a new {@link QueryEnhancer} for a {@link DeclaredQuery}. Convenience method for
	 * {@link QueryEnhancerFactory#create(QueryProvider)}.
	 *
	 * @param query the query to be enhanced.
	 * @return the new {@link QueryEnhancer}.
	 * @since 4.0
	 */
	static QueryEnhancer create(DeclaredQuery query) {
		return QueryEnhancerFactory.forQuery(query).create(query);
	}

	/**
	 * Returns whether the given JPQL query contains a constructor expression.
	 *
	 * @return whether the given JPQL query contains a constructor expression.
	 */
	boolean hasConstructorExpression();

	/**
	 * Resolves the primary alias for the entity to be retrieved from the given JPA query.
	 *
	 * @return can return {@literal null}.
	 */
	@Nullable
	String detectAlias();

	/**
	 * Returns the projection part of the query, i.e. everything between {@code select} and {@code from}.
	 *
	 * @return the projection part of the query.
	 */
	String getProjection();

	/**
	 * Gets the query we want to use for enhancements.
	 *
	 * @return non-null {@link DeclaredQuery} that wraps the query.
	 */
	QueryProvider getQuery();

	/**
	 * Rewrite the query to include sorting and apply {@link ReturnedType} customizations.
	 *
	 * @param rewriteInformation the rewrite information to apply.
	 * @return the modified query string.
	 * @since 4.0
	 */
	String rewrite(QueryRewriteInformation rewriteInformation);

	/**
	 * Creates a count projected query from the given original query using the provided {@code countProjection}.
	 *
	 * @param countProjection may be {@literal null}.
	 * @return a query String to be used a count query for pagination. Guaranteed to be not {@literal null}.
	 */
	String createCountQueryFor(@Nullable String countProjection);

	/**
	 * Interface to describe the information needed to rewrite a query.
	 *
	 * @since 4.0
	 */
	interface QueryRewriteInformation {

		/**
		 * @return the sort specification to apply.
		 */
		Sort getSort();

		/**
		 * @return type expected to be returned by the query.
		 */
		ReturnedType getReturnedType();

	}

}
