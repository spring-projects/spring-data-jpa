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

import org.jspecify.annotations.Nullable;

/**
 * The implementation of the Regex-based {@link QueryEnhancer} using {@link QueryUtils}.
 *
 * @author Diego Krupitza
 * @since 2.7.0
 */
class DefaultQueryEnhancer implements QueryEnhancer {

	private final QueryProvider query;
	private final boolean hasConstructorExpression;
	private final @Nullable  String alias;
	private final String projection;

	public DefaultQueryEnhancer(QueryProvider query) {
		this.query = query;
		this.hasConstructorExpression = QueryUtils.hasConstructorExpression(query.getQueryString());
		this.alias = QueryUtils.detectAlias(query.getQueryString());
		this.projection = QueryUtils.getProjection(this.query.getQueryString());
	}

	@Override
	public String rewrite(QueryRewriteInformation rewriteInformation) {
		return QueryUtils.applySorting(this.query.getQueryString(), rewriteInformation.getSort(), alias);
	}

	@Override
	public String createCountQueryFor(@Nullable String countProjection) {

		boolean nativeQuery = this.query instanceof DeclaredQuery dc ? dc.isNative() : true;
		return QueryUtils.createCountQueryFor(this.query.getQueryString(), countProjection, nativeQuery);
	}

	@Override
	public boolean hasConstructorExpression() {
		return this.hasConstructorExpression;
	}

	@Override
	public @Nullable String detectAlias() {
		return this.alias;
	}

	@Override
	public String getProjection() {
		return this.projection;
	}

	@Override
	public QueryProvider getQuery() {
		return this.query;
	}

}
