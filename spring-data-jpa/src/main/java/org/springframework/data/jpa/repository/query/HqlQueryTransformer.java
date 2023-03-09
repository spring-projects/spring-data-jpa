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

import static org.springframework.data.jpa.repository.query.QueryParsingToken.*;

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
class HqlQueryTransformer extends HqlBaseVisitor<List<QueryParsingToken>> {

	@Nullable private Sort sort;
	private boolean countQuery;

	@Nullable private String countProjection;

	@Nullable private String alias = null;

	private List<QueryParsingToken> projection = null;

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

	public List<QueryParsingToken> getProjection() {
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
	public List<QueryParsingToken> visitStart(HqlParser.StartContext ctx) {
		return visit(ctx.ql_statement());
	}

	@Override
	public List<QueryParsingToken> visitQl_statement(HqlParser.Ql_statementContext ctx) {

		if (ctx.selectStatement() != null) {
			return visit(ctx.selectStatement());
		} else if (ctx.updateStatement() != null) {
			return visit(ctx.updateStatement());
		} else if (ctx.deleteStatement() != null) {
			return visit(ctx.deleteStatement());
		} else if (ctx.insertStatement() != null) {
			return visit(ctx.insertStatement());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitSelectStatement(HqlParser.SelectStatementContext ctx) {
		return visit(ctx.queryExpression());
	}

	@Override
	public List<QueryParsingToken> visitQueryExpression(HqlParser.QueryExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.orderedQuery(0)));

		for (int i = 1; i < ctx.orderedQuery().size(); i++) {

			tokens.addAll(visit(ctx.setOperator(i - 1)));
			tokens.addAll(visit(ctx.orderedQuery(i)));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitOrderedQuery(HqlParser.OrderedQueryContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.query() != null) {
			tokens.addAll(visit(ctx.query()));
		} else if (ctx.queryExpression() != null) {

			tokens.add(new QueryParsingToken("(", false));
			tokens.addAll(visit(ctx.queryExpression()));
			tokens.add(new QueryParsingToken(")"));
		}

		if (!countQuery && !isSubquery(ctx)) {

			if (ctx.queryOrder() != null) {
				tokens.addAll(visit(ctx.queryOrder()));
			}

			if (this.sort != null && this.sort.isSorted()) {

				if (ctx.queryOrder() != null) {

					NOSPACE(tokens);
					tokens.add(new QueryParsingToken(","));
				} else {

					SPACE(tokens);
					tokens.add(new QueryParsingToken("order by"));
				}

				this.sort.forEach(order -> {

					QueryParser.checkSortExpression(order);

					if (order.isIgnoreCase()) {
						tokens.add(new QueryParsingToken("lower(", false));
					}
					tokens.add(new QueryParsingToken(() -> {

						if (order.getProperty().contains("(")) {
							return order.getProperty();
						}

						return this.alias + "." + order.getProperty();
					}, true));
					if (order.isIgnoreCase()) {
						NOSPACE(tokens);
						tokens.add(new QueryParsingToken(")", true));
					}
					tokens.add(new QueryParsingToken(order.isDescending() ? "desc" : "asc", false));
					tokens.add(new QueryParsingToken(","));
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
	public List<QueryParsingToken> visitSelectQuery(HqlParser.SelectQueryContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.selectClause() != null) {
			tokens.addAll(visit(ctx.selectClause()));
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

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFromQuery(HqlParser.FromQueryContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (countQuery && !isSubquery(ctx) && ctx.selectClause() == null) {

			tokens.add(new QueryParsingToken("select count(", false));

			if (countProjection != null) {
				tokens.add(new QueryParsingToken(countProjection));
			} else {
				tokens.add(new QueryParsingToken(() -> this.alias, false));
			}

			tokens.add(new QueryParsingToken(")", true));
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
	public List<QueryParsingToken> visitQueryOrder(HqlParser.QueryOrderContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

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
	public List<QueryParsingToken> visitFromClause(HqlParser.FromClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.FROM().getText()));

		ctx.entityWithJoins().forEach(entityWithJoinsContext -> {
			tokens.addAll(visit(entityWithJoinsContext));
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitEntityWithJoins(HqlParser.EntityWithJoinsContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.fromRoot()));

		ctx.joinSpecifier().forEach(joinSpecifierContext -> {
			tokens.addAll(visit(joinSpecifierContext));
		});

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoinSpecifier(HqlParser.JoinSpecifierContext ctx) {

		if (ctx.join() != null) {
			return visit(ctx.join());
		} else if (ctx.crossJoin() != null) {
			return visit(ctx.crossJoin());
		} else if (ctx.jpaCollectionJoin() != null) {
			return visit(ctx.jpaCollectionJoin());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitFromRoot(HqlParser.FromRootContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

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
				tokens.add(new QueryParsingToken(ctx.LATERAL().getText()));
			}
			tokens.add(new QueryParsingToken("(", false));
			tokens.addAll(visit(ctx.subquery()));
			tokens.add(new QueryParsingToken(")"));

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
	public List<QueryParsingToken> visitJoin(HqlParser.JoinContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.joinType()));
		tokens.add(new QueryParsingToken(ctx.JOIN().getText()));

		if (ctx.FETCH() != null) {
			tokens.add(new QueryParsingToken(ctx.FETCH().getText()));
		}

		tokens.addAll(visit(ctx.joinTarget()));

		if (ctx.joinRestriction() != null) {
			tokens.addAll(visit(ctx.joinRestriction()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoinPath(HqlParser.JoinPathContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.path()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoinSubquery(HqlParser.JoinSubqueryContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LATERAL() != null) {
			tokens.add(new QueryParsingToken(ctx.LATERAL().getText()));
		}

		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.subquery()));
		tokens.add(new QueryParsingToken(")"));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitUpdateStatement(HqlParser.UpdateStatementContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.UPDATE().getText()));

		if (ctx.VERSIONED() != null) {
			tokens.add(new QueryParsingToken(ctx.VERSIONED().getText()));
		}

		tokens.addAll(visit(ctx.targetEntity()));
		tokens.addAll(visit(ctx.setClause()));

		if (ctx.whereClause() != null) {
			tokens.addAll(visit(ctx.whereClause()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitTargetEntity(HqlParser.TargetEntityContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.entityName()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSetClause(HqlParser.SetClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.SET().getText()));

		ctx.assignment().forEach(assignmentContext -> {
			tokens.addAll(visit(assignmentContext));
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitAssignment(HqlParser.AssignmentContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.simplePath()));
		tokens.add(new QueryParsingToken("="));
		tokens.addAll(visit(ctx.expressionOrPredicate()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitDeleteStatement(HqlParser.DeleteStatementContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.DELETE().getText()));

		if (ctx.FROM() != null) {
			tokens.add(new QueryParsingToken(ctx.FROM().getText()));
		}

		tokens.addAll(visit(ctx.targetEntity()));

		if (ctx.whereClause() != null) {
			tokens.addAll(visit(ctx.whereClause()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitInsertStatement(HqlParser.InsertStatementContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.INSERT().getText()));

		if (ctx.INTO() != null) {
			tokens.add(new QueryParsingToken(ctx.INTO().getText()));
		}

		tokens.addAll(visit(ctx.targetEntity()));
		tokens.addAll(visit(ctx.targetFields()));

		if (ctx.queryExpression() != null) {
			tokens.addAll(visit(ctx.queryExpression()));
		} else if (ctx.valuesList() != null) {
			tokens.addAll(visit(ctx.valuesList()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitTargetFields(HqlParser.TargetFieldsContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken("(", false));

		ctx.simplePath().forEach(simplePathContext -> {
			tokens.addAll(visit(simplePathContext));
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitValuesList(HqlParser.ValuesListContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.VALUES().getText()));

		ctx.values().forEach(valuesContext -> {
			tokens.addAll(visit(valuesContext));
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitValues(HqlParser.ValuesContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken("(", false));

		ctx.expression().forEach(expressionContext -> {
			tokens.addAll(visit(expressionContext));
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitProjectedItem(HqlParser.ProjectedItemContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.expression() != null) {
			tokens.addAll(visit(ctx.expression()));
		} else if (ctx.instantiation() != null) {
			tokens.addAll(visit(ctx.instantiation()));
		}

		if (ctx.alias() != null) {
			tokens.addAll(visit(ctx.alias()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitInstantiation(HqlParser.InstantiationContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		this.hasConstructorExpression = true;

		tokens.add(new QueryParsingToken(ctx.NEW().getText()));
		tokens.addAll(visit(ctx.instantiationTarget()));
		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.instantiationArguments()));
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitAlias(HqlParser.AliasContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.AS() != null) {
			tokens.add(new QueryParsingToken(ctx.AS().getText()));
		}

		tokens.addAll(visit(ctx.identifier()));

		if (this.alias == null && !isSubquery(ctx)) {
			this.alias = tokens.get(tokens.size() - 1).getToken();
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGroupedItem(HqlParser.GroupedItemContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.INTEGER_LITERAL().getText()));
		} else if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitSortedItem(HqlParser.SortedItemContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.sortExpression()));

		if (ctx.sortDirection() != null) {
			tokens.addAll(visit(ctx.sortDirection()));
		}

		if (ctx.nullsPrecedence() != null) {
			tokens.addAll(visit(ctx.nullsPrecedence()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSortExpression(HqlParser.SortExpressionContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.INTEGER_LITERAL().getText()));
		} else if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitSortDirection(HqlParser.SortDirectionContext ctx) {

		if (ctx.ASC() != null) {
			return List.of(new QueryParsingToken(ctx.ASC().getText()));
		} else if (ctx.DESC() != null) {
			return List.of(new QueryParsingToken(ctx.DESC().getText()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitNullsPrecedence(HqlParser.NullsPrecedenceContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.NULLS().getText()));

		if (ctx.FIRST() != null) {
			tokens.add(new QueryParsingToken(ctx.FIRST().getText()));
		} else if (ctx.LAST() != null) {
			tokens.add(new QueryParsingToken(ctx.LAST().getText()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitLimitClause(HqlParser.LimitClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.LIMIT().getText()));
		tokens.addAll(visit(ctx.parameterOrIntegerLiteral()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitOffsetClause(HqlParser.OffsetClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.OFFSET().getText()));
		tokens.addAll(visit(ctx.parameterOrIntegerLiteral()));

		if (ctx.ROW() != null) {
			tokens.add(new QueryParsingToken(ctx.ROW().getText()));
		} else if (ctx.ROWS() != null) {
			tokens.add(new QueryParsingToken(ctx.ROWS().getText()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFetchClause(HqlParser.FetchClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.FETCH().getText()));

		if (ctx.FIRST() != null) {
			tokens.add(new QueryParsingToken(ctx.FIRST().getText()));
		} else if (ctx.NEXT() != null) {
			tokens.add(new QueryParsingToken(ctx.NEXT().getText()));
		}

		if (ctx.parameterOrIntegerLiteral() != null) {
			tokens.addAll(visit(ctx.parameterOrIntegerLiteral()));
		} else if (ctx.parameterOrNumberLiteral() != null) {

			tokens.addAll(visit(ctx.parameterOrNumberLiteral()));
			tokens.add(new QueryParsingToken("%"));
		}

		if (ctx.ROW() != null) {
			tokens.add(new QueryParsingToken(ctx.ROW().getText()));
		} else if (ctx.ROWS() != null) {
			tokens.add(new QueryParsingToken(ctx.ROWS().getText()));
		}

		if (ctx.ONLY() != null) {
			tokens.add(new QueryParsingToken(ctx.ONLY().getText()));
		} else if (ctx.WITH() != null) {

			tokens.add(new QueryParsingToken(ctx.WITH().getText()));
			tokens.add(new QueryParsingToken(ctx.TIES().getText()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSubquery(HqlParser.SubqueryContext ctx) {
		return visit(ctx.queryExpression());
	}

	@Override
	public List<QueryParsingToken> visitSelectClause(HqlParser.SelectClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.SELECT().getText()));

		if (countQuery && !isSubquery(ctx)) {
			tokens.add(new QueryParsingToken("count(", false));

			if (countProjection != null) {
				tokens.add(new QueryParsingToken(countProjection));
			}
		}

		if (ctx.DISTINCT() != null) {
			tokens.add(new QueryParsingToken(ctx.DISTINCT().getText()));
		}

		List<QueryParsingToken> selectionListTokens = visit(ctx.selectionList());

		if (countQuery && !isSubquery(ctx)) {

			if (countProjection == null) {

				if (ctx.DISTINCT() != null) {

					if (selectionListTokens.stream().anyMatch(hqlToken -> hqlToken.getToken().contains("new"))) {
						// constructor
						tokens.add(new QueryParsingToken(() -> this.alias));
					} else {
						// keep all the select items to distinct against
						tokens.addAll(selectionListTokens);
					}
				} else {
					tokens.add(new QueryParsingToken(() -> this.alias));
				}
			}

			NOSPACE(tokens);
			tokens.add(new QueryParsingToken(")"));
		} else {
			tokens.addAll(selectionListTokens);
		}

		if (projection == null && !isSubquery(ctx)) {
			this.projection = selectionListTokens;
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSelectionList(HqlParser.SelectionListContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		ctx.selection().forEach(selectionContext -> {
			tokens.addAll(visit(selectionContext));
			NOSPACE(tokens);
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSelection(HqlParser.SelectionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.selectExpression()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSelectExpression(HqlParser.SelectExpressionContext ctx) {

		if (ctx.instantiation() != null) {
			return visit(ctx.instantiation());
		} else if (ctx.mapEntrySelection() != null) {
			return visit(ctx.mapEntrySelection());
		} else if (ctx.jpaSelectObjectSyntax() != null) {
			return visit(ctx.jpaSelectObjectSyntax());
		} else if (ctx.expressionOrPredicate() != null) {
			return visit(ctx.expressionOrPredicate());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitMapEntrySelection(HqlParser.MapEntrySelectionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.ENTRY().getText()));
		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.path()));
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJpaSelectObjectSyntax(HqlParser.JpaSelectObjectSyntaxContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.OBJECT().getText()));
		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.identifier()));
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitWhereClause(HqlParser.WhereClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.WHERE().getText()));

		ctx.predicate().forEach(predicateContext -> {
			tokens.addAll(visit(predicateContext));
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoinType(HqlParser.JoinTypeContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.INNER() != null) {
			tokens.add(new QueryParsingToken(ctx.INNER().getText()));
		}
		if (ctx.LEFT() != null) {
			tokens.add(new QueryParsingToken(ctx.LEFT().getText()));
		}
		if (ctx.RIGHT() != null) {
			tokens.add(new QueryParsingToken(ctx.RIGHT().getText()));
		}
		if (ctx.FULL() != null) {
			tokens.add(new QueryParsingToken(ctx.FULL().getText()));
		}
		if (ctx.OUTER() != null) {
			tokens.add(new QueryParsingToken(ctx.OUTER().getText()));
		}
		if (ctx.CROSS() != null) {
			tokens.add(new QueryParsingToken(ctx.CROSS().getText()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCrossJoin(HqlParser.CrossJoinContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.CROSS().getText()));
		tokens.add(new QueryParsingToken(ctx.JOIN().getText()));
		tokens.addAll(visit(ctx.entityName()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJoinRestriction(HqlParser.JoinRestrictionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.ON() != null) {
			tokens.add(new QueryParsingToken(ctx.ON().getText()));
		} else if (ctx.WITH() != null) {
			tokens.add(new QueryParsingToken(ctx.WITH().getText()));
		}

		tokens.addAll(visit(ctx.predicate()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(","));
		tokens.add(new QueryParsingToken(ctx.IN().getText()));
		tokens.addAll(visit(ctx.path()));
		tokens.add(new QueryParsingToken(")"));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGroupByClause(HqlParser.GroupByClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.GROUP().getText()));
		tokens.add(new QueryParsingToken(ctx.BY().getText()));

		ctx.groupedItem().forEach(groupedItemContext -> {
			tokens.addAll(visit(groupedItemContext));
			NOSPACE(tokens);
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitOrderByClause(HqlParser.OrderByClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.ORDER().getText()));
		tokens.add(new QueryParsingToken(ctx.BY().getText()));

		ctx.projectedItem().forEach(projectedItemContext -> {
			tokens.addAll(visit(projectedItemContext));
			NOSPACE(tokens);
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitHavingClause(HqlParser.HavingClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.HAVING().getText()));

		ctx.predicate().forEach(predicateContext -> {
			tokens.addAll(visit(predicateContext));
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSetOperator(HqlParser.SetOperatorContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.UNION() != null) {
			tokens.add(new QueryParsingToken(ctx.UNION().getText()));
		} else if (ctx.INTERSECT() != null) {
			tokens.add(new QueryParsingToken(ctx.INTERSECT().getText()));
		} else if (ctx.EXCEPT() != null) {
			tokens.add(new QueryParsingToken(ctx.EXCEPT().getText()));
		}

		if (ctx.ALL() != null) {
			tokens.add(new QueryParsingToken(ctx.ALL().getText()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitLiteral(HqlParser.LiteralContext ctx) {

		if (ctx.NULL() != null) {
			return List.of(new QueryParsingToken(ctx.NULL().getText()));
		} else if (ctx.booleanLiteral() != null) {
			return visit(ctx.booleanLiteral());
		} else if (ctx.stringLiteral() != null) {
			return visit(ctx.stringLiteral());
		} else if (ctx.numericLiteral() != null) {
			return visit(ctx.numericLiteral());
		} else if (ctx.dateTimeLiteral() != null) {
			return visit(ctx.dateTimeLiteral());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitBooleanLiteral(HqlParser.BooleanLiteralContext ctx) {

		if (ctx.TRUE() != null) {
			return List.of(new QueryParsingToken(ctx.TRUE().getText()));
		} else if (ctx.FALSE() != null) {
			return List.of(new QueryParsingToken(ctx.FALSE().getText()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitStringLiteral(HqlParser.StringLiteralContext ctx) {

		if (ctx.STRINGLITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.STRINGLITERAL().getText()));
		} else if (ctx.CHARACTER() != null) {
			return List.of(new QueryParsingToken(ctx.CHARACTER().getText()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitNumericLiteral(HqlParser.NumericLiteralContext ctx) {

		if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.INTEGER_LITERAL().getText()));
		} else if (ctx.FLOAT_LITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.FLOAT_LITERAL().getText()));
		} else if (ctx.HEXLITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.HEXLITERAL().getText()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitDateTimeLiteral(HqlParser.DateTimeLiteralContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LOCAL_DATE() != null) {
			tokens.add(new QueryParsingToken(ctx.LOCAL_DATE().getText()));
		} else if (ctx.LOCAL_TIME() != null) {
			tokens.add(new QueryParsingToken(ctx.LOCAL_TIME().getText()));
		} else if (ctx.LOCAL_DATETIME() != null) {
			tokens.add(new QueryParsingToken(ctx.LOCAL_DATETIME().getText()));
		} else if (ctx.CURRENT_DATE() != null) {
			tokens.add(new QueryParsingToken(ctx.CURRENT_DATE().getText()));
		} else if (ctx.CURRENT_TIME() != null) {
			tokens.add(new QueryParsingToken(ctx.CURRENT_TIME().getText()));
		} else if (ctx.CURRENT_TIMESTAMP() != null) {
			tokens.add(new QueryParsingToken(ctx.CURRENT_TIMESTAMP().getText()));
		} else if (ctx.OFFSET_DATETIME() != null) {
			tokens.add(new QueryParsingToken(ctx.OFFSET_DATETIME().getText()));
		} else {

			if (ctx.LOCAL() != null) {
				tokens.add(new QueryParsingToken(ctx.LOCAL().getText()));
			} else if (ctx.CURRENT() != null) {
				tokens.add(new QueryParsingToken(ctx.CURRENT().getText()));
			} else if (ctx.OFFSET() != null) {
				tokens.add(new QueryParsingToken(ctx.OFFSET().getText()));
			}

			if (ctx.DATE() != null) {
				tokens.add(new QueryParsingToken(ctx.DATE().getText()));
			} else if (ctx.TIME() != null) {
				tokens.add(new QueryParsingToken(ctx.TIME().getText()));
			} else if (ctx.DATETIME() != null) {
				tokens.add(new QueryParsingToken(ctx.DATETIME().getText()));
			}

			if (ctx.INSTANT() != null) {
				tokens.add(new QueryParsingToken(ctx.INSTANT().getText()));
			}
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitPlainPrimaryExpression(HqlParser.PlainPrimaryExpressionContext ctx) {
		return visit(ctx.primaryExpression());
	}

	@Override
	public List<QueryParsingToken> visitTupleExpression(HqlParser.TupleExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken("(", false));

		ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {
			tokens.addAll(visit(expressionOrPredicateContext));
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitHqlConcatenationExpression(HqlParser.HqlConcatenationExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(new QueryParsingToken("||"));
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGroupedExpression(HqlParser.GroupedExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.expression()));
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(new QueryParsingToken(ctx.op.getText()));
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSignedNumericLiteral(HqlParser.SignedNumericLiteralContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.op.getText()));
		tokens.addAll(visit(ctx.numericLiteral()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(new QueryParsingToken(ctx.op.getText()));
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSubqueryExpression(HqlParser.SubqueryExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.subquery()));
		NOSPACE(tokens);
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSignedExpression(HqlParser.SignedExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.op.getText()));
		tokens.addAll(visit(ctx.expression()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCaseExpression(HqlParser.CaseExpressionContext ctx) {
		return visit(ctx.caseList());
	}

	@Override
	public List<QueryParsingToken> visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
		return visit(ctx.literal());
	}

	@Override
	public List<QueryParsingToken> visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
		return visit(ctx.parameter());
	}

	@Override
	public List<QueryParsingToken> visitFunctionExpression(HqlParser.FunctionExpressionContext ctx) {
		return visit(ctx.function());
	}

	@Override
	public List<QueryParsingToken> visitGeneralPathExpression(HqlParser.GeneralPathExpressionContext ctx) {
		return visit(ctx.generalPathFragment());
	}

	@Override
	public List<QueryParsingToken> visitIdentificationVariable(HqlParser.IdentificationVariableContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		} else if (ctx.simplePath() != null) {
			return visit(ctx.simplePath());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitPath(HqlParser.PathContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.treatedPath() != null) {

			tokens.addAll(visit(ctx.treatedPath()));

			if (ctx.pathContinutation() != null) {
				tokens.addAll(visit(ctx.pathContinutation()));
			}
		} else if (ctx.generalPathFragment() != null) {
			tokens.addAll(visit(ctx.generalPathFragment()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGeneralPathFragment(HqlParser.GeneralPathFragmentContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.simplePath()));

		if (ctx.indexedPathAccessFragment() != null) {
			tokens.addAll(visit(ctx.indexedPathAccessFragment()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitIndexedPathAccessFragment(HqlParser.IndexedPathAccessFragmentContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken("[", false));
		tokens.addAll(visit(ctx.expression()));
		tokens.add(new QueryParsingToken("]"));

		if (ctx.generalPathFragment() != null) {

			tokens.add(new QueryParsingToken(".", false));
			tokens.addAll(visit(ctx.generalPathFragment()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSimplePath(HqlParser.SimplePathContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.identifier()));

		ctx.simplePathElement().forEach(simplePathElementContext -> {
			tokens.addAll(visit(simplePathElementContext));
		});

		tokens.forEach(hqlToken -> hqlToken.setSpace(false));
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSimplePathElement(HqlParser.SimplePathElementContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(".", false));
		tokens.addAll(visit(ctx.identifier()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCaseList(HqlParser.CaseListContext ctx) {

		if (ctx.simpleCaseExpression() != null) {
			return visit(ctx.simpleCaseExpression());
		} else if (ctx.searchedCaseExpression() != null) {
			return visit(ctx.searchedCaseExpression());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitSimpleCaseExpression(HqlParser.SimpleCaseExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.CASE().getText()));
		tokens.addAll(visit(ctx.expressionOrPredicate(0)));

		ctx.caseWhenExpressionClause().forEach(caseWhenExpressionClauseContext -> {
			tokens.addAll(visit(caseWhenExpressionClauseContext));
		});

		if (ctx.ELSE() != null) {

			tokens.add(new QueryParsingToken(ctx.ELSE().getText()));
			tokens.addAll(visit(ctx.expressionOrPredicate(1)));
		}

		tokens.add(new QueryParsingToken(ctx.END().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitSearchedCaseExpression(HqlParser.SearchedCaseExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.CASE().getText()));

		ctx.caseWhenPredicateClause().forEach(caseWhenPredicateClauseContext -> {
			tokens.addAll(visit(caseWhenPredicateClauseContext));
		});

		if (ctx.ELSE() != null) {

			tokens.add(new QueryParsingToken(ctx.ELSE().getText()));
			tokens.addAll(visit(ctx.expressionOrPredicate()));
		}

		tokens.add(new QueryParsingToken(ctx.END().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCaseWhenExpressionClause(HqlParser.CaseWhenExpressionClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.WHEN().getText()));
		tokens.addAll(visit(ctx.expression()));
		tokens.add(new QueryParsingToken(ctx.THEN().getText()));
		tokens.addAll(visit(ctx.expressionOrPredicate()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCaseWhenPredicateClause(HqlParser.CaseWhenPredicateClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.WHEN().getText()));
		tokens.addAll(visit(ctx.predicate()));
		tokens.add(new QueryParsingToken(ctx.THEN().getText()));
		tokens.addAll(visit(ctx.expressionOrPredicate()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGenericFunction(HqlParser.GenericFunctionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.functionName()));
		NOSPACE(tokens);
		tokens.add(new QueryParsingToken("(", false));

		if (ctx.functionArguments() != null) {
			tokens.addAll(visit(ctx.functionArguments()));
		} else if (ctx.ASTERISK() != null) {
			tokens.add(new QueryParsingToken(ctx.ASTERISK().getText()));
		}

		tokens.add(new QueryParsingToken(")"));

		if (ctx.pathContinutation() != null) {
			tokens.addAll(visit(ctx.pathContinutation()));
		}

		if (ctx.filterClause() != null) {
			tokens.addAll(visit(ctx.filterClause()));
		}

		if (ctx.withinGroup() != null) {
			tokens.addAll(visit(ctx.withinGroup()));
		}

		if (ctx.overClause() != null) {
			tokens.addAll(visit(ctx.overClause()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFunctionWithSubquery(HqlParser.FunctionWithSubqueryContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.functionName()));
		NOSPACE(tokens);
		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.subquery()));
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCastFunctionInvocation(HqlParser.CastFunctionInvocationContext ctx) {
		return visit(ctx.castFunction());
	}

	@Override
	public List<QueryParsingToken> visitExtractFunctionInvocation(HqlParser.ExtractFunctionInvocationContext ctx) {
		return visit(ctx.extractFunction());
	}

	@Override
	public List<QueryParsingToken> visitTrimFunctionInvocation(HqlParser.TrimFunctionInvocationContext ctx) {
		return visit(ctx.trimFunction());
	}

	@Override
	public List<QueryParsingToken> visitEveryFunctionInvocation(HqlParser.EveryFunctionInvocationContext ctx) {
		return visit(ctx.everyFunction());
	}

	@Override
	public List<QueryParsingToken> visitAnyFunctionInvocation(HqlParser.AnyFunctionInvocationContext ctx) {
		return visit(ctx.anyFunction());
	}

	@Override
	public List<QueryParsingToken> visitTreatedPathInvocation(HqlParser.TreatedPathInvocationContext ctx) {
		return visit(ctx.treatedPath());
	}

	@Override
	public List<QueryParsingToken> visitFunctionArguments(HqlParser.FunctionArgumentsContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.DISTINCT() != null) {
			tokens.add(new QueryParsingToken(ctx.DISTINCT().getText()));
		}

		ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {
			tokens.addAll(visit(expressionOrPredicateContext));
			NOSPACE(tokens);
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFilterClause(HqlParser.FilterClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.FILTER().getText()));
		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.whereClause()));
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitWithinGroup(HqlParser.WithinGroupContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.WITHIN().getText()));
		tokens.add(new QueryParsingToken(ctx.GROUP().getText()));
		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.orderByClause()));
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitOverClause(HqlParser.OverClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.OVER().getText()));
		tokens.add(new QueryParsingToken("(", false));

		if (ctx.partitionClause() != null) {
			tokens.addAll(visit(ctx.partitionClause()));
		}

		if (ctx.orderByClause() != null) {
			tokens.addAll(visit(ctx.orderByClause()));
			SPACE(tokens);
		}

		if (ctx.frameClause() != null) {
			tokens.addAll(visit(ctx.frameClause()));
		}

		NOSPACE(tokens);
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitPartitionClause(HqlParser.PartitionClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.PARTITION().getText()));
		tokens.add(new QueryParsingToken(ctx.BY().getText()));

		ctx.expression().forEach(expressionContext -> {
			tokens.addAll(visit(expressionContext));
			NOSPACE(tokens);
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitFrameClause(HqlParser.FrameClauseContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.RANGE() != null) {
			tokens.add(new QueryParsingToken(ctx.RANGE().getText()));
		} else if (ctx.ROWS() != null) {
			tokens.add(new QueryParsingToken(ctx.ROWS().getText()));
		} else if (ctx.GROUPS() != null) {
			tokens.add(new QueryParsingToken(ctx.GROUPS().getText()));
		}

		if (ctx.BETWEEN() != null) {
			tokens.add(new QueryParsingToken(ctx.BETWEEN().getText()));
		}

		tokens.addAll(visit(ctx.frameStart()));

		if (ctx.AND() != null) {

			tokens.add(new QueryParsingToken(ctx.AND().getText()));
			tokens.addAll(visit(ctx.frameEnd()));
		}

		if (ctx.frameExclusion() != null) {
			tokens.addAll(visit(ctx.frameExclusion()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitUnboundedPrecedingFrameStart(HqlParser.UnboundedPrecedingFrameStartContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.UNBOUNDED().getText()));
		tokens.add(new QueryParsingToken(ctx.PRECEDING().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitExpressionPrecedingFrameStart(
			HqlParser.ExpressionPrecedingFrameStartContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new QueryParsingToken(ctx.PRECEDING().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCurrentRowFrameStart(HqlParser.CurrentRowFrameStartContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.CURRENT().getText()));
		tokens.add(new QueryParsingToken(ctx.ROW().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitExpressionFollowingFrameStart(
			HqlParser.ExpressionFollowingFrameStartContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new QueryParsingToken(ctx.FOLLOWING().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCurrentRowFrameExclusion(HqlParser.CurrentRowFrameExclusionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.EXCLUDE().getText()));
		tokens.add(new QueryParsingToken(ctx.CURRENT().getText()));
		tokens.add(new QueryParsingToken(ctx.ROW().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGroupFrameExclusion(HqlParser.GroupFrameExclusionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.EXCLUDE().getText()));
		tokens.add(new QueryParsingToken(ctx.GROUP().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitTiesFrameExclusion(HqlParser.TiesFrameExclusionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.EXCLUDE().getText()));
		tokens.add(new QueryParsingToken(ctx.TIES().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitNoOthersFrameExclusion(HqlParser.NoOthersFrameExclusionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.EXCLUDE().getText()));
		tokens.add(new QueryParsingToken(ctx.NO().getText()));
		tokens.add(new QueryParsingToken(ctx.OTHERS().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitExpressionPrecedingFrameEnd(HqlParser.ExpressionPrecedingFrameEndContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new QueryParsingToken(ctx.PRECEDING().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCurrentRowFrameEnd(HqlParser.CurrentRowFrameEndContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.CURRENT().getText()));
		tokens.add(new QueryParsingToken(ctx.ROW().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitExpressionFollowingFrameEnd(HqlParser.ExpressionFollowingFrameEndContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new QueryParsingToken(ctx.FOLLOWING().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitUnboundedFollowingFrameEnd(HqlParser.UnboundedFollowingFrameEndContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.UNBOUNDED().getText()));
		tokens.add(new QueryParsingToken(ctx.FOLLOWING().getText()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCastFunction(HqlParser.CastFunctionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.CAST().getText()));
		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.expression()));
		tokens.add(new QueryParsingToken(ctx.AS().getText()));
		tokens.addAll(visit(ctx.identifier()));
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitExtractFunction(HqlParser.ExtractFunctionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.EXTRACT() != null) {

			tokens.add(new QueryParsingToken(ctx.EXTRACT().getText()));
			tokens.add(new QueryParsingToken("(", false));
			tokens.addAll(visit(ctx.expression(0)));
			tokens.add(new QueryParsingToken(ctx.FROM().getText()));
			tokens.addAll(visit(ctx.expression(1)));
			tokens.add(new QueryParsingToken(")"));
		} else if (ctx.dateTimeFunction() != null) {

			tokens.addAll(visit(ctx.dateTimeFunction()));
			tokens.add(new QueryParsingToken("(", false));
			tokens.addAll(visit(ctx.expression(0)));
			tokens.add(new QueryParsingToken(")"));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitTrimFunction(HqlParser.TrimFunctionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.TRIM().getText()));
		tokens.add(new QueryParsingToken("(", false));

		if (ctx.LEADING() != null) {
			tokens.add(new QueryParsingToken(ctx.LEADING().getText()));
		} else if (ctx.TRAILING() != null) {
			tokens.add(new QueryParsingToken(ctx.TRAILING().getText()));
		} else if (ctx.BOTH() != null) {
			tokens.add(new QueryParsingToken(ctx.BOTH().getText()));
		}

		if (ctx.stringLiteral() != null) {
			tokens.addAll(visit(ctx.stringLiteral()));
		}

		if (ctx.FROM() != null) {
			tokens.add(new QueryParsingToken(ctx.FROM().getText()));
		}

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitDateTimeFunction(HqlParser.DateTimeFunctionContext ctx) {
		return List.of(new QueryParsingToken(ctx.d.getText()));
	}

	@Override
	public List<QueryParsingToken> visitEveryFunction(HqlParser.EveryFunctionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.every.getText()));

		if (ctx.ELEMENTS() != null) {
			tokens.add(new QueryParsingToken(ctx.ELEMENTS().getText()));
		} else if (ctx.INDICES() != null) {
			tokens.add(new QueryParsingToken(ctx.INDICES().getText()));
		}

		tokens.add(new QueryParsingToken("(", false));

		if (ctx.predicate() != null) {
			tokens.addAll(visit(ctx.predicate()));
		} else if (ctx.subquery() != null) {
			tokens.addAll(visit(ctx.subquery()));
		} else if (ctx.simplePath() != null) {
			tokens.addAll(visit(ctx.simplePath()));
		}

		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitAnyFunction(HqlParser.AnyFunctionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.any.getText()));

		if (ctx.ELEMENTS() != null) {
			tokens.add(new QueryParsingToken(ctx.ELEMENTS().getText()));
		} else if (ctx.INDICES() != null) {
			tokens.add(new QueryParsingToken(ctx.INDICES().getText()));
		}

		tokens.add(new QueryParsingToken("(", false));

		if (ctx.predicate() != null) {
			tokens.addAll(visit(ctx.predicate()));
		} else if (ctx.subquery() != null) {
			tokens.addAll(visit(ctx.subquery()));
		} else if (ctx.simplePath() != null) {
			tokens.addAll(visit(ctx.simplePath()));
		}

		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitTreatedPath(HqlParser.TreatedPathContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.TREAT().getText()));
		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.path()));
		tokens.add(new QueryParsingToken(ctx.AS().getText()));
		tokens.addAll(visit(ctx.simplePath()));
		tokens.add(new QueryParsingToken(")"));

		if (ctx.pathContinutation() != null) {
			tokens.addAll(visit(ctx.pathContinutation()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitPathContinutation(HqlParser.PathContinutationContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(".", false));
		tokens.addAll(visit(ctx.simplePath()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitNullExpressionPredicate(HqlParser.NullExpressionPredicateContext ctx) {
		return visit(ctx.dealingWithNullExpression());
	}

	@Override
	public List<QueryParsingToken> visitBetweenPredicate(HqlParser.BetweenPredicateContext ctx) {
		return visit(ctx.betweenExpression());
	}

	@Override
	public List<QueryParsingToken> visitOrPredicate(HqlParser.OrPredicateContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.predicate(0)));
		tokens.add(new QueryParsingToken(ctx.OR().getText()));
		tokens.addAll(visit(ctx.predicate(1)));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitRelationalPredicate(HqlParser.RelationalPredicateContext ctx) {
		return visit(ctx.relationalExpression());
	}

	@Override
	public List<QueryParsingToken> visitExistsPredicate(HqlParser.ExistsPredicateContext ctx) {
		return visit(ctx.existsExpression());
	}

	@Override
	public List<QueryParsingToken> visitCollectionPredicate(HqlParser.CollectionPredicateContext ctx) {
		return visit(ctx.collectionExpression());
	}

	@Override
	public List<QueryParsingToken> visitAndPredicate(HqlParser.AndPredicateContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.predicate(0)));
		tokens.add(new QueryParsingToken(ctx.AND().getText()));
		tokens.addAll(visit(ctx.predicate(1)));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitGroupedPredicate(HqlParser.GroupedPredicateContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken("(", false));
		tokens.addAll(visit(ctx.predicate()));
		NOSPACE(tokens);
		tokens.add(new QueryParsingToken(")"));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitLikePredicate(HqlParser.LikePredicateContext ctx) {
		return visit(ctx.stringPatternMatching());
	}

	@Override
	public List<QueryParsingToken> visitInPredicate(HqlParser.InPredicateContext ctx) {
		return visit(ctx.inExpression());
	}

	@Override
	public List<QueryParsingToken> visitNotPredicate(HqlParser.NotPredicateContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new QueryParsingToken(ctx.NOT().getText()));
		tokens.addAll(visit(ctx.predicate()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitExpressionPredicate(HqlParser.ExpressionPredicateContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<QueryParsingToken> visitExpressionOrPredicate(HqlParser.ExpressionOrPredicateContext ctx) {

		if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else if (ctx.predicate() != null) {
			return visit(ctx.predicate());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitRelationalExpression(HqlParser.RelationalExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(new QueryParsingToken(ctx.op.getText()));
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitBetweenExpression(HqlParser.BetweenExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));

		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT().getText()));
		}

		tokens.add(new QueryParsingToken(ctx.BETWEEN().getText()));
		tokens.addAll(visit(ctx.expression(1)));
		tokens.add(new QueryParsingToken(ctx.AND().getText()));
		tokens.addAll(visit(ctx.expression(2)));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitDealingWithNullExpression(HqlParser.DealingWithNullExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(new QueryParsingToken(ctx.IS().getText()));

		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT().getText()));
		}

		if (ctx.NULL() != null) {
			tokens.add(new QueryParsingToken(ctx.NULL().getText()));
		} else if (ctx.DISTINCT() != null) {

			tokens.add(new QueryParsingToken(ctx.DISTINCT().getText()));
			tokens.add(new QueryParsingToken(ctx.FROM().getText()));
			tokens.addAll(visit(ctx.expression(1)));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitStringPatternMatching(HqlParser.StringPatternMatchingContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));

		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT().getText()));
		}

		if (ctx.LIKE() != null) {
			tokens.add(new QueryParsingToken(ctx.LIKE().getText()));
		} else if (ctx.ILIKE() != null) {
			tokens.add(new QueryParsingToken(ctx.ILIKE().getText()));
		}

		tokens.addAll(visit(ctx.expression(1)));

		if (ctx.ESCAPE() != null) {

			tokens.add(new QueryParsingToken(ctx.ESCAPE().getText()));
			tokens.addAll(visit(ctx.character()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitInExpression(HqlParser.InExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));

		if (ctx.NOT() != null) {
			tokens.add(new QueryParsingToken(ctx.NOT().getText()));
		}

		tokens.add(new QueryParsingToken(ctx.IN().getText()));
		tokens.addAll(visit(ctx.inList()));

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitInList(HqlParser.InListContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.simplePath() != null) {

			if (ctx.ELEMENTS() != null) {
				tokens.add(new QueryParsingToken(ctx.ELEMENTS().getText()));
			} else if (ctx.INDICES() != null) {
				tokens.add(new QueryParsingToken(ctx.INDICES().getText()));
			}

			tokens.add(new QueryParsingToken("(", false));
			tokens.addAll(visit(ctx.simplePath()));
			NOSPACE(tokens);
			tokens.add(new QueryParsingToken(")"));
		} else if (ctx.subquery() != null) {

			tokens.add(new QueryParsingToken("(", false));
			tokens.addAll(visit(ctx.subquery()));
			NOSPACE(tokens);
			tokens.add(new QueryParsingToken(")"));
		} else if (ctx.parameter() != null) {
			tokens.addAll(visit(ctx.parameter()));
		} else if (ctx.expressionOrPredicate() != null) {

			tokens.add(new QueryParsingToken("(", false));

			ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {
				tokens.addAll(visit(expressionOrPredicateContext));
				tokens.add(new QueryParsingToken(","));
			});
			CLIP(tokens);

			tokens.add(new QueryParsingToken(")"));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitExistsExpression(HqlParser.ExistsExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.simplePath() != null) {

			tokens.add(new QueryParsingToken(ctx.EXISTS().getText()));

			if (ctx.ELEMENTS() != null) {
				tokens.add(new QueryParsingToken(ctx.ELEMENTS().getText()));
			} else if (ctx.INDICES() != null) {
				tokens.add(new QueryParsingToken(ctx.INDICES().getText()));
			}

			tokens.add(new QueryParsingToken("(", false));
			tokens.addAll(visit(ctx.simplePath()));
			tokens.add(new QueryParsingToken(")"));

		} else if (ctx.expression() != null) {

			tokens.add(new QueryParsingToken(ctx.EXISTS().getText()));
			tokens.addAll(visit(ctx.expression()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCollectionExpression(HqlParser.CollectionExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));

		if (ctx.IS() != null) {

			tokens.add(new QueryParsingToken(ctx.IS().getText()));

			if (ctx.NOT() != null) {
				tokens.add(new QueryParsingToken(ctx.NOT().getText()));
			}

			tokens.add(new QueryParsingToken(ctx.EMPTY().getText()));
		} else if (ctx.MEMBER() != null) {

			if (ctx.NOT() != null) {
				tokens.add(new QueryParsingToken(ctx.NOT().getText()));
			}

			tokens.add(new QueryParsingToken(ctx.MEMBER().getText()));
			tokens.add(new QueryParsingToken(ctx.OF().getText()));
			tokens.addAll(visit(ctx.path()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitInstantiationTarget(HqlParser.InstantiationTargetContext ctx) {

		if (ctx.LIST() != null) {
			return List.of(new QueryParsingToken(ctx.LIST().getText()));
		} else if (ctx.MAP() != null) {
			return List.of(new QueryParsingToken(ctx.MAP().getText()));
		} else if (ctx.simplePath() != null) {

			List<QueryParsingToken> tokens = visit(ctx.simplePath());
			NOSPACE(tokens);
			return tokens;
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitInstantiationArguments(HqlParser.InstantiationArgumentsContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		ctx.instantiationArgument().forEach(instantiationArgumentContext -> {
			tokens.addAll(visit(instantiationArgumentContext));
			NOSPACE(tokens);
			tokens.add(new QueryParsingToken(","));
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitInstantiationArgument(HqlParser.InstantiationArgumentContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.expressionOrPredicate() != null) {
			tokens.addAll(visit(ctx.expressionOrPredicate()));
		} else if (ctx.instantiation() != null) {
			tokens.addAll(visit(ctx.instantiation()));
		}

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitParameterOrIntegerLiteral(HqlParser.ParameterOrIntegerLiteralContext ctx) {

		if (ctx.parameter() != null) {
			return visit(ctx.parameter());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new QueryParsingToken(ctx.INTEGER_LITERAL().getText()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitParameterOrNumberLiteral(HqlParser.ParameterOrNumberLiteralContext ctx) {

		if (ctx.parameter() != null) {
			return visit(ctx.parameter());
		} else if (ctx.numericLiteral() != null) {
			return visit(ctx.numericLiteral());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitVariable(HqlParser.VariableContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identifier() != null) {

			tokens.add(new QueryParsingToken(ctx.AS().getText()));
			tokens.addAll(visit(ctx.identifier()));
		} else if (ctx.reservedWord() != null) {
			tokens.addAll(visit(ctx.reservedWord()));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitParameter(HqlParser.ParameterContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.prefix.getText().equals(":")) {

			tokens.add(new QueryParsingToken(":", false));
			tokens.addAll(visit(ctx.identifier()));
		} else if (ctx.prefix.getText().equals("?")) {

			tokens.add(new QueryParsingToken("?", false));

			if (ctx.INTEGER_LITERAL() != null) {
				tokens.add(new QueryParsingToken(ctx.INTEGER_LITERAL().getText()));
			} else if (ctx.spelExpression() != null) {
				tokens.addAll(visit(ctx.spelExpression()));
			}
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitEntityName(HqlParser.EntityNameContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		ctx.identifier().forEach(identifierContext -> {
			tokens.addAll(visit(identifierContext));
			tokens.add(new QueryParsingToken("."));
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitIdentifier(HqlParser.IdentifierContext ctx) {

		if (ctx.reservedWord() != null) {
			return visit(ctx.reservedWord());
		} else if (ctx.spelExpression() != null) {
			return visit(ctx.spelExpression());
		} else {
			return List.of();
		}
	}

	@Override
	public List<QueryParsingToken> visitSpelExpression(HqlParser.SpelExpressionContext ctx) {

		List<QueryParsingToken> tokens = new ArrayList<>();

		if (ctx.prefix.equals("#{#")) { // #{#entityName}

			tokens.add(new QueryParsingToken(ctx.prefix.getText()));
			ctx.identificationVariable().forEach(identificationVariableContext -> {
				tokens.addAll(visit(identificationVariableContext));
				tokens.add(new QueryParsingToken("."));
			});
			CLIP(tokens);
			tokens.add(new QueryParsingToken("}"));

		} else if (ctx.prefix.equals("#{#[")) { // #{[0]}

			tokens.add(new QueryParsingToken(ctx.prefix.getText()));
			tokens.add(new QueryParsingToken(ctx.INTEGER_LITERAL().getText()));
			tokens.add(new QueryParsingToken("]}"));

		} else if (ctx.prefix.equals("#{")) {// #{escape([0])} or #{escape('foo')}

			tokens.add(new QueryParsingToken(ctx.prefix.getText()));
			tokens.addAll(visit(ctx.identificationVariable(0)));
			tokens.add(new QueryParsingToken("("));

			if (ctx.stringLiteral() != null) {
				tokens.addAll(visit(ctx.stringLiteral()));
			} else if (ctx.INTEGER_LITERAL() != null) {

				tokens.add(new QueryParsingToken("["));
				tokens.add(new QueryParsingToken(ctx.INTEGER_LITERAL().getText()));
				tokens.add(new QueryParsingToken("]"));
			}

			tokens.add(new QueryParsingToken(")}"));
		}

		return tokens;
	}

	@Override
	public List<QueryParsingToken> visitCharacter(HqlParser.CharacterContext ctx) {
		return List.of(new QueryParsingToken(ctx.CHARACTER().getText()));
	}

	@Override
	public List<QueryParsingToken> visitFunctionName(HqlParser.FunctionNameContext ctx) {
		return visit(ctx.reservedWord());
	}

	@Override
	public List<QueryParsingToken> visitReservedWord(HqlParser.ReservedWordContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return List.of(new QueryParsingToken(ctx.IDENTIFICATION_VARIABLE().getText()));
		} else {
			return List.of(new QueryParsingToken(ctx.f.getText()));
		}
	}
}
