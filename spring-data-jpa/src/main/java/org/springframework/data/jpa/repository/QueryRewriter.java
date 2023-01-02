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
package org.springframework.data.jpa.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Callback to rewrite a query and apply sorting and pagination settings that cannot be applied based on a regularly
 * detectable scheme.
 * <p>
 * The underlying the query is the one right before it is used for query object creation, so everything that Spring Data
 * and tools intends to do has been done. You can customize the query to apply final changes. Rewriting can only make
 * use of already existing contextual data. That is, adding or replacing query text or reuse of bound parameters. Query
 * rewriting must not add additional bindable parameters as these cannot be materialized.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.0
 * @see jakarta.persistence.EntityManager#createQuery
 * @see jakarta.persistence.EntityManager#createNativeQuery
 */
@FunctionalInterface
public interface QueryRewriter {

	/**
	 * Rewrite the assembled query with the given {@link Sort}.
	 * <p>
	 * WARNING: No checks are performed before the transformed query is passed to the EntityManager.
	 *
	 * @param query the assembled query.
	 * @param sort current {@link Sort} settings provided by the method, or {@link Sort#unsorted()}} if there are none.
	 * @return the query to be used with the {@code EntityManager}.
	 */
	String rewrite(String query, Sort sort);

	/**
	 * Rewrite the assembled query with the given {@link Pageable}.
	 *
	 * @param query the assembled query.
	 * @param pageRequest current {@link Pageable} settings provided by the method, or {@link Pageable#unpaged()} if not
	 *          paged.
	 * @return the query to be used with the {@code EntityManager}.
	 */
	default String rewrite(String query, Pageable pageRequest) {
		return rewrite(query, pageRequest.getSort());
	}

	/**
	 * A {@link QueryRewriter} that doesn't change the query.
	 */
	enum IdentityQueryRewriter implements QueryRewriter {

		INSTANCE;

		@Override
		public String rewrite(String query, Sort sort) {
			return query;
		}
	}
}
