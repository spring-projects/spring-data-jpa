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
package org.springframework.data.jpa.repository.query;

import org.springframework.util.ObjectUtils;

/**
 * Utility class encapsulating {@code DeclaredQuery} implementations.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class DeclaredQueries {

	static final class JpqlQuery implements DeclaredQuery {

		private final String jpql;

		JpqlQuery(String jpql) {
			this.jpql = jpql;
		}

		@Override
		public boolean isNative() {
			return false;
		}

		@Override
		public String getQueryString() {
			return jpql;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof JpqlQuery jpqlQuery)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(jpql, jpqlQuery.jpql);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(jpql);
		}

		@Override
		public String toString() {
			return "JPQL[" + jpql + "]";
		}

	}

	static final class NativeQuery implements DeclaredQuery {

		private final String sql;

		NativeQuery(String sql) {
			this.sql = sql;
		}

		@Override
		public boolean isNative() {
			return true;
		}

		@Override
		public String getQueryString() {
			return sql;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof NativeQuery that)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(sql, that.sql);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(sql);
		}

		@Override
		public String toString() {
			return "Native[" + sql + "]";
		}

	}

	/**
	 * A rewritten {@link DeclaredQuery} holding a reference to its original query.
	 */
	static class RewrittenQuery implements DeclaredQuery {

		private final DeclaredQuery source;
		private final String queryString;

		public RewrittenQuery(DeclaredQuery source, String queryString) {
			this.source = source;
			this.queryString = queryString;
		}

		@Override
		public boolean isNative() {
			return source.isNative();
		}

		@Override
		public String getQueryString() {
			return queryString;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof RewrittenQuery that)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(queryString, that.queryString);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(queryString);
		}

		@Override
		public String toString() {
			return isNative() ? "Rewritten Native[" + queryString + "]" : "Rewritten JPQL[" + queryString + "]";
		}

	}

}
