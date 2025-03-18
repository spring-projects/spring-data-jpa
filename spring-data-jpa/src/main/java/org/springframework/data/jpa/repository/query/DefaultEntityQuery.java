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

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Encapsulation of a JPA query string, typically returning entities or DTOs. Provides access to parameter bindings.
 * <p>
 * The internal {@link PreprocessedQuery query string} is cleaned from decorated parameters like {@literal %:lastname%}
 * and the matching bindings take care of applying the decorations in the {@link ParameterBinding#prepare(Object)}
 * method. Note that this class also handles replacing SpEL expressions with synthetic bind parameters.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Oliver Wehrens
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Yuriy Tsarkov
 * @since 4.0
 */
class DefaultEntityQuery implements EntityQuery, DeclaredQuery {

	private final PreprocessedQuery query;
	private final QueryEnhancer queryEnhancer;

	DefaultEntityQuery(PreprocessedQuery query, QueryEnhancerFactory queryEnhancerFactory) {
		this.query = query;
		this.queryEnhancer = queryEnhancerFactory.create(query);
	}

	@Override
	public boolean isNative() {
		return query.isNative();
	}

	@Override
	public String getQueryString() {
		return query.getQueryString();
	}

	/**
	 * Returns whether we have found some like bindings.
	 */
	@Override
	public boolean hasParameterBindings() {
		return this.query.hasBindings();
	}

	@Override
	public boolean usesJdbcStyleParameters() {
		return query.usesJdbcStyleParameters();
	}

	@Override
	public boolean hasNamedParameter() {
		return query.hasNamedBindings();
	}

	@Override
	public List<ParameterBinding> getParameterBindings() {
		return this.query.getBindings();
	}

	@Override
	public boolean hasConstructorExpression() {
		return queryEnhancer.hasConstructorExpression();
	}

	@Override
	public boolean isDefaultProjection() {
		return queryEnhancer.getProjection().equalsIgnoreCase(getAlias());
	}

	@Nullable
	String getAlias() {
		return queryEnhancer.detectAlias();
	}

	@Override
	public boolean usesPaging() {
		return query.containsPageableInSpel();
	}

	String getProjection() {
		return this.queryEnhancer.getProjection();
	}

	@Override
	public ParametrizedQuery deriveCountQuery(@Nullable String countQueryProjection) {
		return new SimpleParametrizedQuery(this.query.rewrite(queryEnhancer.createCountQueryFor(countQueryProjection)));
	}

	@Override
	public QueryProvider rewrite(QueryEnhancer.QueryRewriteInformation rewriteInformation) {
		return this.query.rewrite(queryEnhancer.rewrite(rewriteInformation));
	}

	@Override
	public String toString() {
		return "EntityQuery[" + getQueryString() + ", " + getParameterBindings() + ']';
	}

	/**
	 * Simple {@link ParametrizedQuery} variant forwarding to {@link PreprocessedQuery}.
	 */
	static class SimpleParametrizedQuery implements ParametrizedQuery {

		private final PreprocessedQuery query;

		SimpleParametrizedQuery(PreprocessedQuery query) {
			this.query = query;
		}

		@Override
		public String getQueryString() {
			return query.getQueryString();
		}

		@Override
		public boolean hasParameterBindings() {
			return query.hasBindings();
		}

		@Override
		public boolean usesJdbcStyleParameters() {
			return query.usesJdbcStyleParameters();
		}

		@Override
		public boolean hasNamedParameter() {
			return query.hasNamedBindings();
		}

		@Override
		public List<ParameterBinding> getParameterBindings() {
			return query.getBindings();
		}

	}

}
