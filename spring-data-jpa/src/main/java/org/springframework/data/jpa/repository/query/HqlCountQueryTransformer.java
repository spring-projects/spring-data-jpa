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

import org.springframework.data.jpa.repository.query.HqlParser.SelectClauseContext;
import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.data.jpa.repository.query.QueryTransformers.CountSelectionTokenStream;
import org.springframework.util.StringUtils;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed HQL query into a
 * {@code COUNT(…)} query.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Oscar Fanchin
 * @since 3.1
 */
@SuppressWarnings("ConstantValue")
class HqlCountQueryTransformer extends HqlQueryRenderer {

	private final @Nullable String countProjection;
	private final @Nullable String primaryFromAlias;
	private final boolean containsCTE;
	private final boolean containsFromFunction;

	HqlCountQueryTransformer(@Nullable String countProjection, HibernateQueryInformation queryInformation) {
		this.countProjection = countProjection;
		this.primaryFromAlias = queryInformation.getAlias();
		this.containsCTE = queryInformation.hasCte();
		this.containsFromFunction = queryInformation.hasFromFunction();
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
			if (primaryFromAlias == null) {
				builder.append(TOKEN_AS);
				builder.append(TOKEN_DOUBLE_UNDERSCORE);
			}
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
	public QueryRendererBuilder visitJoin(HqlParser.JoinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_SPACE);
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

				// with CTE primary alias fails with hibernate (WITH entities AS (…) SELECT count(c) FROM entities c)
				if (containsCTE || containsFromFunction) {
					nested.append(QueryTokens.token("*"));
				} else {

					if (StringUtils.hasText(primaryFromAlias)) {
						nested.append(QueryTokens.token(primaryFromAlias));
					} else {
						nested.append(QueryTokens.token("*"));
					}
				}
			}
		} else {
			if (usesDistinct) {
				nested.append(QueryTokens.expression(ctx.DISTINCT()));
			}
			nested.append(QueryTokens.token(countProjection));
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

			if (primaryFromAlias != null) {
				// constructor
				nested.append(QueryTokens.token(primaryFromAlias));
			} else {
				nested.append(countSelection.withoutConstructorExpression());
			}
		} else {
			// keep all the select items to distinct against
			nested.append(selectionListbuilder);
		}
		return nested;
	}

}
