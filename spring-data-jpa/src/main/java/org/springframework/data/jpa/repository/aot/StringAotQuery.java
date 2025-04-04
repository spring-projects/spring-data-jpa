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

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.ParameterBinding;
import org.springframework.data.jpa.repository.query.PreprocessedQuery;
import org.springframework.data.jpa.repository.query.QueryProvider;

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

	/**
	 * Creates a new {@code StringAotQuery} from a {@link DeclaredQuery}. Parses the query into {@link PreprocessedQuery}.
	 */
	static StringAotQuery of(DeclaredQuery query) {

		if (query instanceof PreprocessedQuery pq) {
			return new DeclaredAotQuery(pq, false);
		}

		return new DeclaredAotQuery(PreprocessedQuery.parse(query), false);
	}

	/**
	 * Creates a new {@code StringAotQuery} from a JPQL {@code queryString}. Parses the query into
	 * {@link PreprocessedQuery}.
	 */
	static StringAotQuery jpqlQuery(String queryString) {
		return of(DeclaredQuery.jpqlQuery(queryString));
	}

	/**
	 * Creates a JPQL {@code StringAotQuery} using the given bindings and limit.
	 */
	public static StringAotQuery jpqlQuery(String queryString, List<ParameterBinding> bindings, Limit resultLimit,
			boolean delete, boolean exists) {
		return new DerivedAotQuery(queryString, bindings, resultLimit, delete, exists);
	}

	/**
	 * Creates a new {@code StringAotQuery} from a native (SQL) {@code queryString}. Parses the query into
	 * {@link PreprocessedQuery}.
	 */
	static StringAotQuery nativeQuery(String queryString) {
		return of(DeclaredQuery.nativeQuery(queryString));
	}

	/**
	 * @return the underlying declared query.
	 */
	public abstract DeclaredQuery getQuery();

	public String getQueryString() {
		return getQuery().getQueryString();
	}

	/**
	 * @return {@literal true} if query is expected to return the declared method type directly; {@literal false} if the
	 *         result requires projection post-processing. See also {@code NativeJpaQuery#getTypeToQueryFor}.
	 */
	public abstract boolean hasConstructorExpressionOrDefaultProjection();

	/**
	 * @return a new {@link StringAotQuery} using constructor expressions or containing the default (primary alias)
	 *         projection.
	 */
	public abstract StringAotQuery withConstructorExpressionOrDefaultProjection();

	@Override
	public String toString() {
		return getQueryString();
	}

	public abstract StringAotQuery rewrite(QueryProvider rewritten);

	/**
	 * @author Christoph Strobl
	 * @author Mark Paluch
	 */
	private static class DeclaredAotQuery extends StringAotQuery {

		private final PreprocessedQuery query;
		private final boolean constructorExpressionOrDefaultProjection;

		DeclaredAotQuery(PreprocessedQuery query, boolean constructorExpressionOrDefaultProjection) {
			super(query.getBindings());
			this.query = query;
			this.constructorExpressionOrDefaultProjection = constructorExpressionOrDefaultProjection;
		}

		@Override
		public PreprocessedQuery getQuery() {
			return query;
		}

		@Override
		public String getQueryString() {
			return query.getQueryString();
		}

		@Override
		public boolean isNative() {
			return query.isNative();
		}

		@Override
		public boolean hasConstructorExpressionOrDefaultProjection() {
			return constructorExpressionOrDefaultProjection;
		}

		@Override
		public StringAotQuery withConstructorExpressionOrDefaultProjection() {
			return new DeclaredAotQuery(query, true);
		}

		@Override
		public StringAotQuery rewrite(QueryProvider rewritten) {
			return new DeclaredAotQuery(query.rewrite(rewritten.getQueryString()), constructorExpressionOrDefaultProjection);
		}

	}

	/**
	 * PartTree (derived) Query with a limit associated.
	 *
	 * @author Mark Paluch
	 */
	private static class DerivedAotQuery extends StringAotQuery {

		private final String queryString;
		private final Limit limit;
		private final boolean delete;
		private final boolean exists;

		DerivedAotQuery(String queryString, List<ParameterBinding> parameterBindings, Limit limit, boolean delete,
				boolean exists) {
			super(parameterBindings);
			this.queryString = queryString;
			this.limit = limit;
			this.delete = delete;
			this.exists = exists;
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

		@Override
		public boolean isDelete() {
			return delete;
		}

		@Override
		public boolean isExists() {
			return exists;
		}

		@Override
		public boolean hasConstructorExpressionOrDefaultProjection() {
			return false;
		}

		@Override
		public StringAotQuery withConstructorExpressionOrDefaultProjection() {
			return this;
		}

		@Override
		public StringAotQuery rewrite(QueryProvider rewritten) {
			return new DerivedAotQuery(rewritten.getQueryString(), this.getParameterBindings(), getLimit(), delete, exists);
		}

	}
}
