/*
 * Copyright 2022-2024 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.JpaQueryParsingToken.*;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed JPQL query by applying
 * {@link Sort}.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.1
 */
@SuppressWarnings("ConstantValue")
class JpqlSortedQueryTransformer extends JpqlQueryRenderer {

	private final JpaQueryTransformerSupport transformerSupport = new JpaQueryTransformerSupport();
	private final Sort sort;
	private final @Nullable String primaryFromAlias;

	JpqlSortedQueryTransformer(Sort sort, @Nullable String primaryFromAlias) {

		Assert.notNull(sort, "Sort must not be null");

		this.sort = sort;
		this.primaryFromAlias = primaryFromAlias;
	}

	@Override
	public QueryRendererBuilder visitSelect_statement(JpqlParser.Select_statementContext ctx) {

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

		doVisitOrderBy(builder, ctx);

		return builder;
	}

	private void doVisitOrderBy(QueryRendererBuilder builder, JpqlParser.Select_statementContext ctx) {

		if (ctx.orderby_clause() != null) {
			QueryRendererBuilder existingOrder = visit(ctx.orderby_clause());
			if (sort.isSorted()) {
				builder.appendInline(existingOrder);
			} else {
				builder.append(existingOrder);
			}
		}

		if (sort.isSorted()) {

			List<JpaQueryParsingToken> sortBy = transformerSupport.orderBy(primaryFromAlias, sort);

			if (ctx.orderby_clause() != null) {

				QueryRendererBuilder extension = QueryRenderer.builder().append(TOKEN_COMMA).append(sortBy);

				builder.appendInline(extension);
			} else {
				builder.append(TOKEN_ORDER_BY);
				builder.append(sortBy);
			}
		}
	}

	@Override
	public QueryRendererBuilder visitSelect_item(JpqlParser.Select_itemContext ctx) {

		QueryRendererBuilder builder = super.visitSelect_item(ctx);

		if (ctx.result_variable() != null) {
			transformerSupport.registerAlias(builder.lastToken());
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitJoin(JpqlParser.JoinContext ctx) {

		QueryRendererBuilder builder = super.visitJoin(ctx);

		transformerSupport.registerAlias(builder.lastToken());

		return builder;
	}
}
