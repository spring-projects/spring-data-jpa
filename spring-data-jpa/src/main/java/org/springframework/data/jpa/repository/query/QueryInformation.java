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
 * Value object capturing introspection details of a parsed query.
 *
 * @author Mark Paluch
 * @author Soomin Kim
 * @since 3.5
 */
class QueryInformation {

	private final @Nullable String alias;
	private final List<QueryToken> projection;
	private final boolean hasConstructorExpression;
	private final StatementType statementType;

	QueryInformation(QueryInformationHolder introspection) {
		this.alias = introspection.getAlias();
		this.projection = introspection.getProjection();
		this.hasConstructorExpression = introspection.hasConstructorExpression();
		this.statementType = introspection.getStatementType();
	}

	/**
	 * Primary table alias. Contains the first table name/table alias in case multiple tables are specified in the query.
	 *
	 * @return
	 */
	public @Nullable String getAlias() {
		return alias;
	}

	/**
	 * @return the primary selection.
	 */
	public List<QueryToken> getProjection() {
		return projection;
	}

	/**
	 * @return {@code true} if the query uses a constructor expression.
	 */
	public boolean hasConstructorExpression() {
		return hasConstructorExpression;
	}

	/**
	 * @return the statement type of the query.
	 * @since 4.0
	 */
	public StatementType getStatementType() {
		return statementType;
	}

	/**
	 * @return {@code true} if the query is a SELECT statement.
	 * @since 4.0
	 */
	public boolean isSelectStatement() {
		return statementType == StatementType.SELECT;
	}

	/**
	 * Enum representing the type of SQL/JPQL statement.
	 *
	 * @since 4.0
	 */
	enum StatementType {

		/**
		 * SELECT statement.
		 */
		SELECT,

		/**
		 * INSERT statement.
		 */
		INSERT,

		/**
		 * UPDATE statement.
		 */
		UPDATE,

		/**
		 * DELETE statement.
		 */
		DELETE,

		/**
		 * MERGE statement.
		 */
		MERGE,

		/**
		 * Other statement types.
		 */
		OTHER
	}

}
