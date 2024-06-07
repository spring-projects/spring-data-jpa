/*
 * Copyright 2024 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * {@link ParsedQueryIntrospector} for EQL queries.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("UnreachableCode")
class EqlQueryIntrospector extends EqlBaseVisitor<Void> implements ParsedQueryIntrospector {

	private final EqlQueryRenderer renderer = new EqlQueryRenderer();

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
	public Void visitSelect_clause(EqlParser.Select_clauseContext ctx) {

		List<EqlParser.Select_itemContext> selections = ctx.select_item();
		List<QueryToken> selectItemTokens = new ArrayList<>(selections.size() * 2);

		for (EqlParser.Select_itemContext selection : selections) {

			if (!selectItemTokens.isEmpty()) {
				selectItemTokens.add(TOKEN_COMMA);
			}

			selectItemTokens.add(QueryTokens.token(QueryRenderer.from(renderer.visitSelect_item(selection)).render()));
		}

		if (!projectionProcessed) {
			projection = selectItemTokens;
			projectionProcessed = true;
		}

		return super.visitSelect_clause(ctx);
	}

	@Override
	public Void visitRange_variable_declaration(EqlParser.Range_variable_declarationContext ctx) {

		if (primaryFromAlias == null) {
			primaryFromAlias = ctx.identification_variable() != null ? ctx.identification_variable().getText()
					: ctx.entity_name().getText();
		}

		return super.visitRange_variable_declaration(ctx);
	}

	@Override
	public Void visitConstructor_expression(EqlParser.Constructor_expressionContext ctx) {

		hasConstructorExpression = true;

		return super.visitConstructor_expression(ctx);
	}

}
