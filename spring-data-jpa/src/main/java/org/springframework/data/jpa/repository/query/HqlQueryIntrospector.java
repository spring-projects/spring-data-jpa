/*
 * Copyright 2024-2025 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import org.springframework.data.jpa.repository.query.HqlParser.VariableContext;
import org.springframework.lang.Nullable;

/**
 * {@link ParsedQueryIntrospector} for HQL queries.
 *
 * @author Mark Paluch
 */
@SuppressWarnings({ "UnreachableCode", "ConstantValue" })
class HqlQueryIntrospector extends HqlBaseVisitor<Void> implements ParsedQueryIntrospector {

	private final HqlQueryRenderer renderer = new HqlQueryRenderer();

	private @Nullable String primaryFromAlias = null;
	private @Nullable List<QueryToken> projection;
	private boolean projectionProcessed;
	private boolean hasConstructorExpression = false;

	@Override
	public String getAlias() {
		return primaryFromAlias;
	}

	@Override
	public List<QueryToken> getProjection() {
		return projection == null ? Collections.emptyList() : projection;
	}

	@Override
	public boolean hasConstructorExpression() {
		return hasConstructorExpression;
	}

	@Override
	public Void visitSelectClause(HqlParser.SelectClauseContext ctx) {

		if (!projectionProcessed) {
			projection = captureSelectItems(ctx.selectionList().selection(), renderer);
			projectionProcessed = true;
		}

		return super.visitSelectClause(ctx);
	}

	@Override
	public Void visitFromRoot(HqlParser.FromRootContext ctx) {

		if (primaryFromAlias == null && ctx.variable() != null && !HqlQueryRenderer.isSubquery(ctx)) {
			primaryFromAlias = capturePrimaryAlias(ctx.variable());
		}

		return super.visitFromRoot(ctx);
	}

	@Override
	public Void visitInstantiation(HqlParser.InstantiationContext ctx) {

		hasConstructorExpression = true;

		return super.visitInstantiation(ctx);
	}

	private static String capturePrimaryAlias(VariableContext ctx) {
		return ((ctx).nakedIdentifier() != null ? ctx.nakedIdentifier() : ctx.identifier()).getText();
	}

	private static List<QueryToken> captureSelectItems(List<HqlParser.SelectionContext> selections,
			HqlQueryRenderer itemRenderer) {

		List<QueryToken> selectItemTokens = new ArrayList<>(selections.size() * 2);
		for (HqlParser.SelectionContext selection : selections) {

			if (!selectItemTokens.isEmpty()) {
				selectItemTokens.add(TOKEN_COMMA);
			}

			selectItemTokens.add(QueryTokens.token(QueryRenderer.from(itemRenderer.visitSelection(selection)).render()));
		}
		return selectItemTokens;
	}
}
