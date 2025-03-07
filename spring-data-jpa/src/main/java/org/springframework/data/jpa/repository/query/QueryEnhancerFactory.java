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

/**
 * Encapsulates different strategies for the creation of a {@link QueryEnhancer} from a {@link IntrospectedQuery}.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.7
 */
public interface QueryEnhancerFactory {

	/**
	 * Returns whether this QueryEnhancerFactory supports the given {@link DeclaredQuery}.
	 *
	 * @param query the query to be enhanced and introspected.
	 * @return {@code true} if this QueryEnhancer supports the given query; {@code false} otherwise.
	 */
	boolean supports(DeclaredQuery query);

	/**
	 * Creates a new {@link QueryEnhancer} for the given query.
	 *
	 * @param query the query to be enhanced and introspected.
	 * @return
	 */
	QueryEnhancer create(StructuredQuery query);

	/**
	 * Creates a new {@link QueryEnhancerFactory} for the given {@link DeclaredQuery}.
	 *
	 * @param query must not be {@literal null}.
	 * @return an implementation of {@link QueryEnhancer} that suits the query the most
	 */
	static QueryEnhancerFactory forQuery(DeclaredQuery query) {
		return QueryEnhancerSelector.DEFAULT_SELECTOR.select(query);
	}

}
