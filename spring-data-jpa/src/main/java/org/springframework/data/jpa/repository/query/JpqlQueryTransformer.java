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

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed JPQL query.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class JpqlQueryTransformer extends JpqlQueryRenderer {

	@Nullable private Sort sort;
	private boolean countQuery;

	@Nullable private String countProjection;

	@Nullable private String alias = null;

	private List<JpaQueryParsingToken> projection = null;

	private boolean hasConstructorExpression = false;

	JpqlQueryTransformer() {
		this(null, false, null);
	}

	JpqlQueryTransformer(@Nullable Sort sort) {
		this(sort, false, null);
	}

	JpqlQueryTransformer(boolean countQuery, @Nullable String countProjection) {
		this(null, countQuery, countProjection);
	}

	private JpqlQueryTransformer(@Nullable Sort sort, boolean countQuery, @Nullable String countProjection) {

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

	@Override
	public List<JpaQueryParsingToken> visitSelect_statement(JpqlParser.Select_statementContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.select_clause()));
		tokens.addAll(visit(ctx.from_clause()));

		if (ctx.where_clause() != null) {
			tokens.addAll(visit(ctx.where_clause()));
		}

		if (ctx.groupby_clause() != null) {
			tokens.addAll(visit(ctx.groupby_clause()));
		}

		if (ctx.having_clause() != null) {
			tokens.addAll(visit(ctx.having_clause()));
		}

		if (!countQuery) {

			if (ctx.orderby_clause() != null) {
				tokens.addAll(visit(ctx.orderby_clause()));
			}

			if (this.sort != null && this.sort.isSorted()) {

				if (ctx.orderby_clause() != null) {

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
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_clause(JpqlParser.Select_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SELECT()));

		if (countQuery) {
			tokens.add(TOKEN_COUNT_FUNC);
		}

		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}

		List<JpaQueryParsingToken> selectItemTokens = new ArrayList<>();

		ctx.select_item().forEach(selectItemContext -> {
			selectItemTokens.addAll(visit(selectItemContext));
			NOSPACE(selectItemTokens);
			selectItemTokens.add(TOKEN_COMMA);
		});
		CLIP(selectItemTokens);
		SPACE(selectItemTokens);

		if (countQuery) {

			if (countProjection != null) {
				tokens.add(new JpaQueryParsingToken(countProjection));
			} else {

				if (ctx.DISTINCT() != null) {

					if (selectItemTokens.stream().anyMatch(jpqlToken -> jpqlToken.getToken().contains("new"))) {
						// constructor
						tokens.add(new JpaQueryParsingToken(() -> this.alias));
					} else {
						// keep all the select items to distinct against
						tokens.addAll(selectItemTokens);
					}
				} else {
					tokens.add(new JpaQueryParsingToken(() -> this.alias));
				}
			}

			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else {
			tokens.addAll(selectItemTokens);
		}

		if (projection == null) {
			this.projection = selectItemTokens;
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitRange_variable_declaration(JpqlParser.Range_variable_declarationContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.entity_name()));

		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}

		tokens.addAll(visit(ctx.identification_variable()));

		if (this.alias == null) {
			this.alias = tokens.get(tokens.size() - 1).getToken();
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitConstructor_expression(JpqlParser.Constructor_expressionContext ctx) {

		this.hasConstructorExpression = true;

		return super.visitConstructor_expression(ctx);
	}
}
