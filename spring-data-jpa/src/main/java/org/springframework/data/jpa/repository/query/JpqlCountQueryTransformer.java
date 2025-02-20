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

import static org.springframework.data.jpa.repository.query.QueryTokens.*;

import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.data.jpa.repository.query.QueryTransformers.CountSelectionTokenStream;
import org.springframework.util.StringUtils;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed JPQL query into a
 * {@code COUNT(…)} query.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.1
 */
@SuppressWarnings("ConstantValue")
class JpqlCountQueryTransformer extends JpqlQueryRenderer {

	private final @Nullable String countProjection;
	private final @Nullable String primaryFromAlias;

	JpqlCountQueryTransformer(@Nullable String countProjection, QueryInformation queryInformation) {
		this.countProjection = countProjection;
		this.primaryFromAlias = queryInformation.getAlias();
	}

	@Override
	public QueryTokenStream visitSelect_statement(JpqlParser.Select_statementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.select_clause()));
		builder.appendExpression(visit(ctx.from_clause()));

		if (ctx.where_clause() != null) {
			builder.appendExpression(visit(ctx.where_clause()));
		}
		if (ctx.groupby_clause() != null) {
			builder.appendExpression(visit(ctx.groupby_clause()));
		}
		if (ctx.having_clause() != null) {
			builder.appendExpression(visit(ctx.having_clause()));
		}
		if (ctx.set_fuction() != null) {
			builder.appendExpression(visit(ctx.set_fuction()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSelect_clause(JpqlParser.Select_clauseContext ctx) {

		boolean usesDistinct = ctx.DISTINCT() != null;

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.SELECT()));
		builder.append(TOKEN_COUNT_FUNC);

		QueryRendererBuilder nested = QueryRenderer.builder();
		if (countProjection == null) {
			if (usesDistinct) {
				nested.append(QueryTokens.expression(ctx.DISTINCT()));
				nested.append(getDistinctCountSelection(QueryTokenStream.concat(ctx.select_item(), this::visit, TOKEN_COMMA)));
			} else if (StringUtils.hasText(primaryFromAlias)) {
				nested.append(QueryTokens.token(primaryFromAlias));
			} else {
				throw new IllegalStateException("No primary alias present");
			}
		} else {
			builder.append(QueryTokens.token(countProjection));
			if (usesDistinct) {
				nested.append(QueryTokens.expression(ctx.DISTINCT()));
			}
		}

		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	private QueryRendererBuilder getDistinctCountSelection(QueryTokenStream selectionListbuilder) {

		QueryRendererBuilder nested = new QueryRendererBuilder();
		CountSelectionTokenStream countSelection = CountSelectionTokenStream.create(selectionListbuilder);

		if (countSelection.requiresPrimaryAlias()) {
			// constructor
			if (primaryFromAlias == null) {
				throw new IllegalStateException(
						"Primary alias must be set for DISTINCT count selection using constructor expressions");
			}
			nested.append(QueryTokens.token(primaryFromAlias));
		} else {
			// keep all the select items to distinct against
			nested.append(countSelection);
		}
		return nested;
	}

}
