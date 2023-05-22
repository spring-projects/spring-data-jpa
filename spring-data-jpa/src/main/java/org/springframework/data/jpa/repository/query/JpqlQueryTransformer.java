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
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed JPQL query.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class JpqlQueryTransformer extends JpqlQueryRenderer {

	// TODO: Separate input from result parameters, encapsulation...
	private final Sort sort;
	private final boolean countQuery;

	private final @Nullable String countProjection;

	private @Nullable String primaryFromAlias = null;

	private List<JpaQueryParsingToken> projection = Collections.emptyList();
	private boolean projectionProcessed;

	private boolean hasConstructorExpression = false;

	private JpaQueryTransformerSupport transformerSupport;

	JpqlQueryTransformer() {
		this(Sort.unsorted(), false, null);
	}

	JpqlQueryTransformer(Sort sort) {
		this(sort, false, null);
	}

	JpqlQueryTransformer(boolean countQuery, @Nullable String countProjection) {
		this(Sort.unsorted(), countQuery, countProjection);
	}

	private JpqlQueryTransformer(Sort sort, boolean countQuery, @Nullable String countProjection) {

		Assert.notNull(sort, "Sort must not be null");

		this.sort = sort;
		this.countQuery = countQuery;
		this.countProjection = countProjection;
		this.transformerSupport = new JpaQueryTransformerSupport();
	}

	@Nullable
	public String getAlias() {
		return this.primaryFromAlias;
	}

	public List<JpaQueryParsingToken> getProjection() {
		return this.projection;
	}

	public boolean hasConstructorExpression() {
		return this.hasConstructorExpression;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_statement(JpqlParser.Select_statementContext ctx) {

		List<JpaQueryParsingToken> tokens = newArrayList();

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

			if (sort.isSorted()) {

				if (ctx.orderby_clause() != null) {

					NOSPACE(tokens);
					tokens.add(TOKEN_COMMA);
				} else {

					SPACE(tokens);
					tokens.add(TOKEN_ORDER_BY);
				}

				tokens.addAll(transformerSupport.generateOrderByArguments(primaryFromAlias, sort));
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_clause(JpqlParser.Select_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = newArrayList();

		tokens.add(new JpaQueryParsingToken(ctx.SELECT()));

		if (countQuery) {
			tokens.add(TOKEN_COUNT_FUNC);
		}

		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}

		List<JpaQueryParsingToken> selectItemTokens = newArrayList();

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
						tokens.add(new JpaQueryParsingToken(() -> primaryFromAlias));
					} else {
						// keep all the select items to distinct against
						tokens.addAll(selectItemTokens);
					}
				} else {
					tokens.add(new JpaQueryParsingToken(() -> primaryFromAlias));
				}
			}

			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else {
			tokens.addAll(selectItemTokens);
		}

		if (!projectionProcessed) {
			projection = selectItemTokens;
			projectionProcessed = true;
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_item(JpqlParser.Select_itemContext ctx) {

		List<JpaQueryParsingToken> tokens = super.visitSelect_item(ctx);

		if (ctx.result_variable() != null) {
			transformerSupport.registerAlias(tokens.get(tokens.size() - 1).getToken());
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitRange_variable_declaration(JpqlParser.Range_variable_declarationContext ctx) {

		List<JpaQueryParsingToken> tokens = newArrayList();

		tokens.addAll(visit(ctx.entity_name()));

		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}

		tokens.addAll(visit(ctx.identification_variable()));

		if (primaryFromAlias == null) {
			primaryFromAlias = tokens.get(tokens.size() - 1).getToken();
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin(JpqlParser.JoinContext ctx) {

		List<JpaQueryParsingToken> tokens = super.visitJoin(ctx);

		transformerSupport.registerAlias(tokens.get(tokens.size() - 1).getToken());

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitConstructor_expression(JpqlParser.Constructor_expressionContext ctx) {

		hasConstructorExpression = true;

		return super.visitConstructor_expression(ctx);
	}

	private static <T> ArrayList<T> newArrayList() {
		return new ArrayList<>();
	}
}
