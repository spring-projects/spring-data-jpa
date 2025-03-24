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
package org.springframework.data.jpa.repository.aot.generated;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.ParameterBinding;
import org.springframework.data.jpa.repository.query.PreprocessedQuery;

/**
 * An AOT query represented by a string.
 *
 * @author Mark Paluch
 * @since 4.0
 */
abstract class StringAotQuery extends AotQuery {

	private StringAotQuery(List<ParameterBinding> parameterBindings) {
		super(parameterBindings);
	}

	static StringAotQuery of(DeclaredQuery query) {

		if (query instanceof PreprocessedQuery pq) {
			return new DeclaredAotQuery(pq);
		}

		return new DeclaredAotQuery(PreprocessedQuery.parse(query));
	}

	static StringAotQuery jpqlQuery(String queryString) {
		return of(DeclaredQuery.jpqlQuery(queryString));
	}

	public static StringAotQuery jpqlQuery(String queryString, List<ParameterBinding> bindings, Limit resultLimit) {
		return new LimitedAotQuery(queryString, bindings, resultLimit);
	}

	static StringAotQuery nativeQuery(String queryString) {
		return of(DeclaredQuery.nativeQuery(queryString));
	}

	public abstract DeclaredQuery getQuery();

	public abstract String getQueryString();

	@Override
	public String toString() {
		return getQueryString();
	}

	/**
	 * @author Christoph Strobl
	 * @author Mark Paluch
	 */
	static class DeclaredAotQuery extends StringAotQuery {

		private final PreprocessedQuery query;

		DeclaredAotQuery(PreprocessedQuery query) {
			super(query.getBindings());
			this.query = query;
		}

		@Override
		public String getQueryString() {
			return query.getQueryString();
		}

		@Override
		public boolean isNative() {
			return query.isNative();
		}

		public PreprocessedQuery getQuery() {
			return query;
		}

	}

	/**
	 * @author Mark Paluch
	 */
	static class LimitedAotQuery extends StringAotQuery {

		private final String queryString;
		private final Limit limit;

		LimitedAotQuery(String queryString, List<ParameterBinding> parameterBindings, Limit limit) {
			super(parameterBindings);
			this.queryString = queryString;
			this.limit = limit;
		}

		@Override
		public DeclaredQuery getQuery() {
			return DeclaredQuery.jpqlQuery(queryString);
		}

		@Override
		public String getQueryString() {
			return queryString;
		}

		@Override
		public boolean isNative() {
			return false;
		}

		@Override
		public Limit getLimit() {
			return limit;
		}

	}
}
