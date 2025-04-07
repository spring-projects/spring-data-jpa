/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.aot;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.QueryEnhancer;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.repository.aot.generate.QueryMetadata;
import org.springframework.util.StringUtils;

/**
 * Value object capturing queries used for repository query methods.
 *
 * @author Mark Paluch
 * @since 4.0
 */
record AotQueries(AotQuery result, AotQuery count) {

	/**
	 * Derive a count query from the given query.
	 */
	public static AotQueries from(StringAotQuery query, @Nullable String countProjection,
			QueryEnhancerSelector selector) {
		return from(query, StringAotQuery::getQuery, countProjection, selector);
	}

	/**
	 * Derive a count query from the given query.
	 */
	public static <T extends AotQuery> AotQueries from(T query, Function<T, DeclaredQuery> queryMapper,
			@Nullable String countProjection, QueryEnhancerSelector selector) {

		DeclaredQuery underlyingQuery = queryMapper.apply(query);
		QueryEnhancer queryEnhancer = selector.select(underlyingQuery).create(underlyingQuery);

		String derivedCountQuery = queryEnhancer
				.createCountQueryFor(StringUtils.hasText(countProjection) ? countProjection : null);

		DeclaredQuery countQuery = underlyingQuery.rewrite(derivedCountQuery);
		return new AotQueries(query, StringAotQuery.of(countQuery));
	}

	/**
	 * Create new {@code AotQueries} for the given queries.
	 */
	public static AotQueries from(AotQuery result, AotQuery count) {
		return new AotQueries(result, count);
	}

	public boolean isNative() {
		return result().isNative();
	}

	public QueryMetadata toMetadata(boolean paging) {
		return new AotQueryMetadata(paging);
	}

	/**
	 * String and Named Query-based {@link QueryMetadata}.
	 */
	private class AotQueryMetadata implements QueryMetadata {

		private final boolean paging;

		AotQueryMetadata(boolean paging) {
			this.paging = paging;
		}

		@Override
		public Map<String, Object> serialize() {

			Map<String, Object> serialized = new LinkedHashMap<>();

			if (result() instanceof NamedAotQuery nq) {

				serialized.put("name", nq.getName());
				serialized.put("query", nq.getQueryString());
			}

			if (result() instanceof StringAotQuery sq) {
				serialized.put("query", sq.getQueryString());
			}

			if (paging) {

				if (count() instanceof NamedAotQuery nq) {

					serialized.put("count-name", nq.getName());
					serialized.put("count-query", nq.getQueryString());
				}

				if (count() instanceof StringAotQuery sq) {
					serialized.put("count-query", sq.getQueryString());
				}
			}

			return serialized;
		}

	}

}
