/*
 * Copyright 2008-2022 the original author or authors.
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
 * Callback to rewrite a query right before it's handed to the EntityManager.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
@FunctionalInterface
public interface QueryRewriter {

	/**
	 * The assembled query and current {@link Sort} settings are offered. This is the query right before it's handed to
	 * the EntityManager, so everything that Spring Data and tools intends to do has been done. The user is able to make
	 * any last minute changes.<br/>
	 * <br/>
	 * WARNING: No checks are performed before the transformed query is passed to the EntityManager.
	 * 
	 * @param query - the assembled generated query, right before it's handed over to the EntityManager.
	 * @param sort - current {@link Sort} settings provided by the method, or {@link Sort#unsorted()}} if there are none.
	 * @return alter the query however you like.
	 */
	String rewrite(String query, Sort sort);

	/**
	 * This alternative is used to handle {@link Pageable}-based methods.
	 * 
	 * @param query - the assembled generated query, right before it's handed over to the EntityManager.
	 * @param pageRequest
	 * @return
	 */
	default String rewrite(String query, Pageable pageRequest) {
		return rewrite(query, pageRequest.getSort());
	}

	/**
	 * A {@link QueryRewriter} that doesn't change the query.
	 */
	public class NoopQueryRewriter implements QueryRewriter {

		@Override
		public String rewrite(String query, Sort sort) {
			return query;
		}
	}
}
