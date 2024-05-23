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

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
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
				countBuilder.append(new JpaQueryParsingToken(countProjection));
			} else {
				if (primaryFromAlias == null) {
					countBuilder.append(TOKEN_DOUBLE_UNDERSCORE);
				} else {
					countBuilder.append(JpaQueryParsingToken.token(primaryFromAlias));
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
				builder.append(JpaQueryParsingToken.expression(ctx.LATERAL()));
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
		builder.append(JpaQueryParsingToken.expression(ctx.JOIN()));

		builder.appendExpression(visit(ctx.joinTarget()));

		if (ctx.joinRestriction() != null) {
			builder.appendExpression(visit(ctx.joinRestriction()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSelectClause(HqlParser.SelectClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(JpaQueryParsingToken.expression(ctx.SELECT()));

		QueryRendererBuilder selectionListbuilder = visit(ctx.selectionList());

		if (!isSubquery(ctx)) {

			builder.append(TOKEN_COUNT_FUNC);

			if (countProjection != null) {
				builder.append(JpaQueryParsingToken.token(countProjection));
			}

			QueryRendererBuilder nested = QueryRenderer.builder();

			if (ctx.DISTINCT() != null) {
				nested.append(JpaQueryParsingToken.expression(ctx.DISTINCT()));
			}

			if (countProjection == null) {

				if (ctx.DISTINCT() != null) {

					List<JpaQueryParsingToken> countSelection = QueryTransformers
							.filterCountSelection(selectionListbuilder.build().stream().toList());

					if (countSelection.stream().anyMatch(hqlToken -> hqlToken.getToken().contains("new"))) {
						// constructor
						nested.append(new JpaQueryParsingToken(primaryFromAlias));
					} else {
						// keep all the select items to distinct against
						nested.append(countSelection);
					}
				} else {
					nested.append(new JpaQueryParsingToken(primaryFromAlias));
				}
			}

			builder.appendInline(nested);
			builder.append(TOKEN_CLOSE_PAREN);

		} else {

			if (ctx.DISTINCT() != null) {
				builder.append(JpaQueryParsingToken.expression(ctx.DISTINCT()));
			}

			builder.append(selectionListbuilder);
		}

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

}
