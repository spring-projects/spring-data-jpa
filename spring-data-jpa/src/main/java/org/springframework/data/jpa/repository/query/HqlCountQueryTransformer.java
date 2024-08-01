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

import org.springframework.data.jpa.repository.query.HqlParser.SelectClauseContext;
import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.data.jpa.repository.query.QueryTransformers.CountSelectionTokenStream;
import org.springframework.lang.Nullable;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed HQL query into a
 * {@code COUNT(…)} query.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.1
 */
@SuppressWarnings("ConstantValue")
class HqlCountQueryTransformer extends HqlQueryRenderer {

	private final @Nullable String countProjection;
	private final @Nullable String primaryFromAlias;

	HqlCountQueryTransformer(@Nullable String countProjection, @Nullable String primaryFromAlias) {
		this.countProjection = countProjection;
		this.primaryFromAlias = primaryFromAlias;
	}

	@Override
	public QueryRendererBuilder visitOrderedQuery(HqlParser.OrderedQueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.query() != null) {
			builder.append(visit(ctx.query()));
		} else if (ctx.queryExpression() != null) {

			QueryRendererBuilder nested = QueryRenderer.builder();
			nested.append(TOKEN_OPEN_PAREN);
			nested.appendInline(visit(ctx.queryExpression()));
			nested.append(TOKEN_CLOSE_PAREN);

			builder.appendExpression(nested);
		}

		if (ctx.queryOrder() != null) {
			builder.append(visit(ctx.queryOrder()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitFromQuery(HqlParser.FromQueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (!isSubquery(ctx) && ctx.selectClause() == null) {

			QueryRendererBuilder countBuilder = QueryRenderer.builder();
			countBuilder.append(TOKEN_SELECT_COUNT);

			if (countProjection != null) {
				countBuilder.append(QueryTokens.token(countProjection));
			} else {
				if (primaryFromAlias == null) {
					countBuilder.append(TOKEN_DOUBLE_UNDERSCORE);
				} else {
					countBuilder.append(QueryTokens.token(primaryFromAlias));
				}
			}

			countBuilder.append(TOKEN_CLOSE_PAREN);

			builder.appendExpression(countBuilder);
		}

		if (ctx.fromClause() != null) {
			builder.appendExpression(visit(ctx.fromClause()));
		}

		if (ctx.whereClause() != null) {
			builder.appendExpression(visit(ctx.whereClause()));
		}

		if (ctx.groupByClause() != null) {
			builder.appendExpression(visit(ctx.groupByClause()));
		}

		if (ctx.havingClause() != null) {
			builder.appendExpression(visit(ctx.havingClause()));
		}

		if (ctx.selectClause() != null) {
			builder.appendExpression(visit(ctx.selectClause()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitFromRoot(HqlParser.FromRootContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.entityName() != null) {

			builder.appendExpression(visit(ctx.entityName()));

			if (ctx.variable() != null) {
				builder.appendExpression(visit(ctx.variable()));

			} else {

				builder.append(TOKEN_AS);
				builder.append(TOKEN_DOUBLE_UNDERSCORE);
			}
		} else if (ctx.subquery() != null) {

			if (ctx.LATERAL() != null) {
				builder.append(QueryTokens.expression(ctx.LATERAL()));
			}

			QueryRendererBuilder nested = QueryRenderer.builder();

			nested.append(TOKEN_OPEN_PAREN);
			nested.appendInline(visit(ctx.subquery()));
			nested.append(TOKEN_CLOSE_PAREN);

			builder.appendExpression(nested);

			if (ctx.variable() != null) {
				builder.appendExpression(visit(ctx.variable()));
			}
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitJoin(HqlParser.JoinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.joinType()));
		builder.append(QueryTokens.expression(ctx.JOIN()));

		builder.appendExpression(visit(ctx.joinTarget()));

		if (ctx.joinRestriction() != null) {
			builder.appendExpression(visit(ctx.joinRestriction()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSelectClause(HqlParser.SelectClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(QueryTokens.expression(ctx.SELECT()));

		if (isSubquery(ctx)) {
			return visitSubQuerySelectClause(ctx, builder);
		}

		builder.append(TOKEN_COUNT_FUNC);
		boolean usesDistinct = ctx.DISTINCT() != null;
		QueryRendererBuilder nested = QueryRenderer.builder();
		if (countProjection == null) {
			if (usesDistinct) {

				nested.append(QueryTokens.expression(ctx.DISTINCT()));
				nested.append(getDistinctCountSelection(visit(ctx.selectionList())));
			} else {
				nested.append(QueryTokens.token(primaryFromAlias));
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

	@Override
	public QueryTokenStream visitSelection(HqlParser.SelectionContext ctx) {

		if (isSubquery(ctx)) {
			return super.visitSelection(ctx);
		}

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.selectExpression()));

		// do not append variables to skip AS field aliasing

		return builder;
	}

	@Override
	public QueryRendererBuilder visitQueryOrder(HqlParser.QueryOrderContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.limitClause() != null) {
			builder.appendExpression(visit(ctx.limitClause()));
		}

		if (ctx.offsetClause() != null) {
			builder.appendExpression(visit(ctx.offsetClause()));
		}

		if (ctx.fetchClause() != null) {
			builder.appendExpression(visit(ctx.fetchClause()));
		}

		return builder;
	}

	private QueryRendererBuilder visitSubQuerySelectClause(SelectClauseContext ctx, QueryRendererBuilder builder) {
		if (ctx.DISTINCT() != null) {
			builder.append(QueryTokens.expression(ctx.DISTINCT()));
		}

		builder.append(visit(ctx.selectionList()));
		return builder;
	}

	private QueryRendererBuilder getDistinctCountSelection(QueryTokenStream selectionListbuilder) {

		QueryRendererBuilder nested = new QueryRendererBuilder();
		CountSelectionTokenStream countSelection = CountSelectionTokenStream.create(selectionListbuilder);

		if (countSelection.requiresPrimaryAlias()) {
			// constructor
			nested.append(QueryTokens.token(primaryFromAlias));
		} else {
			// keep all the select items to distinct against
			nested.append(selectionListbuilder);
		}
		return nested;
	}

}
