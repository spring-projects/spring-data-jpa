/*
 * Copyright 2022-present the original author or authors.
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

import static org.springframework.data.jpa.repository.query.QueryTokens.TOKEN_AS;
import static org.springframework.data.jpa.repository.query.QueryTokens.TOKEN_DOUBLE_UNDERSCORE;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.query.JpqlParser.Constructor_expressionContext;
import org.springframework.data.jpa.repository.query.JpqlParser.Constructor_itemContext;
import org.springframework.data.jpa.repository.query.JpqlParser.Select_itemContext;
import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed JPQL query into a
 * {@code COUNT(…)} query.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.1
 */
class JpqlCountQueryTransformer extends JpqlQueryRenderer
		implements CountSelectionSupport<Select_itemContext, Constructor_expressionContext, Constructor_itemContext> {

	private final @Nullable String countProjection;
	private final @Nullable String primaryFromAlias;

	JpqlCountQueryTransformer(@Nullable String countProjection, QueryInformation queryInformation) {
		this.countProjection = countProjection;
		this.primaryFromAlias = queryInformation.getAlias();
	}

	@Override
	public QueryTokenStream visitSelectQuery(JpqlParser.SelectQueryContext ctx) {

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

		return builder;
	}

	@Override
	public QueryTokenStream visitFromQuery(JpqlParser.FromQueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(QueryTransformers.selectCount(countProjection, primaryFromAlias));

		if (ctx.from_clause() != null) {
			builder.appendExpression(visit(ctx.from_clause()));
			if (primaryFromAlias == null) {
				builder.append(TOKEN_AS);
				builder.append(TOKEN_DOUBLE_UNDERSCORE);
			}
		}

		if (ctx.where_clause() != null) {
			builder.appendExpression(visit(ctx.where_clause()));
		}
		if (ctx.groupby_clause() != null) {
			builder.appendExpression(visit(ctx.groupby_clause()));
		}
		if (ctx.having_clause() != null) {
			builder.appendExpression(visit(ctx.having_clause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSelect_clause(JpqlParser.Select_clauseContext ctx) {
		return renderCountSelection(QueryTokens.expression(ctx.SELECT()), QueryTokens.expressionOrNull(ctx.DISTINCT()),
				ctx.select_item());
	}

	@Override
	public QueryTokenStream visitSelect_item(JpqlParser.Select_itemContext ctx) {
		return visit(ctx.select_expression());
	}

	@Override
	public QueryTokenStream visitConstructor_expression(JpqlParser.Constructor_expressionContext ctx) {
		return renderConstructor(ctx);
	}

	@Override
	public @Nullable String getPrimaryAlias() {
		return primaryFromAlias;
	}

	@Override
	public @Nullable String getCountProjection() {
		return countProjection;
	}

	@Override
	public QueryTokenStream renderSelectItem(Select_itemContext selectItem) {
		return visit(selectItem);
	}

	@Override
	public QueryTokenStream renderConstructorArgument(Constructor_itemContext argument) {
		return visit(argument);
	}

	@Override
	public QueryTokenStream renderCountSelectionFallback(List<Select_itemContext> selectItems) {

		if (selectItems.isEmpty()) {
			// cannot happen as per grammar, but you never know…
			return QueryTokens.token("1");
		}

		// count(*) is not supported - use first select item
		return renderSelectItem(selectItems.get(0));
	}

	@Override
	public @Nullable Constructor_expressionContext getConstructorExpression(Select_itemContext selectItem) {
		return selectItem.select_expression().constructor_expression();
	}

	@Override
	public List<Constructor_itemContext> getConstructorArguments(Constructor_expressionContext constructorExpression) {
		return constructorExpression.constructor_item();
	}

	@Override
	public boolean isPath(Constructor_itemContext argument) {
		return argument.single_valued_path_expression() != null || argument.identification_variable() != null;
	}

}
