/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed HQL query.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class HqlQueryTransformer extends HqlQueryRenderer {

	@Nullable private Sort sort;
	private boolean countQuery;

	@Nullable private String countProjection;

	@Nullable private String alias = null;

	private List<JpaQueryParsingToken> projection = null;

	private boolean hasConstructorExpression = false;

	HqlQueryTransformer() {
		this(null, false, null);
	}

	HqlQueryTransformer(@Nullable Sort sort) {
		this(sort, false, null);
	}

	HqlQueryTransformer(boolean countQuery, @Nullable String countProjection) {
		this(null, countQuery, countProjection);
	}

	private HqlQueryTransformer(@Nullable Sort sort, boolean countQuery, @Nullable String countProjection) {

		this.sort = sort;
		this.countQuery = countQuery;
		this.countProjection = countProjection;
	}

	@Nullable
	public String getAlias() {
		return this.alias;
	}

	public List<JpaQueryParsingToken> getProjection() {
		return this.projection;
	}

	public boolean hasConstructorExpression() {
		return this.hasConstructorExpression;
	}

	/**
	 * Is this select clause a {@literal subquery}?
	 *
	 * @return boolean
	 */
	private static boolean isSubquery(ParserRuleContext ctx) {

		if (ctx instanceof HqlParser.SubqueryContext) {
			return true;
		} else if (ctx instanceof HqlParser.SelectStatementContext) {
			return false;
		} else {
			return isSubquery(ctx.getParent());
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitOrderedQuery(HqlParser.OrderedQueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.query() != null) {
			tokens.addAll(visit(ctx.query()));
		} else if (ctx.queryExpression() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.queryExpression()));
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		if (!countQuery && !isSubquery(ctx)) {

			if (ctx.queryOrder() != null) {
				tokens.addAll(visit(ctx.queryOrder()));
			}

			if (this.sort != null && this.sort.isSorted()) {

				if (ctx.queryOrder() != null) {

					NOSPACE(tokens);
					tokens.add(TOKEN_COMMA);
				} else {

					SPACE(tokens);
					tokens.add(TOKEN_ORDER_BY);
				}

				this.sort.forEach(order -> {

					JpaQueryParser.checkSortExpression(order);

					if (order.isIgnoreCase()) {
						tokens.add(TOKEN_LOWER_FUNC);
					}
					tokens.add(new JpaQueryParsingToken(() -> {

						if (order.getProperty().contains("(")) {
							return order.getProperty();
						}

						return this.alias + "." + order.getProperty();
					}, true));
					if (order.isIgnoreCase()) {
						NOSPACE(tokens);
						tokens.add(TOKEN_CLOSE_PAREN);
					}
					tokens.add(order.isDescending() ? TOKEN_DESC : TOKEN_ASC);
					tokens.add(TOKEN_COMMA);
				});
				CLIP(tokens);
			}
		} else {

			if (ctx.queryOrder() != null) {
				tokens.addAll(visit(ctx.queryOrder()));
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFromQuery(HqlParser.FromQueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (countQuery && !isSubquery(ctx) && ctx.selectClause() == null) {

			tokens.add(TOKEN_SELECT_COUNT);

			if (countProjection != null) {
				tokens.add(new JpaQueryParsingToken(countProjection));
			} else {
				tokens.add(new JpaQueryParsingToken(() -> this.alias, false));
			}

			tokens.add(TOKEN_CLOSE_PAREN);
		}

		if (ctx.fromClause() != null) {
			tokens.addAll(visit(ctx.fromClause()));
		}

		if (ctx.whereClause() != null) {
			tokens.addAll(visit(ctx.whereClause()));
		}

		if (ctx.groupByClause() != null) {
			tokens.addAll(visit(ctx.groupByClause()));
		}

		if (ctx.havingClause() != null) {
			tokens.addAll(visit(ctx.havingClause()));
		}

		if (ctx.selectClause() != null) {
			tokens.addAll(visit(ctx.selectClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitQueryOrder(HqlParser.QueryOrderContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (!countQuery) {
			tokens.addAll(visit(ctx.orderByClause()));
		}

		if (ctx.limitClause() != null) {
			SPACE(tokens);
			tokens.addAll(visit(ctx.limitClause()));
		}
		if (ctx.offsetClause() != null) {
			tokens.addAll(visit(ctx.offsetClause()));
		}
		if (ctx.fetchClause() != null) {
			tokens.addAll(visit(ctx.fetchClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFromRoot(HqlParser.FromRootContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.entityName() != null) {

			tokens.addAll(visit(ctx.entityName()));

			if (ctx.variable() != null) {
				tokens.addAll(visit(ctx.variable()));

				if (this.alias == null && !isSubquery(ctx)) {
					this.alias = tokens.get(tokens.size() - 1).getToken();
				}
			}
		} else if (ctx.subquery() != null) {

			if (ctx.LATERAL() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.LATERAL()));
			}
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			tokens.add(TOKEN_CLOSE_PAREN);

			if (ctx.variable() != null) {
				tokens.addAll(visit(ctx.variable()));

				if (this.alias == null && !isSubquery(ctx)) {
					this.alias = tokens.get(tokens.size() - 1).getToken();
				}
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitAlias(HqlParser.AliasContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}

		tokens.addAll(visit(ctx.identifier()));

		if (this.alias == null && !isSubquery(ctx)) {
			this.alias = tokens.get(tokens.size() - 1).getToken();
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelectClause(HqlParser.SelectClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SELECT()));

		if (countQuery && !isSubquery(ctx)) {
			tokens.add(TOKEN_COUNT_FUNC);

			if (countProjection != null) {
				tokens.add(new JpaQueryParsingToken(countProjection));
			}
		}

		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}

		List<JpaQueryParsingToken> selectionListTokens = visit(ctx.selectionList());

		if (countQuery && !isSubquery(ctx)) {

			if (countProjection == null) {

				if (ctx.DISTINCT() != null) {

					if (selectionListTokens.stream().anyMatch(hqlToken -> hqlToken.getToken().contains("new"))) {
						// constructor
						tokens.add(new JpaQueryParsingToken(() -> this.alias));
					} else {
						// keep all the select items to distinct against
						tokens.addAll(selectionListTokens);
					}
				} else {
					tokens.add(new JpaQueryParsingToken(() -> this.alias));
				}
			}

			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else {
			tokens.addAll(selectionListTokens);
		}

		if (projection == null && !isSubquery(ctx)) {
			this.projection = selectionListTokens;
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInstantiation(HqlParser.InstantiationContext ctx) {

		this.hasConstructorExpression = true;

		return super.visitInstantiation(ctx);
	}
}
