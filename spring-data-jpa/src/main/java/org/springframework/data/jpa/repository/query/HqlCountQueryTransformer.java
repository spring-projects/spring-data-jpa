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

import static org.springframework.data.jpa.repository.query.QueryTokens.*;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.repository.query.HqlParser.InstantiationArgumentContext;
import org.springframework.data.jpa.repository.query.HqlParser.InstantiationContext;
import org.springframework.data.jpa.repository.query.HqlParser.SelectionContext;
import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;

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
class HqlCountQueryTransformer extends HqlQueryRenderer
		implements CountSelection.Grammar<SelectionContext, InstantiationContext, InstantiationArgumentContext> {

	private final @Nullable String primaryFromAlias;
	private final CountSelection<SelectionContext, InstantiationContext, InstantiationArgumentContext> countSelection;

	HqlCountQueryTransformer(@Nullable String countProjection, HibernateQueryInformation queryInformation) {

		this.primaryFromAlias = queryInformation.getAlias();

		// with CTE primary alias fails with hibernate (WITH entities AS (…) SELECT count(c) FROM entities c)
		boolean primaryAliasCountable = !queryInformation.hasCte() && !queryInformation.hasFromFunction();
		this.countSelection = new CountSelection<>(countProjection, primaryFromAlias, primaryAliasCountable, true, this);
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
			builder.appendExpression(countSelection.render());
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

		if (isSubquery(ctx)) {
			return super.visitSelectClause(ctx);
		}

		return countSelection.render(QueryTokens.expression(ctx.SELECT()), QueryTokens.expressionOrNull(ctx.DISTINCT()),
				ctx.selectionList().selection());
	}

	@Override
	public QueryTokenStream visitSelection(HqlParser.SelectionContext ctx) {

		if (isSubquery(ctx)) {
			return super.visitSelection(ctx);
		}

		return visit(ctx.selectExpression()); // skip AS field aliasing
	}

	@Override
	public QueryTokenStream visitInstantiation(HqlParser.InstantiationContext ctx) {

		if (isSubquery(ctx)) {
			return super.visitInstantiation(ctx);
		}

		return countSelection.renderConstructor(ctx);
	}

	@Override
	public @Nullable InstantiationContext getConstructorExpression(SelectionContext selectItem) {
		return selectItem.selectExpression().instantiation();
	}

	@Override
	public List<InstantiationArgumentContext> getConstructorArguments(InstantiationContext constructorExpression) {
		return constructorExpression.instantiationArguments().instantiationArgument();
	}

	@Override
	public boolean isPath(InstantiationArgumentContext argument) {

		if (argument.expressionOrPredicate() == null
				|| !(argument.expressionOrPredicate().predicate() instanceof HqlParser.ExpressionPredicateContext predicate)) {
			return false;
		}

		if (!(predicate.expression() instanceof HqlParser.PlainPrimaryExpressionContext expression)) {
			return false;
		}

		return expression.primaryExpression() instanceof HqlParser.GeneralPathExpressionContext;
	}

}
