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
import org.springframework.data.jpa.repository.query.EntityQuery;
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
	 * Creates a new {@code StringAotQuery} from a {@link EntityQuery}. Parses the query into {@link PreprocessedQuery}.
	 */
	static StringAotQuery of(EntityQuery query) {
		return new DeclaredAotQuery(query);
	}

	/**
	 * Creates a new named (via {@link org.springframework.data.repository.core.NamedQueries}) {@code StringAotQuery} from
	 * a {@link EntityQuery}. Parses the query into {@link PreprocessedQuery}.
	 */
	static StringAotQuery named(String queryName, EntityQuery query) {
		return new NamedStringAotQuery(queryName, query);
	}

	/**
	 * Creates a JPQL {@code StringAotQuery} using the given bindings and limit.
	 */
	public static StringAotQuery jpqlQuery(String queryString, List<ParameterBinding> bindings, Limit resultLimit,
			boolean delete, boolean exists) {
		return new DerivedAotQuery(queryString, bindings, resultLimit, delete, exists);
	}

	/**
	 * @return the underlying declared query.
	 */
	public abstract DeclaredQuery getQuery();

	public String getQueryString() {
		return getQuery().getQueryString();
	}

	/**
	 * @return {@literal true} if query uses an own paging mechanism through {@code {#pageable}}.
	 */
	public abstract boolean hasPagingExpression();

	public abstract StringAotQuery rewrite(QueryProvider rewritten);

	@Override
	public String toString() {
		return getQueryString();
	}

	/**
	 * @author Christoph Strobl
	 * @author Mark Paluch
	 */
	private static class DeclaredAotQuery extends StringAotQuery {

		private final PreprocessedQuery query;
		private final boolean constructorExpressionOrDefaultProjection;
		private final boolean hasPagingExpression;

		DeclaredAotQuery(EntityQuery query) {
			super(query.getParameterBindings());
			this.query = query.getQuery();
			this.hasPagingExpression = query.usesPaging();
			this.constructorExpressionOrDefaultProjection = hasConstructorExpressionOrDefaultProjection(query);
		}

		DeclaredAotQuery(PreprocessedQuery query, boolean constructorExpressionOrDefaultProjection) {
			super(query.getBindings());
			this.query = query;
			this.hasPagingExpression = query.containsPageableInSpel();
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
		public boolean hasPagingExpression() {
			return hasPagingExpression;
		}

		@Override
		public StringAotQuery rewrite(QueryProvider rewritten) {
			return new DeclaredAotQuery(query.rewrite(rewritten.getQueryString()), constructorExpressionOrDefaultProjection);
		}

	}

	static class NamedStringAotQuery extends DeclaredAotQuery {

		private final String queryName;

		NamedStringAotQuery(String queryName, EntityQuery entityQuery) {
			super(entityQuery);
			this.queryName = queryName;
		}

		NamedStringAotQuery(String queryName, PreprocessedQuery query, boolean constructorExpressionOrDefaultProjection) {
			super(query, constructorExpressionOrDefaultProjection);
			this.queryName = queryName;
		}

		public String getQueryName() {
			return queryName;
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
		public boolean hasPagingExpression() {
			return false;
		}

		@Override
		public StringAotQuery rewrite(QueryProvider rewritten) {
			return new DerivedAotQuery(rewritten.getQueryString(), this.getParameterBindings(), getLimit(), delete, exists);
		}

	}
}
