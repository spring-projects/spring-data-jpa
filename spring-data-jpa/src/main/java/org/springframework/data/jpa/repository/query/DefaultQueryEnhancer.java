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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * The implementation of the Regex-based {@link QueryEnhancer} using {@link QueryUtils}.
 *
 * @author Diego Krupitza
 * @author kssumin
 * @since 2.7.0
 */
class DefaultQueryEnhancer implements QueryEnhancer {

	private static final Pattern STATEMENT_TYPE_PATTERN = Pattern.compile(
			"^\\s*(SELECT|FROM|INSERT|UPDATE|DELETE|MERGE)", Pattern.CASE_INSENSITIVE);

	private final QueryProvider query;
	private final boolean hasConstructorExpression;
	private final @Nullable  String alias;
	private final String projection;
	private final StatementType statementType;

	public DefaultQueryEnhancer(QueryProvider query) {
		this.query = query;
		this.hasConstructorExpression = QueryUtils.hasConstructorExpression(query.getQueryString());
		this.alias = QueryUtils.detectAlias(query.getQueryString());
		this.projection = QueryUtils.getProjection(this.query.getQueryString());
		this.statementType = detectStatementType(query.getQueryString());
	}

	@Override
	public String rewrite(QueryRewriteInformation rewriteInformation) {

		if (statementType != StatementType.SELECT && !rewriteInformation.getSort().isUnsorted()) {
			throw new IllegalStateException(String.format(
					"Cannot apply sorting to %s statement. Sorting is only supported for SELECT statements.",
					statementType));
		}

		return QueryUtils.applySorting(this.query.getQueryString(), rewriteInformation.getSort(), alias);
	}

	@Override
	public String createCountQueryFor(@Nullable String countProjection) {

		if (statementType != StatementType.SELECT) {
			throw new IllegalStateException(String.format(
					"Cannot derive count query for %s statement. Count queries are only supported for SELECT statements.",
					statementType));
		}

		boolean nativeQuery = this.query instanceof DeclaredQuery dc ? dc.isNative() : true;
		return QueryUtils.createCountQueryFor(this.query.getQueryString(), countProjection, nativeQuery);
	}

	private static StatementType detectStatementType(String query) {

		Matcher matcher = STATEMENT_TYPE_PATTERN.matcher(query);
		if (matcher.find()) {
			String type = matcher.group(1).toUpperCase(Locale.ENGLISH);
			return switch (type) {
				case "SELECT", "FROM" -> StatementType.SELECT; // FROM is also a SELECT in JPQL
				case "INSERT" -> StatementType.INSERT;
				case "UPDATE" -> StatementType.UPDATE;
				case "DELETE" -> StatementType.DELETE;
				case "MERGE" -> StatementType.MERGE;
				default -> StatementType.OTHER;
			};
		}
		return StatementType.OTHER;
	}

	enum StatementType {
		SELECT, INSERT, UPDATE, DELETE, MERGE, OTHER
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
