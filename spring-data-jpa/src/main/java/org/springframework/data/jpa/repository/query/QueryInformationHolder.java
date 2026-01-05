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
package org.springframework.data.jpa.repository.query;

import static org.springframework.data.jpa.repository.query.QueryTokens.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * Stateful object capturing introspection details of a parsed query. Introspection captures the first occurrence of a
 * primary alias or projection items.
 *
 * @author Mark Paluch
 * @since 4.0
 */
class QueryInformationHolder {

	private @Nullable String primaryFromAlias = null;
	private @Nullable List<QueryToken> projection;
	private boolean projectionProcessed;
	private boolean hasConstructorExpression = false;
	private QueryInformation.@Nullable StatementType statementType;

	public @Nullable String getAlias() {
		return primaryFromAlias;
	}

	public void capturePrimaryAlias(String alias) {

		if (primaryFromAlias != null) {
			return;
		}

		this.primaryFromAlias = alias;
	}

	public boolean hasConstructorExpression() {
		return hasConstructorExpression;
	}

	public void constructorExpressionPresent() {
		this.hasConstructorExpression = true;
	}

	public QueryInformation.StatementType getStatementType() {
		return statementType == null ? QueryInformation.StatementType.OTHER : statementType;
	}

	public void setStatementType(QueryInformation.StatementType statementType) {
		if (this.statementType == null) {
			this.statementType = statementType;
		}
	}

	public List<QueryToken> getProjection() {
		return projection == null ? List.of() : List.copyOf(projection);
	}

	/**
	 * Capture projection items if not already captured.
	 *
	 * @param selections collection of the selection items.
	 * @param tokenStreamFunction function that translates a selection item into a {@link QueryTokenStream} (i.e. a
	 *          renderer).
	 */
	public <C> void captureProjection(Collection<C> selections, Function<C, QueryTokenStream> tokenStreamFunction) {

		if (projectionProcessed) {
			return;

		}
		List<QueryToken> selectItemTokens = new ArrayList<>(selections.size() * 2);

		for (C selection : selections) {

			if (!selectItemTokens.isEmpty()) {
				selectItemTokens.add(TOKEN_COMMA);
			}

			selectItemTokens.add(QueryTokens.token(QueryRenderer.from(tokenStreamFunction.apply(selection)).render()));
		}

		projection = selectItemTokens;
		projectionProcessed = true;
	}

}
