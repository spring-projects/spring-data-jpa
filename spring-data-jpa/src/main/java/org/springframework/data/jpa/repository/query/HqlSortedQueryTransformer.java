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

import static org.springframework.data.jpa.repository.query.QueryTokens.*;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed HQL query.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @since 3.1
 */
@SuppressWarnings("ConstantValue")
class HqlSortedQueryTransformer extends HqlQueryRenderer {

	private final JpaQueryTransformerSupport transformerSupport = new JpaQueryTransformerSupport();
	private final Sort sort;
	private final @Nullable String primaryFromAlias;

	HqlSortedQueryTransformer(Sort sort, @Nullable String primaryFromAlias) {

		Assert.notNull(sort, "Sort must not be null");

		this.sort = sort;
		this.primaryFromAlias = primaryFromAlias;
	}

	@Override
	public QueryTokenStream visitQueryExpression(HqlParser.QueryExpressionContext ctx) {

		if (ObjectUtils.isEmpty(ctx.setOperator())) {
			return super.visitQueryExpression(ctx);
		}

		QueryRendererBuilder builder = QueryRenderer.builder();
		if (ctx.withClause() != null) {
			builder.appendExpression(visit(ctx.withClause()));
		}

		List<HqlParser.OrderedQueryContext> orderedQueries = ctx.orderedQuery();
		for (int i = 0; i < orderedQueries.size(); i++) {

			if (i != 0) {
				builder.append(visit(ctx.setOperator(i - 1)));
			}

			if (i == orderedQueries.size() - 1) {
				builder.append(visitOrderedQuery(ctx.orderedQuery(i), this.sort));
			} else {
				builder.append(visitOrderedQuery(ctx.orderedQuery(i), Sort.unsorted()));
			}
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitOrderedQuery(HqlParser.OrderedQueryContext ctx) {
		return visitOrderedQuery(ctx, this.sort);
	}

	@Override
	public QueryTokenStream visitJoinPath(HqlParser.JoinPathContext ctx) {

		QueryTokenStream tokens = super.visitJoinPath(ctx);

		if (ctx.variable() != null && !isSubquery(ctx)) {
			transformerSupport.registerAlias(tokens.getLast());
		}

		return tokens;
	}

	@Override
	public QueryTokenStream visitJoinSubquery(HqlParser.JoinSubqueryContext ctx) {

		QueryTokenStream tokens = super.visitJoinSubquery(ctx);

		if (ctx.variable() != null && !tokens.isEmpty() && !isSubquery(ctx)) {
			transformerSupport.registerAlias(tokens.getLast());
		}

		return tokens;
	}

	@Override
	public QueryTokenStream visitVariable(HqlParser.VariableContext ctx) {

		QueryTokenStream tokens = super.visitVariable(ctx);

		if (ctx.identifier() != null && !tokens.isEmpty() && !isSubquery(ctx)) {
			transformerSupport.registerAlias(tokens.getLast());
		}

		return tokens;
	}

	private QueryRendererBuilder visitOrderedQuery(HqlParser.OrderedQueryContext ctx, Sort sort) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.query() != null) {
			builder.append(visit(ctx.query()));
		} else if (ctx.queryExpression() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.queryExpression()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		if (!isSubquery(ctx)) {

			if (ctx.queryOrder() != null) {
				QueryTokenStream existingOrder = visit(ctx.queryOrder());
				if (sort.isSorted()) {
					builder.appendInline(existingOrder);
				} else {
					builder.append(existingOrder);
				}
			}

			if (sort.isSorted()) {

				List<QueryToken> sortBy = transformerSupport.orderBy(primaryFromAlias, sort);

				if (ctx.queryOrder() != null) {

					QueryRendererBuilder extension = QueryRenderer.builder().append(TOKEN_COMMA).append(sortBy);

					builder.appendInline(extension);
				} else {
					builder.append(TOKEN_ORDER_BY);
					builder.append(sortBy);
				}
			}
		} else {

			if (ctx.queryOrder() != null) {
				builder.append(visit(ctx.queryOrder()));
			}
		}

		return builder;
	}

}
