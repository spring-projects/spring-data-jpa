/*
 * Copyright 2025-present the original author or authors.
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
import java.util.List;
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

	public AotQueries(AotQuery query) {
		this(query, AbsentCountQuery.INSTANCE);
	}

	/**
	 * Derive a count query from the given query.
	 */
	public static <T extends AotQuery> AotQueries withDerivedCountQuery(T query, Function<T, DeclaredQuery> queryMapper,
			@Nullable String countProjection, QueryEnhancerSelector selector) {

		DeclaredQuery underlyingQuery = queryMapper.apply(query);
		QueryEnhancer queryEnhancer = selector.select(underlyingQuery).create(underlyingQuery);

		if (!queryEnhancer.isSelectQuery()) {
			return new AotQueries(query);
		}

		String derivedCountQuery = queryEnhancer
				.createCountQueryFor(StringUtils.hasText(countProjection) ? countProjection : null);

		return new AotQueries(query, StringAotQuery.of(underlyingQuery.rewrite(derivedCountQuery)));
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

	private static class AbsentCountQuery extends AotQuery {

		static final AbsentCountQuery INSTANCE = new AbsentCountQuery();

		AbsentCountQuery() {
			super(List.of());
		}

		@Override
		public boolean isNative() {
			return false;
		}

		@Override
		public boolean hasConstructorExpressionOrDefaultProjection() {
			return false;
		}
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

			if (result() instanceof AotQuery.NamedQuery nq) {
				serialized.put("name", nq.getQueryName());
			}

			if (result() instanceof StringAotQuery sq) {
				serialized.put("query", sq.getQueryString());
			}

			if (paging) {

				if (count() instanceof AotQuery.NamedQuery nq) {
					serialized.put("count-name", nq.getQueryName());
				}

				if (count() instanceof StringAotQuery sq) {
					serialized.put("count-query", sq.getQueryString());
				}
			}

			return serialized;
		}

	}

}
