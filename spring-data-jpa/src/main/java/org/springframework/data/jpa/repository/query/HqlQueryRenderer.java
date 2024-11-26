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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.util.ObjectUtils;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that renders an HQL query without making any changes.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @since 3.1
 */
@SuppressWarnings({ "ConstantConditions", "DuplicatedCode", "UnreachableCode" })
class HqlQueryRenderer extends HqlBaseVisitor<QueryTokenStream> {

	/**
	 * Is this select clause a {@literal subquery}?
	 *
	 * @return boolean
	 */
	static boolean isSubquery(ParserRuleContext ctx) {

		if (ctx instanceof HqlParser.SubqueryContext || ctx instanceof HqlParser.CteContext) {
			return true;
		} else if (ctx instanceof HqlParser.SelectStatementContext) {
			return false;
		} else if (ctx instanceof HqlParser.InsertStatementContext) {
			return false;
		} else if (ctx instanceof HqlParser.DeleteStatementContext) {
			return false;
		} else if (ctx instanceof HqlParser.UpdateStatementContext) {
			return false;
		} else {
			return ctx.getParent() != null && isSubquery(ctx.getParent());
		}
	}

	@Override
	public QueryTokenStream visitStart(HqlParser.StartContext ctx) {
		return visit(ctx.ql_statement());
	}

	@Override
	public QueryTokenStream visitQl_statement(HqlParser.Ql_statementContext ctx) {

		if (ctx.selectStatement() != null) {
			return visit(ctx.selectStatement());
		} else if (ctx.updateStatement() != null) {
			return visit(ctx.updateStatement());
		} else if (ctx.deleteStatement() != null) {
			return visit(ctx.deleteStatement());
		} else if (ctx.insertStatement() != null) {
			return visit(ctx.insertStatement());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitSelectStatement(HqlParser.SelectStatementContext ctx) {
		return visit(ctx.queryExpression());
	}

	@Override
	public QueryTokenStream visitQueryExpression(HqlParser.QueryExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.withClause() != null) {
			builder.appendExpression(visit(ctx.withClause()));
		}

		builder.append(visit(ctx.orderedQuery(0)));

		for (int i = 1; i < ctx.orderedQuery().size(); i++) {

			builder.append(visit(ctx.setOperator(i - 1)));
			builder.append(visit(ctx.orderedQuery(i)));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitWithClause(HqlParser.WithClauseContext ctx) {

		QueryRendererBuilder builder = QueryRendererBuilder.from(TOKEN_WITH);
		builder.append(QueryTokenStream.concatExpressions(ctx.cte(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitCte(HqlParser.CteContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.identifier()));
		builder.append(TOKEN_AS);

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		if (ctx.MATERIALIZED() != null) {
			builder.append(TOKEN_MATERIALIZED);
		}

		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.queryExpression()));
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.searchClause() != null) {
			builder.appendExpression(visit(ctx.searchClause()));
		}

		if (ctx.cycleClause() != null) {
			builder.appendExpression(visit(ctx.cycleClause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSearchClause(HqlParser.SearchClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.SEARCH()));

		if (ctx.BREADTH() != null) {
			builder.append(QueryTokens.expression(ctx.BREADTH()));
		} else if (ctx.DEPTH() != null) {
			builder.append(QueryTokens.expression(ctx.DEPTH()));
		}

		builder.append(QueryTokens.expression(ctx.FIRST()));
		builder.append(QueryTokens.expression(ctx.BY()));
		builder.append(visit(ctx.searchSpecifications()));
		builder.append(QueryTokens.expression(ctx.SET()));
		builder.append(visit(ctx.identifier()));

		return builder;
	}

	@Override
	public QueryTokenStream visitSearchSpecifications(HqlParser.SearchSpecificationsContext ctx) {
		return QueryTokenStream.concat(ctx.searchSpecification(), this::visit, TOKEN_COMMA);
	}

	@Override
	public QueryTokenStream visitSearchSpecification(HqlParser.SearchSpecificationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.identifier()));

		if (ctx.sortDirection() != null) {
			builder.append(visit(ctx.sortDirection()));
		}

		if (ctx.nullsPrecedence() != null) {
			builder.append(visit(ctx.nullsPrecedence()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCycleClause(HqlParser.CycleClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.CYCLE().getText()));
		builder.append(visit(ctx.cteAttributes()));
		builder.append(QueryTokens.expression(ctx.SET().getText()));
		builder.append(visit(ctx.identifier(0)));

		if (ctx.TO() != null) {

			builder.append(QueryTokens.expression(ctx.TO().getText()));
			builder.append(visit(ctx.literal(0)));
			builder.append(QueryTokens.expression(ctx.DEFAULT().getText()));
			builder.append(visit(ctx.literal(1)));
		}

		if (ctx.USING() != null) {

			builder.append(QueryTokens.expression(ctx.USING().getText()));
			builder.append(visit(ctx.identifier(1)));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCteAttributes(HqlParser.CteAttributesContext ctx) {
		return QueryTokenStream.concat(ctx.identifier(), this::visit, TOKEN_COMMA);
	}

	@Override
	public QueryTokenStream visitOrderedQuery(HqlParser.OrderedQueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.query() != null) {
			builder.append(visit(ctx.query()));
		} else if (ctx.queryExpression() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.append(visit(ctx.queryExpression()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		if (ctx.queryOrder() != null) {
			builder.append(visit(ctx.queryOrder()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSelectQuery(HqlParser.SelectQueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.selectClause() != null) {
			builder.appendExpression(visit(ctx.selectClause()));
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

		return builder;
	}

	@Override
	public QueryTokenStream visitFromQuery(HqlParser.FromQueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.fromClause()));

		if (ctx.whereClause() != null) {
			builder.append(visit(ctx.whereClause()));
		}

		if (ctx.groupByClause() != null) {
			builder.append(visit(ctx.groupByClause()));
		}

		if (ctx.havingClause() != null) {
			builder.append(visit(ctx.havingClause()));
		}

		if (ctx.selectClause() != null) {
			builder.append(visit(ctx.selectClause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitQueryOrder(HqlParser.QueryOrderContext ctx) {

		if (ctx.limitClause() == null && ctx.offsetClause() == null && ctx.fetchClause() == null) {
			return visit(ctx.orderByClause());
		}

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.orderByClause()));

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
	public QueryTokenStream visitFromClause(HqlParser.FromClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.FROM()));
		builder.appendExpression(QueryTokenStream.concat(ctx.entityWithJoins(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitEntityWithJoins(HqlParser.EntityWithJoinsContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.fromRoot()));
		builder.appendInline(QueryTokenStream.concat(ctx.joinSpecifier(), this::visit, TOKEN_SPACE));

		return builder;
	}

	@Override
	public QueryTokenStream visitJoinSpecifier(HqlParser.JoinSpecifierContext ctx) {

		if (ctx.join() != null) {
			return visit(ctx.join());
		} else if (ctx.crossJoin() != null) {
			return visit(ctx.crossJoin());
		} else if (ctx.jpaCollectionJoin() != null) {
			return visit(ctx.jpaCollectionJoin());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitFromRoot(HqlParser.FromRootContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.entityName() != null) {

			builder.appendExpression(visit(ctx.entityName()));

			if (ctx.variable() != null) {
				builder.appendExpression(visit(ctx.variable()));
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
	public QueryTokenStream visitJoin(HqlParser.JoinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.joinType()));
		builder.append(QueryTokens.expression(ctx.JOIN()));

		if (ctx.FETCH() != null) {
			builder.append(QueryTokens.expression(ctx.FETCH()));
		}

		builder.append(visit(ctx.joinTarget()));

		if (ctx.joinRestriction() != null) {
			builder.appendExpression(visit(ctx.joinRestriction()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitJoinPath(HqlParser.JoinPathContext ctx) {

		HqlParser.VariableContext variable = ctx.variable();

		if (variable == null) {
			return visit(ctx.path());
		}

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.appendExpression(visit(ctx.path()));
		builder.appendExpression(visit(variable));

		return builder;
	}

	@Override
	public QueryTokenStream visitJoinSubquery(HqlParser.JoinSubqueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LATERAL() != null) {
			builder.append(QueryTokens.expression(ctx.LATERAL()));
		}

		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.subquery()));
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.variable() != null) {
			builder.appendExpression(visit(ctx.variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitUpdateStatement(HqlParser.UpdateStatementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.UPDATE()));

		if (ctx.VERSIONED() != null) {
			builder.append(QueryTokens.expression(ctx.VERSIONED()));
		}

		builder.appendExpression(visit(ctx.targetEntity()));
		builder.appendExpression(visit(ctx.setClause()));

		if (ctx.whereClause() != null) {
			builder.appendExpression(visit(ctx.whereClause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitTargetEntity(HqlParser.TargetEntityContext ctx) {

		HqlParser.VariableContext variable = ctx.variable();

		if (variable == null) {
			return visit(ctx.entityName());
		}

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.appendExpression(visit(ctx.entityName()));
		builder.appendExpression(visit(variable));

		return builder;
	}

	@Override
	public QueryTokenStream visitSetClause(HqlParser.SetClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.SET()));
		return builder.append(QueryTokenStream.concat(ctx.assignment(), this::visit, TOKEN_COMMA));
	}

	@Override
	public QueryTokenStream visitAssignment(HqlParser.AssignmentContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.simplePath()));
		builder.append(TOKEN_EQUALS);
		builder.append(visit(ctx.expressionOrPredicate()));

		return builder;
	}

	@Override
	public QueryTokenStream visitDeleteStatement(HqlParser.DeleteStatementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.DELETE()));

		if (ctx.FROM() != null) {
			builder.append(QueryTokens.expression(ctx.FROM()));
		}

		builder.append(visit(ctx.targetEntity()));

		if (ctx.whereClause() != null) {
			builder.append(visit(ctx.whereClause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitInsertStatement(HqlParser.InsertStatementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.INSERT()));

		if (ctx.INTO() != null) {
			builder.append(QueryTokens.expression(ctx.INTO()));
		}

		builder.appendExpression(visit(ctx.targetEntity()));
		builder.appendExpression(visit(ctx.targetFields()));

		if (ctx.queryExpression() != null) {
			builder.appendExpression(visit(ctx.queryExpression()));
		} else if (ctx.valuesList() != null) {
			builder.appendExpression(visit(ctx.valuesList()));
		}

		if (ctx.conflictClause() != null) {
			builder.appendExpression(visit(ctx.conflictClause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitTargetFields(HqlParser.TargetFieldsContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_PAREN);
		builder.append(QueryTokenStream.concat(ctx.simplePath(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitValuesList(HqlParser.ValuesListContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.VALUES()));
		builder.append(QueryTokenStream.concat(ctx.values(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitValues(HqlParser.ValuesContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_PAREN);
		builder.append(QueryTokenStream.concat(ctx.expression(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitConflictClause(HqlParser.ConflictClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.ON()));
		builder.append(QueryTokens.expression(ctx.CONFLICT()));

		if (ctx.conflictTarget() != null) {
			builder.appendExpression(visit(ctx.conflictTarget()));
		}

		builder.append(QueryTokens.expression(ctx.DO()));
		builder.appendExpression(visit(ctx.conflictAction()));

		return builder;
	}

	@Override
	public QueryTokenStream visitConflictTarget(HqlParser.ConflictTargetContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.identifier() != null) {

			builder.append(QueryTokens.expression(ctx.ON()));
			builder.append(QueryTokens.expression(ctx.CONSTRAINT()));
			builder.appendExpression(visit(ctx.identifier()));
		}

		if (!ObjectUtils.isEmpty(ctx.simplePath())) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.append(QueryTokenStream.concat(ctx.simplePath(), this::visit, TOKEN_COMMA));

			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitConflictAction(HqlParser.ConflictActionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.NOTHING() != null) {
			builder.append(QueryTokens.expression(ctx.NOTHING()));
		} else {
			builder.append(QueryTokens.expression(ctx.UPDATE()));
			builder.appendExpression(visit(ctx.setClause()));

			if (ctx.whereClause() != null) {
				builder.appendExpression(visit(ctx.whereClause()));
			}
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitInstantiation(HqlParser.InstantiationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.NEW()));
		builder.append(visit(ctx.instantiationTarget()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.instantiationArguments()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitGroupedItem(HqlParser.GroupedItemContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.INTEGER_LITERAL()));
		} else if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitSortedItem(HqlParser.SortedItemContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.sortExpression()));

		if (ctx.sortDirection() != null) {
			builder.append(visit(ctx.sortDirection()));
		}

		if (ctx.nullsPrecedence() != null) {
			builder.appendExpression(visit(ctx.nullsPrecedence()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSortExpression(HqlParser.SortExpressionContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.INTEGER_LITERAL()));
		} else if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitSortDirection(HqlParser.SortDirectionContext ctx) {

		if (ctx.ASC() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.ASC()));
		} else if (ctx.DESC() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.DESC()));
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitNullsPrecedence(HqlParser.NullsPrecedenceContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.NULLS()));

		if (ctx.FIRST() != null) {
			builder.append(QueryTokens.expression(ctx.FIRST()));
		} else if (ctx.LAST() != null) {
			builder.append(QueryTokens.expression(ctx.LAST()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitLimitClause(HqlParser.LimitClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.LIMIT()));
		builder.append(visit(ctx.parameterOrIntegerLiteral()));

		return builder;
	}

	@Override
	public QueryTokenStream visitOffsetClause(HqlParser.OffsetClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.OFFSET()));
		builder.append(visit(ctx.parameterOrIntegerLiteral()));

		if (ctx.ROW() != null) {
			builder.append(QueryTokens.expression(ctx.ROW()));
		} else if (ctx.ROWS() != null) {
			builder.append(QueryTokens.expression(ctx.ROWS()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFetchClause(HqlParser.FetchClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.FETCH()));

		if (ctx.FIRST() != null) {
			builder.append(QueryTokens.expression(ctx.FIRST()));
		} else if (ctx.NEXT() != null) {
			builder.append(QueryTokens.expression(ctx.NEXT()));
		}

		if (ctx.parameterOrIntegerLiteral() != null) {
			builder.append(visit(ctx.parameterOrIntegerLiteral()));
		} else if (ctx.parameterOrNumberLiteral() != null) {
			builder.append(visit(ctx.parameterOrNumberLiteral()));
		}

		if (ctx.ROW() != null) {
			builder.append(QueryTokens.expression(ctx.ROW()));
		} else if (ctx.ROWS() != null) {
			builder.append(QueryTokens.expression(ctx.ROWS()));
		}

		if (ctx.ONLY() != null) {
			builder.append(QueryTokens.expression(ctx.ONLY()));
		} else if (ctx.WITH() != null) {

			builder.append(QueryTokens.expression(ctx.WITH()));
			builder.append(QueryTokens.expression(ctx.TIES()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSubquery(HqlParser.SubqueryContext ctx) {
		return visit(ctx.queryExpression());
	}

	@Override
	public QueryTokenStream visitSelectClause(HqlParser.SelectClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.SELECT()));

		if (ctx.DISTINCT() != null) {
			builder.append(QueryTokens.expression(ctx.DISTINCT()));
		}

		builder.appendExpression(visit(ctx.selectionList()));

		return builder;
	}

	@Override
	public QueryTokenStream visitSelectionList(HqlParser.SelectionListContext ctx) {
		return QueryTokenStream.concat(ctx.selection(), this::visit, TOKEN_COMMA);
	}

	@Override
	public QueryTokenStream visitSelection(HqlParser.SelectionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.selectExpression()));

		if (ctx.variable() != null) {
			builder.appendExpression(visit(ctx.variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSelectExpression(HqlParser.SelectExpressionContext ctx) {

		if (ctx.instantiation() != null) {
			return visit(ctx.instantiation());
		} else if (ctx.mapEntrySelection() != null) {
			return visit(ctx.mapEntrySelection());
		} else if (ctx.jpaSelectObjectSyntax() != null) {
			return visit(ctx.jpaSelectObjectSyntax());
		} else if (ctx.expressionOrPredicate() != null) {
			return visit(ctx.expressionOrPredicate());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitMapEntrySelection(HqlParser.MapEntrySelectionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.ENTRY()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.path()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitJpaSelectObjectSyntax(HqlParser.JpaSelectObjectSyntaxContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.OBJECT()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.identifier()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitWhereClause(HqlParser.WhereClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WHERE()));
		builder.append(QueryTokenStream.concatExpressions(ctx.predicate(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitJoinType(HqlParser.JoinTypeContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.INNER() != null) {
			builder.append(QueryTokens.expression(ctx.INNER()));
		}
		if (ctx.LEFT() != null) {
			builder.append(QueryTokens.expression(ctx.LEFT()));
		}
		if (ctx.RIGHT() != null) {
			builder.append(QueryTokens.expression(ctx.RIGHT()));
		}
		if (ctx.FULL() != null) {
			builder.append(QueryTokens.expression(ctx.FULL()));
		}
		if (ctx.OUTER() != null) {
			builder.append(QueryTokens.expression(ctx.OUTER()));
		}
		if (ctx.CROSS() != null) {
			builder.append(QueryTokens.expression(ctx.CROSS()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCrossJoin(HqlParser.CrossJoinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.CROSS()));
		builder.append(QueryTokens.expression(ctx.JOIN()));
		builder.appendExpression(visit(ctx.entityName()));

		if (ctx.variable() != null) {
			builder.appendExpression(visit(ctx.variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitJoinRestriction(HqlParser.JoinRestrictionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.ON() != null) {
			builder.append(QueryTokens.expression(ctx.ON()));
		} else if (ctx.WITH() != null) {
			builder.append(QueryTokens.expression(ctx.WITH()));
		}

		builder.appendExpression(visit(ctx.predicate()));

		return builder;
	}

	@Override
	public QueryTokenStream visitJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_COMMA);
		builder.append(QueryTokens.token(ctx.IN()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.path()));
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.variable() != null) {
			builder.appendExpression(visit(ctx.variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitGroupByClause(HqlParser.GroupByClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.GROUP()));
		builder.append(QueryTokens.expression(ctx.BY()));
		builder.append(QueryTokenStream.concat(ctx.groupedItem(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitOrderByClause(HqlParser.OrderByClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.ORDER()));
		builder.append(QueryTokens.expression(ctx.BY()));
		builder.appendExpression(QueryTokenStream.concat(ctx.sortedItem(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitHavingClause(HqlParser.HavingClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.HAVING()));
		builder.appendExpression(QueryTokenStream.concat(ctx.predicate(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitSetOperator(HqlParser.SetOperatorContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.UNION() != null) {
			builder.append(QueryTokens.expression(ctx.UNION()));
		} else if (ctx.INTERSECT() != null) {
			builder.append(QueryTokens.expression(ctx.INTERSECT()));
		} else if (ctx.EXCEPT() != null) {
			builder.append(QueryTokens.expression(ctx.EXCEPT()));
		}

		if (ctx.ALL() != null) {
			builder.append(QueryTokens.expression(ctx.ALL()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitLiteral(HqlParser.LiteralContext ctx) {

		if (ctx.NULL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.NULL()));
		} else if (ctx.booleanLiteral() != null) {
			return visit(ctx.booleanLiteral());
		} else if (ctx.stringLiteral() != null) {
			return visit(ctx.stringLiteral());
		} else if (ctx.numericLiteral() != null) {
			return visit(ctx.numericLiteral());
		} else if (ctx.dateTimeLiteral() != null) {
			return visit(ctx.dateTimeLiteral());
		} else if (ctx.binaryLiteral() != null) {
			return visit(ctx.binaryLiteral());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitBooleanLiteral(HqlParser.BooleanLiteralContext ctx) {

		if (ctx.TRUE() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.TRUE()));
		} else if (ctx.FALSE() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.FALSE()));
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitStringLiteral(HqlParser.StringLiteralContext ctx) {

		if (ctx.STRINGLITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.STRINGLITERAL()));
		} else if (ctx.CHARACTER() != null) {
			return QueryRendererBuilder.from(QueryTokens.expression(ctx.CHARACTER()));
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitNumericLiteral(HqlParser.NumericLiteralContext ctx) {

		if (ctx.INTEGER_LITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.INTEGER_LITERAL()));
		} else if (ctx.FLOAT_LITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.FLOAT_LITERAL()));
		} else if (ctx.HEXLITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.HEXLITERAL()));
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitDateTimeLiteral(HqlParser.DateTimeLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LOCAL_DATE() != null) {
			builder.append(QueryTokens.expression(ctx.LOCAL_DATE()));
		} else if (ctx.LOCAL_TIME() != null) {
			builder.append(QueryTokens.expression(ctx.LOCAL_TIME()));
		} else if (ctx.LOCAL_DATETIME() != null) {
			builder.append(QueryTokens.expression(ctx.LOCAL_DATETIME()));
		} else if (ctx.CURRENT_DATE() != null) {
			builder.append(QueryTokens.expression(ctx.CURRENT_DATE()));
		} else if (ctx.CURRENT_TIME() != null) {
			builder.append(QueryTokens.expression(ctx.CURRENT_TIME()));
		} else if (ctx.CURRENT_TIMESTAMP() != null) {
			builder.append(QueryTokens.expression(ctx.CURRENT_TIMESTAMP()));
		} else if (ctx.OFFSET_DATETIME() != null) {
			builder.append(QueryTokens.expression(ctx.OFFSET_DATETIME()));
		} else {

			if (ctx.LOCAL() != null) {
				builder.append(QueryTokens.expression(ctx.LOCAL()));
			} else if (ctx.CURRENT() != null) {
				builder.append(QueryTokens.expression(ctx.CURRENT()));
			} else if (ctx.OFFSET() != null) {
				builder.append(QueryTokens.expression(ctx.OFFSET()));
			}

			if (ctx.DATE() != null) {
				builder.append(QueryTokens.expression(ctx.DATE()));
			} else if (ctx.TIME() != null) {
				builder.append(QueryTokens.expression(ctx.TIME()));
			} else if (ctx.DATETIME() != null) {
				builder.append(QueryTokens.expression(ctx.DATETIME()));
			}

			if (ctx.INSTANT() != null) {
				builder.append(QueryTokens.expression(ctx.INSTANT()));
			}
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitDatetimeField(HqlParser.DatetimeFieldContext ctx) {

		if (ctx.YEAR() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.YEAR()));
		} else if (ctx.MONTH() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.MONTH()));
		} else if (ctx.DAY() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.DAY()));
		} else if (ctx.WEEK() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.WEEK()));
		} else if (ctx.QUARTER() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.QUARTER()));
		} else if (ctx.HOUR() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.HOUR()));
		} else if (ctx.MINUTE() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.MINUTE()));
		} else if (ctx.SECOND() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.SECOND()));
		} else if (ctx.NANOSECOND() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.NANOSECOND()));
		} else if (ctx.EPOCH() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.EPOCH()));
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitDayField(HqlParser.DayFieldContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.DAY()));
		builder.append(QueryTokens.expression(ctx.OF()));

		if (ctx.MONTH() != null) {
			builder.append(QueryTokens.expression(ctx.MONTH()));
		}

		if (ctx.WEEK() != null) {
			builder.append(QueryTokens.expression(ctx.WEEK()));
		}

		if (ctx.YEAR() != null) {
			builder.append(QueryTokens.expression(ctx.YEAR()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitWeekField(HqlParser.WeekFieldContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WEEK()));
		builder.append(QueryTokens.expression(ctx.OF()));

		if (ctx.MONTH() != null) {
			builder.append(QueryTokens.expression(ctx.MONTH()));
		}

		if (ctx.YEAR() != null) {
			builder.append(QueryTokens.expression(ctx.YEAR()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitTimeZoneField(HqlParser.TimeZoneFieldContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.OFFSET() != null) {
			builder.append(QueryTokens.expression(ctx.OFFSET()));

			if (ctx.HOUR() != null) {
				builder.append(QueryTokens.expression(ctx.HOUR()));
			}

			if (ctx.MINUTE() != null) {
				builder.append(QueryTokens.expression(ctx.MINUTE()));
			}
		}

		if (ctx.TIMEZONE_HOUR() != null) {
			builder.append(QueryTokens.expression(ctx.TIMEZONE_HOUR()));
		}

		if (ctx.TIMEZONE_HOUR() != null) {
			builder.append(QueryTokens.expression(ctx.TIMEZONE_MINUTE()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitDateOrTimeField(HqlParser.DateOrTimeFieldContext ctx) {
		return QueryRendererBuilder.from(QueryTokens.expression(ctx.DATE() != null ? ctx.DATE() : ctx.TIME()));
	}

	@Override
	public QueryTokenStream visitBinaryLiteral(HqlParser.BinaryLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.BINARY_LITERAL() != null) {
			builder.append(QueryTokens.expression(ctx.BINARY_LITERAL()));
		} else if (ctx.HEXLITERAL() != null) {

			builder.append(TOKEN_OPEN_BRACE);

			builder.append(QueryTokenStream.concat(ctx.HEXLITERAL(), it -> {
				return QueryRendererBuilder.from(QueryTokens.token(it));
			}, TOKEN_COMMA));

			builder.append(TOKEN_CLOSE_BRACE);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitPlainPrimaryExpression(HqlParser.PlainPrimaryExpressionContext ctx) {
		return visit(ctx.primaryExpression());
	}

	@Override
	public QueryTokenStream visitTupleExpression(HqlParser.TupleExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_PAREN);
		builder.append(QueryTokenStream.concat(ctx.expressionOrPredicate(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitHqlConcatenationExpression(HqlParser.HqlConcatenationExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.expression(0)));
		builder.append(TOKEN_DOUBLE_PIPE);
		builder.append(visit(ctx.expression(1)));

		return builder;
	}

	@Override
	public QueryTokenStream visitDayOfWeekExpression(HqlParser.DayOfWeekExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.DAY()));
		builder.append(QueryTokens.expression(ctx.OF()));
		builder.append(QueryTokens.expression(ctx.WEEK()));

		return builder;
	}

	@Override
	public QueryTokenStream visitDayOfMonthExpression(HqlParser.DayOfMonthExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.DAY()));
		builder.append(QueryTokens.expression(ctx.OF()));
		builder.append(QueryTokens.expression(ctx.MONTH()));

		return builder;
	}

	@Override
	public QueryTokenStream visitWeekOfYearExpression(HqlParser.WeekOfYearExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WEEK()));
		builder.append(QueryTokens.expression(ctx.OF()));
		builder.append(QueryTokens.expression(ctx.YEAR()));

		return builder;
	}

	@Override
	public QueryTokenStream visitGroupedExpression(HqlParser.GroupedExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.expression()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.expression(0)));
		builder.append(QueryTokens.ventilated(ctx.op));
		builder.appendInline(visit(ctx.expression(1)));

		return builder;
	}

	@Override
	public QueryTokenStream visitSignedNumericLiteral(HqlParser.SignedNumericLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.op));
		builder.append(visit(ctx.numericLiteral()));

		return builder;
	}

	@Override
	public QueryTokenStream visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expression(0)));
		builder.append(QueryTokens.expression(ctx.op));
		builder.appendExpression(visit(ctx.expression(1)));

		return builder;
	}

	@Override
	public QueryTokenStream visitSubqueryExpression(HqlParser.SubqueryExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.subquery()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitSignedExpression(HqlParser.SignedExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.op));
		builder.appendInline(visit(ctx.expression()));

		return builder;
	}

	@Override
	public QueryTokenStream visitToDurationExpression(HqlParser.ToDurationExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.expression()));
		builder.appendExpression(visit(ctx.datetimeField()));

		return builder;
	}

	@Override
	public QueryTokenStream visitFromDurationExpression(HqlParser.FromDurationExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.expression()));
		builder.append(QueryTokens.expression(ctx.BY()));
		builder.appendExpression(visit(ctx.datetimeField()));

		return builder;
	}

	@Override
	public QueryTokenStream visitCaseExpression(HqlParser.CaseExpressionContext ctx) {
		return visit(ctx.caseList());
	}

	@Override
	public QueryTokenStream visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
		return visit(ctx.literal());
	}

	@Override
	public QueryTokenStream visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
		return visit(ctx.parameter());
	}

	@Override
	public QueryTokenStream visitFunctionExpression(HqlParser.FunctionExpressionContext ctx) {
		return visit(ctx.function());
	}

	@Override
	public QueryTokenStream visitStandardFunctionInvocation(HqlParser.StandardFunctionInvocationContext ctx) {
		return visit(ctx.standardFunction());
	}

	@Override
	public QueryTokenStream visitAggregateFunctionInvocation(HqlParser.AggregateFunctionInvocationContext ctx) {
		return visit(ctx.aggregateFunction());
	}

	@Override
	public QueryTokenStream visitCollectionSizeFunctionInvocation(HqlParser.CollectionSizeFunctionInvocationContext ctx) {
		return visit(ctx.collectionSizeFunction());
	}

	@Override
	public QueryTokenStream visitCollectionAggregateFunctionInvocation(
			HqlParser.CollectionAggregateFunctionInvocationContext ctx) {
		return visit(ctx.collectionAggregateFunction());
	}

	@Override
	public QueryTokenStream visitCollectionFunctionMisuseInvocation(
			HqlParser.CollectionFunctionMisuseInvocationContext ctx) {
		return visit(ctx.collectionFunctionMisuse());
	}

	@Override
	public QueryTokenStream visitJpaNonstandardFunctionInvocation(HqlParser.JpaNonstandardFunctionInvocationContext ctx) {
		return visit(ctx.jpaNonstandardFunction());
	}

	@Override
	public QueryTokenStream visitColumnFunctionInvocation(HqlParser.ColumnFunctionInvocationContext ctx) {
		return visit(ctx.columnFunction());
	}

	@Override
	public QueryTokenStream visitGenericFunctionInvocation(HqlParser.GenericFunctionInvocationContext ctx) {
		return visit(ctx.genericFunction());
	}

	@Override
	public QueryTokenStream visitStandardFunction(HqlParser.StandardFunctionContext ctx) {

		if (ctx.castFunction() != null) {
			return visit(ctx.castFunction());
		}

		if (ctx.treatedPath() != null) {
			return visit(ctx.treatedPath());
		}

		if (ctx.extractFunction() != null) {
			return visit(ctx.extractFunction());
		}

		if (ctx.truncFunction() != null) {
			return visit(ctx.truncFunction());
		}

		if (ctx.formatFunction() != null) {
			return visit(ctx.formatFunction());
		}

		if (ctx.collateFunction() != null) {
			return visit(ctx.collateFunction());
		}

		if (ctx.substringFunction() != null) {
			return visit(ctx.substringFunction());
		}

		if (ctx.overlayFunction() != null) {
			return visit(ctx.overlayFunction());
		}

		if (ctx.trimFunction() != null) {
			return visit(ctx.trimFunction());
		}

		if (ctx.padFunction() != null) {
			return visit(ctx.padFunction());
		}

		if (ctx.positionFunction() != null) {
			return visit(ctx.positionFunction());
		}

		if (ctx.currentDateFunction() != null) {
			return visit(ctx.currentDateFunction());
		}

		if (ctx.currentTimeFunction() != null) {
			return visit(ctx.currentTimeFunction());
		}

		if (ctx.currentTimestampFunction() != null) {
			return visit(ctx.currentTimestampFunction());
		}

		if (ctx.instantFunction() != null) {
			return visit(ctx.instantFunction());
		}

		if (ctx.localDateFunction() != null) {
			return visit(ctx.localDateFunction());
		}

		if (ctx.localTimeFunction() != null) {
			return visit(ctx.localTimeFunction());
		}

		if (ctx.localDateTimeFunction() != null) {
			return visit(ctx.localDateTimeFunction());
		}

		if (ctx.offsetDateTimeFunction() != null) {
			return visit(ctx.offsetDateTimeFunction());
		}

		if (ctx.cube() != null) {
			return visit(ctx.cube());
		}

		if (ctx.rollup() != null) {
			return visit(ctx.rollup());
		}

		return QueryTokenStream.empty();
	}

	@Override
	public QueryTokenStream visitSubstringFunction(HqlParser.SubstringFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.SUBSTRING()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.expression()));

		if (ctx.FROM() == null) {
			builder.append(TOKEN_COMMA);
		} else {
			builder.append(QueryTokens.expression(ctx.FROM()));
		}

		builder.append(visit(ctx.substringFunctionStartArgument()));

		if (ctx.substringFunctionLengthArgument() != null) {
			if (ctx.FOR() == null) {
				builder.append(TOKEN_COMMA);
			} else {
				builder.append(QueryTokens.expression(ctx.FOR()));
			}

			builder.append(visit(ctx.substringFunctionLengthArgument()));
		}

		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitSubstringFunctionStartArgument(HqlParser.SubstringFunctionStartArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitSubstringFunctionLengthArgument(HqlParser.SubstringFunctionLengthArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitPadFunction(HqlParser.PadFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.PAD()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.expression()));
		builder.append(QueryTokens.expression(ctx.WITH()));
		builder.appendExpression(visit(ctx.padLength()));

		if (ctx.padCharacter() != null) {
			builder.appendExpression(visit(ctx.padSpecification()));
			builder.appendInline(visit(ctx.padCharacter()));
		} else {
			builder.append(visit(ctx.padSpecification()));
		}

		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitPadSpecification(HqlParser.PadSpecificationContext ctx) {
		return QueryRendererBuilder.from(QueryTokens.token(ctx.LEADING() != null ? ctx.LEADING() : ctx.TRAILING()));
	}

	@Override
	public QueryTokenStream visitPadCharacter(HqlParser.PadCharacterContext ctx) {
		return visit(ctx.stringLiteral());
	}

	@Override
	public QueryTokenStream visitPadLength(HqlParser.PadLengthContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitPositionFunction(HqlParser.PositionFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.POSITION()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.positionFunctionPatternArgument()));
		builder.append(QueryTokens.expression(ctx.IN()));
		builder.appendInline(visit(ctx.positionFunctionStringArgument()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitPositionFunctionPatternArgument(HqlParser.PositionFunctionPatternArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitPositionFunctionStringArgument(HqlParser.PositionFunctionStringArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitOverlayFunction(HqlParser.OverlayFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.OVERLAY()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.overlayFunctionStringArgument()));
		builder.append(QueryTokens.expression(ctx.PLACING()));
		builder.append(visit(ctx.overlayFunctionReplacementArgument()));
		builder.append(QueryTokens.expression(ctx.FROM()));
		builder.append(visit(ctx.overlayFunctionStartArgument()));

		if (ctx.overlayFunctionLengthArgument() != null) {
			builder.append(QueryTokens.expression(ctx.FOR()));
			builder.append(visit(ctx.overlayFunctionLengthArgument()));
		}
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitOverlayFunctionStringArgument(HqlParser.OverlayFunctionStringArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitOverlayFunctionReplacementArgument(
			HqlParser.OverlayFunctionReplacementArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitOverlayFunctionStartArgument(HqlParser.OverlayFunctionStartArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitOverlayFunctionLengthArgument(HqlParser.OverlayFunctionLengthArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitCurrentDateFunction(HqlParser.CurrentDateFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CURRENT_DATE() != null) {
			builder.append(QueryTokens.token(ctx.CURRENT_DATE()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		} else {
			builder.append(QueryTokens.expression(ctx.CURRENT()));
			builder.append(QueryTokens.expression(ctx.DATE()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCurrentTimeFunction(HqlParser.CurrentTimeFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CURRENT_TIME() != null) {
			builder.append(QueryTokens.token(ctx.CURRENT_TIME()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		} else {
			builder.append(QueryTokens.expression(ctx.CURRENT()));
			builder.append(QueryTokens.expression(ctx.TIME()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCurrentTimestampFunction(HqlParser.CurrentTimestampFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CURRENT_TIMESTAMP() != null) {
			builder.append(QueryTokens.token(ctx.CURRENT_TIMESTAMP()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		} else {
			builder.append(QueryTokens.expression(ctx.CURRENT()));
			builder.append(QueryTokens.expression(ctx.TIMESTAMP()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitInstantFunction(HqlParser.InstantFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CURRENT_INSTANT() != null) {
			builder.append(QueryTokens.token(ctx.CURRENT_INSTANT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		} else {
			builder.append(QueryTokens.expression(ctx.INSTANT()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitLocalDateTimeFunction(HqlParser.LocalDateTimeFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LOCAL_DATETIME() != null) {
			builder.append(QueryTokens.token(ctx.LOCAL_DATETIME()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		} else {
			builder.append(QueryTokens.expression(ctx.LOCAL()));
			builder.append(QueryTokens.expression(ctx.DATETIME()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitOffsetDateTimeFunction(HqlParser.OffsetDateTimeFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.OFFSET_DATETIME() != null) {
			builder.append(QueryTokens.token(ctx.OFFSET_DATETIME()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		} else {
			builder.append(QueryTokens.expression(ctx.OFFSET()));
			builder.append(QueryTokens.expression(ctx.DATETIME()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitLocalDateFunction(HqlParser.LocalDateFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LOCAL_DATE() != null) {
			builder.append(QueryTokens.token(ctx.LOCAL_DATE()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		} else {
			builder.append(QueryTokens.expression(ctx.LOCAL()));
			builder.append(QueryTokens.expression(ctx.DATE()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitLocalTimeFunction(HqlParser.LocalTimeFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LOCAL_TIME() != null) {
			builder.append(QueryTokens.token(ctx.LOCAL_TIME()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		} else {
			builder.append(QueryTokens.expression(ctx.LOCAL()));
			builder.append(QueryTokens.expression(ctx.TIME()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFormatFunction(HqlParser.FormatFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.FORMAT()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.expression()));
		builder.append(QueryTokens.expression(ctx.AS()));
		builder.appendInline(visit(ctx.format()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitCollation(HqlParser.CollationContext ctx) {
		return visit(ctx.simplePath());
	}

	@Override
	public QueryTokenStream visitCollateFunction(HqlParser.CollateFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.COLLATE()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.expression()));
		builder.append(QueryTokens.expression(ctx.AS()));
		builder.appendInline(visit(ctx.collation()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitCube(HqlParser.CubeContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.CUBE()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(QueryTokenStream.concat(ctx.expressionOrPredicate(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitRollup(HqlParser.RollupContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.ROLLUP()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(QueryTokenStream.concat(ctx.expressionOrPredicate(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitFormat(HqlParser.FormatContext ctx) {
		return visit(ctx.stringLiteral());
	}

	@Override
	public QueryTokenStream visitTruncFunction(HqlParser.TruncFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.TRUNC() != null) {
			builder.append(QueryTokens.token(ctx.TRUNC()));
		} else {
			builder.append(QueryTokens.token(ctx.TRUNCATE()));
		}

		builder.append(TOKEN_OPEN_PAREN);

		if (ctx.datetimeField() != null) {
			builder.append(visit(ctx.expression(0)));
			builder.append(TOKEN_COMMA);
			builder.append(visit(ctx.datetimeField()));
		} else {
			builder.append(QueryTokenStream.concat(ctx.expression(), this::visit, TOKEN_COMMA));
		}
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitJpaNonstandardFunction(HqlParser.JpaNonstandardFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.FUNCTION()));
		builder.append(TOKEN_OPEN_PAREN);

		QueryRendererBuilder nested = QueryRenderer.builder();
		nested.appendInline(visit(ctx.jpaNonstandardFunctionName()));

		if (ctx.castTarget() != null) {
			nested.append(QueryTokens.expression(ctx.AS()));
			nested.append(visit(ctx.castTarget()));
		}

		if (ctx.genericFunctionArguments() != null) {
			nested.append(TOKEN_COMMA);
			nested.appendInline(visit(ctx.genericFunctionArguments()));
		}

		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitJpaNonstandardFunctionName(HqlParser.JpaNonstandardFunctionNameContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		}

		return visit(ctx.stringLiteral());
	}

	@Override
	public QueryTokenStream visitColumnFunction(HqlParser.ColumnFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.COLUMN()));
		builder.append(TOKEN_OPEN_PAREN);

		QueryRendererBuilder nested = QueryRenderer.builder();
		nested.appendInline(visit(ctx.path()));
		nested.append(TOKEN_DOT);
		nested.appendExpression(visit(ctx.jpaNonstandardFunctionName()));

		if (ctx.castTarget() != null) {
			nested.append(QueryTokens.expression(ctx.AS()));
			nested.appendExpression(visit(ctx.jpaNonstandardFunctionName()));
		}

		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitGenericFunctionName(HqlParser.GenericFunctionNameContext ctx) {
		return visit(ctx.simplePath());
	}

	@Override
	public QueryTokenStream visitGenericFunctionArguments(HqlParser.GenericFunctionArgumentsContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.DISTINCT() != null) {
			builder.append(QueryTokens.expression(ctx.DISTINCT()));
		}

		if (ctx.datetimeField() != null) {
			builder.append(visit(ctx.datetimeField()));
			builder.append(TOKEN_COMMA);
		}

		builder.append(QueryTokenStream.concat(ctx.expressionOrPredicate(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitCollectionSizeFunction(HqlParser.CollectionSizeFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.SIZE()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.path()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitElementAggregateFunction(HqlParser.ElementAggregateFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.MAXELEMENT() != null || ctx.MINELEMENT() != null) {
			builder.append(QueryTokens.token(ctx.MAXELEMENT() != null ? ctx.MAXELEMENT() : ctx.MINELEMENT()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(visit(ctx.path()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else {

			if (ctx.MAX() != null) {
				builder.append(QueryTokens.token(ctx.MAX()));
			}
			if (ctx.MIN() != null) {
				builder.append(QueryTokens.token(ctx.MIN()));
			}
			if (ctx.SUM() != null) {
				builder.append(QueryTokens.token(ctx.SUM()));
			}
			if (ctx.AVG() != null) {
				builder.append(QueryTokens.token(ctx.AVG()));
			}

			builder.append(TOKEN_OPEN_PAREN);
			builder.append(visit(ctx.elementsValuesQuantifier()));
			builder.append(TOKEN_OPEN_PAREN);

			if (ctx.path() != null) {
				builder.append(visit(ctx.path()));
			}

			builder.append(TOKEN_CLOSE_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitIndexAggregateFunction(HqlParser.IndexAggregateFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.MAXINDEX() != null || ctx.MININDEX() != null) {
			builder.append(QueryTokens.token(ctx.MAXINDEX() != null ? ctx.MAXINDEX() : ctx.MININDEX()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.append(visit(ctx.path()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else {

			if (ctx.MAX() != null) {
				builder.append(QueryTokens.token(ctx.MAX()));
			}
			if (ctx.MIN() != null) {
				builder.append(QueryTokens.token(ctx.MIN()));
			}
			if (ctx.SUM() != null) {
				builder.append(QueryTokens.token(ctx.SUM()));
			}
			if (ctx.AVG() != null) {
				builder.append(QueryTokens.token(ctx.AVG()));
			}

			builder.append(TOKEN_OPEN_PAREN);
			builder.append(visit(ctx.indicesKeysQuantifier()));
			builder.append(TOKEN_OPEN_PAREN);

			if (ctx.path() != null) {
				builder.append(visit(ctx.path()));
			}

			builder.append(TOKEN_CLOSE_PAREN);
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCollectionFunctionMisuse(HqlParser.CollectionFunctionMisuseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(
				visit(ctx.elementsValuesQuantifier() != null ? ctx.elementsValuesQuantifier() : ctx.indicesKeysQuantifier()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.path()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitAggregateFunction(HqlParser.AggregateFunctionContext ctx) {

		if (ctx.everyFunction() != null) {
			return visit(ctx.everyFunction());
		}

		if (ctx.anyFunction() != null) {
			return visit(ctx.anyFunction());
		}

		return visit(ctx.listaggFunction());
	}

	@Override
	public QueryTokenStream visitEveryAllQuantifier(HqlParser.EveryAllQuantifierContext ctx) {

		if (ctx.EVERY() != null) {
			return QueryRenderer.from(QueryTokens.token(ctx.EVERY()));
		}

		return QueryRenderer.from(QueryTokens.token(ctx.ALL()));
	}

	@Override
	public QueryTokenStream visitAnySomeQuantifier(HqlParser.AnySomeQuantifierContext ctx) {

		if (ctx.ANY() != null) {
			return QueryRenderer.from(QueryTokens.token(ctx.ANY()));
		}

		return QueryRenderer.from(QueryTokens.token(ctx.SOME()));
	}

	@Override
	public QueryTokenStream visitListaggFunction(HqlParser.ListaggFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.LISTAGG()));
		builder.append(TOKEN_OPEN_PAREN);

		QueryRendererBuilder nested = QueryRenderer.builder();

		if (ctx.DISTINCT() != null) {
			builder.append(QueryTokens.expression(ctx.DISTINCT()));
		}

		builder.appendInline(visit(ctx.expressionOrPredicate(0)));
		builder.append(TOKEN_COMMA);
		builder.appendInline(visit(ctx.expressionOrPredicate(1)));

		if (ctx.onOverflowClause() != null) {
			builder.appendExpression(visit(ctx.onOverflowClause()));
		}

		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.withinGroupClause() != null) {
			builder.appendExpression(visit(ctx.withinGroupClause()));
		}

		if (ctx.filterClause() != null) {
			builder.appendExpression(visit(ctx.filterClause()));
		}

		if (ctx.overClause() != null) {
			builder.appendExpression(visit(ctx.overClause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitOnOverflowClause(HqlParser.OnOverflowClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.ON()));
		builder.append(QueryTokens.expression(ctx.OVERFLOW()));

		if (ctx.ERROR() != null) {
			builder.append(QueryTokens.expression(ctx.ERROR()));
		} else {

			builder.append(QueryTokens.expression(ctx.TRUNCATE()));

			if (ctx.expression() != null) {
				builder.appendExpression(visit(ctx.expression()));
			}

			if (ctx.WITH() != null) {
				builder.append(QueryTokens.expression(ctx.WITH()));
			}

			if (ctx.WITHOUT() != null) {
				builder.append(QueryTokens.expression(ctx.WITHOUT()));
			}

			if (ctx.COUNT() != null) {
				builder.append(QueryTokens.expression(ctx.COUNT()));
			}
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitWithinGroupClause(HqlParser.WithinGroupClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WITHIN()));
		builder.append(QueryTokens.expression(ctx.GROUP()));

		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.orderByClause()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitNullsClause(HqlParser.NullsClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.IGNORE() != null) {
			builder.append(QueryTokens.expression(ctx.IGNORE()));
		} else {
			builder.append(QueryTokens.expression(ctx.RESPECT()));
		}

		builder.append(QueryTokens.expression(ctx.NULLS()));

		return builder;
	}

	@Override
	public QueryTokenStream visitNthSideClause(HqlParser.NthSideClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.FROM()));

		if (ctx.FIRST() != null) {
			builder.append(QueryTokens.expression(ctx.FIRST()));
		} else {
			builder.append(QueryTokens.expression(ctx.LAST()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFrameStart(HqlParser.FrameStartContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CURRENT() != null) {

			builder.append(QueryTokens.expression(ctx.CURRENT()));
			builder.append(QueryTokens.expression(ctx.ROW()));
		} else if (ctx.UNBOUNDED() != null) {
			builder.append(QueryTokens.expression(ctx.UNBOUNDED()));
			builder.append(QueryTokens.expression(ctx.PRECEDING()));
		} else {

			builder.appendExpression(visit(ctx.expression()));
			builder.append(QueryTokens.expression(ctx.PRECEDING() != null ? ctx.PRECEDING() : ctx.FOLLOWING()));
		}

		return builder;

	}

	@Override
	public QueryTokenStream visitFrameEnd(HqlParser.FrameEndContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.CURRENT() != null) {

			builder.append(QueryTokens.expression(ctx.CURRENT()));
			builder.append(QueryTokens.expression(ctx.ROW()));
		} else if (ctx.UNBOUNDED() != null) {
			builder.append(QueryTokens.expression(ctx.UNBOUNDED()));
			builder.append(QueryTokens.expression(ctx.FOLLOWING()));
		} else {

			builder.appendExpression(visit(ctx.expression()));
			builder.append(QueryTokens.expression(ctx.PRECEDING() != null ? ctx.PRECEDING() : ctx.FOLLOWING()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFrameExclusion(HqlParser.FrameExclusionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.EXCLUDE()));

		if (ctx.CURRENT() != null) {
			builder.append(QueryTokens.expression(ctx.CURRENT()));
			builder.append(QueryTokens.expression(ctx.ROW()));
		} else if (ctx.GROUP() != null) {
			builder.append(QueryTokens.expression(ctx.GROUP()));
		} else if (ctx.TIES() != null) {
			builder.append(QueryTokens.expression(ctx.TIES()));
		} else {
			builder.append(QueryTokens.expression(ctx.NO()));
			builder.append(QueryTokens.expression(ctx.OTHERS()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCollectionQuantifier(HqlParser.CollectionQuantifierContext ctx) {

		if (ctx.elementsValuesQuantifier() != null) {
			return visit(ctx.elementsValuesQuantifier());
		}

		return visit(ctx.indicesKeysQuantifier());
	}

	@Override
	public QueryTokenStream visitElementsValuesQuantifier(HqlParser.ElementsValuesQuantifierContext ctx) {
		return QueryRenderer.from(QueryTokens.token(ctx.ELEMENTS() != null ? ctx.ELEMENTS() : ctx.VALUES()));
	}

	@Override
	public QueryTokenStream visitIndicesKeysQuantifier(HqlParser.IndicesKeysQuantifierContext ctx) {
		return QueryRenderer.from(QueryTokens.token(ctx.INDICES() != null ? ctx.INDICES() : ctx.KEYS()));
	}

	@Override
	public QueryTokenStream visitGeneralPathExpression(HqlParser.GeneralPathExpressionContext ctx) {
		return visit(ctx.generalPathFragment());
	}

	@Override
	public QueryTokenStream visitPath(HqlParser.PathContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.treatedPath() != null) {

			builder.append(visit(ctx.treatedPath()));

			if (ctx.pathContinutation() != null) {
				builder.append(visit(ctx.pathContinutation()));
			}
		} else if (ctx.generalPathFragment() != null) {
			builder.append(visit(ctx.generalPathFragment()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitGeneralPathFragment(HqlParser.GeneralPathFragmentContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.simplePath()));

		if (ctx.indexedPathAccessFragment() != null) {
			builder.append(visit(ctx.indexedPathAccessFragment()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitIndexedPathAccessFragment(HqlParser.IndexedPathAccessFragmentContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_SQUARE_BRACKET);
		builder.appendInline(visit(ctx.expression()));
		builder.append(TOKEN_CLOSE_SQUARE_BRACKET);

		if (ctx.generalPathFragment() != null) {

			builder.append(TOKEN_DOT);
			builder.append(visit(ctx.generalPathFragment()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSimplePath(HqlParser.SimplePathContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.identifier()));

		if (!ctx.simplePathElement().isEmpty()) {
			builder.append(TOKEN_DOT);
		}

		builder.append(QueryTokenStream.concat(ctx.simplePathElement(), this::visit, TOKEN_DOT));

		return builder;
	}

	@Override
	public QueryTokenStream visitSimplePathElement(HqlParser.SimplePathElementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.identifier()));

		return builder;
	}

	@Override
	public QueryTokenStream visitCaseList(HqlParser.CaseListContext ctx) {

		if (ctx.simpleCaseExpression() != null) {
			return visit(ctx.simpleCaseExpression());
		} else if (ctx.searchedCaseExpression() != null) {
			return visit(ctx.searchedCaseExpression());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitSimpleCaseExpression(HqlParser.SimpleCaseExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.CASE()));
		builder.append(visit(ctx.expressionOrPredicate(0)));

		ctx.caseWhenExpressionClause().forEach(caseWhenExpressionClauseContext -> {
			builder.append(visit(caseWhenExpressionClauseContext));
		});

		if (ctx.ELSE() != null) {

			builder.append(QueryTokens.expression(ctx.ELSE()));
			builder.append(visit(ctx.expressionOrPredicate(1)));
		}

		builder.append(QueryTokens.expression(ctx.END()));

		return builder;
	}

	@Override
	public QueryTokenStream visitSearchedCaseExpression(HqlParser.SearchedCaseExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.CASE()));

		builder.append(QueryTokenStream.concat(ctx.caseWhenPredicateClause(), this::visit, TOKEN_SPACE));

		if (ctx.ELSE() != null) {

			builder.append(QueryTokens.expression(ctx.ELSE()));
			builder.appendExpression(visit(ctx.expressionOrPredicate()));
		}

		builder.append(QueryTokens.expression(ctx.END()));

		return builder;
	}

	@Override
	public QueryTokenStream visitCaseWhenExpressionClause(HqlParser.CaseWhenExpressionClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WHEN()));
		builder.appendExpression(visit(ctx.expression()));
		builder.append(QueryTokens.expression(ctx.THEN()));
		builder.appendExpression(visit(ctx.expressionOrPredicate()));

		return builder;
	}

	@Override
	public QueryTokenStream visitCaseWhenPredicateClause(HqlParser.CaseWhenPredicateClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WHEN()));
		builder.appendExpression(visit(ctx.predicate()));
		builder.append(QueryTokens.expression(ctx.THEN()));
		builder.appendExpression(visit(ctx.expressionOrPredicate()));

		return builder;
	}

	@Override
	public QueryTokenStream visitGenericFunction(HqlParser.GenericFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		QueryRendererBuilder nested = QueryRenderer.builder();

		nested.append(visit(ctx.genericFunctionName()));
		nested.append(TOKEN_OPEN_PAREN);

		if (ctx.genericFunctionArguments() != null) {
			nested.appendInline(visit(ctx.genericFunctionArguments()));
		} else if (ctx.ASTERISK() != null) {
			nested.append(QueryTokens.token(ctx.ASTERISK()));
		}

		nested.append(TOKEN_CLOSE_PAREN);
		builder.append(nested);

		if (ctx.pathContinutation() != null) {
			builder.append(visit(ctx.pathContinutation()));
		}

		if (ctx.nthSideClause() != null) {
			builder.appendExpression(visit(ctx.nthSideClause()));
		}

		if (ctx.nullsClause() != null) {
			builder.appendExpression(visit(ctx.nullsClause()));
		}

		if (ctx.withinGroupClause() != null) {
			builder.appendExpression(visit(ctx.withinGroupClause()));
		}

		if (ctx.filterClause() != null) {
			builder.appendExpression(visit(ctx.filterClause()));
		}

		if (ctx.overClause() != null) {
			builder.appendExpression(visit(ctx.overClause()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitFilterClause(HqlParser.FilterClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.FILTER()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.whereClause()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitOverClause(HqlParser.OverClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(QueryTokens.expression(ctx.OVER()));

		QueryRendererBuilder nested = QueryRenderer.builder();
		nested.append(TOKEN_OPEN_PAREN);

		List<ParseTree> trees = new ArrayList<>();

		if (ctx.partitionClause() != null) {
			trees.add(ctx.partitionClause());
		}

		if (ctx.orderByClause() != null) {
			trees.add(ctx.orderByClause());
		}

		if (ctx.frameClause() != null) {
			trees.add(ctx.frameClause());
		}

		nested.appendInline(QueryTokenStream.concat(trees, this::visit, TOKEN_SPACE));
		nested.append(TOKEN_CLOSE_PAREN);

		builder.appendInline(nested);

		return builder;
	}

	@Override
	public QueryTokenStream visitPartitionClause(HqlParser.PartitionClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.PARTITION()));
		builder.append(QueryTokens.expression(ctx.BY()));

		builder.append(QueryTokenStream.concat(ctx.expression(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitFrameClause(HqlParser.FrameClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.RANGE() != null) {
			builder.append(QueryTokens.expression(ctx.RANGE()));
		} else if (ctx.ROWS() != null) {
			builder.append(QueryTokens.expression(ctx.ROWS()));
		} else if (ctx.GROUPS() != null) {
			builder.append(QueryTokens.expression(ctx.GROUPS()));
		}

		if (ctx.BETWEEN() != null) {
			builder.append(QueryTokens.expression(ctx.BETWEEN()));
		}

		builder.appendExpression(visit(ctx.frameStart()));

		if (ctx.AND() != null) {

			builder.append(QueryTokens.expression(ctx.AND()));
			builder.appendExpression(visit(ctx.frameEnd()));
		}

		if (ctx.frameExclusion() != null) {
			builder.appendExpression(visit(ctx.frameExclusion()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCastFunction(HqlParser.CastFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.CAST()));

		builder.append(TOKEN_OPEN_PAREN);

		QueryRendererBuilder nested = QueryRenderer.builder();
		nested.appendExpression(visit(ctx.expression()));
		nested.append(QueryTokens.expression(ctx.AS()));
		nested.appendExpression(visit(ctx.castTarget()));

		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitCastTarget(HqlParser.CastTargetContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.castTargetType()));

		if (ctx.INTEGER_LITERAL() != null && !ctx.INTEGER_LITERAL().isEmpty()) {

			builder.append(TOKEN_OPEN_PAREN);

			List<QueryToken> tokens = new ArrayList<>();
			ctx.INTEGER_LITERAL().forEach(terminalNode -> {

				if (!tokens.isEmpty()) {
					tokens.add(TOKEN_COMMA);
				}
				tokens.add(QueryTokens.expression(terminalNode));

			});

			builder.append(tokens);
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCastTargetType(HqlParser.CastTargetTypeContext ctx) {
		return QueryRendererBuilder.from(QueryTokens.expression(ctx.fullTargetName));
	}

	@Override
	public QueryTokenStream visitExtractFunction(HqlParser.ExtractFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.EXTRACT() != null) {

			builder.append(QueryTokens.token(ctx.EXTRACT()));
			builder.append(TOKEN_OPEN_PAREN);

			QueryRendererBuilder nested = QueryRenderer.builder();

			nested.appendExpression(visit(ctx.extractField()));
			nested.append(QueryTokens.expression(ctx.FROM()));
			nested.append(visit(ctx.expression()));

			builder.appendInline(nested);
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.datetimeField() != null) {

			builder.append(visit(ctx.datetimeField()));
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.expression()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitExtractField(HqlParser.ExtractFieldContext ctx) {

		if (ctx.datetimeField() != null) {
			return visit(ctx.datetimeField());
		}

		if (ctx.dayField() != null) {
			return visit(ctx.dayField());
		}

		if (ctx.weekField() != null) {
			return visit(ctx.weekField());
		}

		if (ctx.timeZoneField() != null) {
			return visit(ctx.timeZoneField());
		}

		if (ctx.dateOrTimeField() != null) {
			return visit(ctx.dateOrTimeField());
		}

		return QueryRenderer.builder();
	}

	@Override
	public QueryTokenStream visitTrimFunction(HqlParser.TrimFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.TRIM()));
		builder.append(TOKEN_OPEN_PAREN);

		if (ctx.trimSpecification() != null) {
			builder.appendExpression(visit(ctx.trimSpecification()));
		}

		if (ctx.trimCharacter() != null) {
			builder.appendExpression(visit(ctx.trimCharacter()));
		}

		if (ctx.FROM() != null) {
			builder.append(QueryTokens.expression(ctx.FROM()));
		}

		if (ctx.expression() != null) {
			builder.append(visit(ctx.expression()));
		}

		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitTrimSpecification(HqlParser.TrimSpecificationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.BOTH() != null) {
			builder.append(QueryTokens.expression(ctx.BOTH()));
		} else if (ctx.LEADING() != null) {
			builder.append(QueryTokens.expression(ctx.LEADING()));
		} else if (ctx.TRAILING() != null) {
			builder.append(QueryTokens.expression(ctx.TRAILING()));
		}

		return builder.build();
	}

	@Override
	public QueryTokenStream visitTrimCharacter(HqlParser.TrimCharacterContext ctx) {

		if (ctx.stringLiteral() != null) {
			return visit(ctx.stringLiteral());
		}

		return visit(ctx.parameter());
	}

	@Override
	public QueryTokenStream visitEveryFunction(HqlParser.EveryFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.everyAllQuantifier()));

		if (ctx.predicate() != null) {
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.predicate()));
			builder.append(TOKEN_CLOSE_PAREN);

			if (ctx.filterClause() != null) {
				builder.appendExpression(visit(ctx.filterClause()));
			}

			if (ctx.overClause() != null) {
				builder.appendExpression(visit(ctx.overClause()));
			}
		} else if (ctx.subquery() != null) {
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.subquery()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else {

			builder.appendExpression(visit(ctx.collectionQuantifier()));

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.simplePath()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitAnyFunction(HqlParser.AnyFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.anySomeQuantifier()));

		if (ctx.predicate() != null) {
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.predicate()));
			builder.append(TOKEN_CLOSE_PAREN);

			if (ctx.filterClause() != null) {
				builder.appendExpression(visit(ctx.filterClause()));
			}

			if (ctx.overClause() != null) {
				builder.appendExpression(visit(ctx.overClause()));
			}
		} else if (ctx.subquery() != null) {
			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.subquery()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else {

			builder.appendExpression(visit(ctx.collectionQuantifier()));

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.simplePath()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitTreatedPath(HqlParser.TreatedPathContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.TREAT()));
		builder.append(TOKEN_OPEN_PAREN);

		QueryRendererBuilder nested = QueryRenderer.builder();
		nested.appendExpression(visit(ctx.path()));
		nested.append(QueryTokens.expression(ctx.AS()));
		nested.append(visit(ctx.simplePath()));

		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinutation() != null) {
			builder.append(visit(ctx.pathContinutation()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitPathContinutation(HqlParser.PathContinutationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_DOT);
		builder.append(visit(ctx.simplePath()));

		return builder;
	}

	@Override
	public QueryTokenStream visitIsBooleanPredicate(HqlParser.IsBooleanPredicateContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expression()));
		builder.append(QueryTokens.expression(ctx.IS()));

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		if (ctx.NULL() != null) {
			builder.append(QueryTokens.expression(ctx.NULL()));
		}

		if (ctx.TRUE() != null) {
			builder.append(QueryTokens.expression(ctx.TRUE()));
		}

		if (ctx.FALSE() != null) {
			builder.append(QueryTokens.expression(ctx.FALSE()));
		}

		if (ctx.EMPTY() != null) {
			builder.append(QueryTokens.expression(ctx.EMPTY()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitMemberOfPredicate(HqlParser.MemberOfPredicateContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expression()));
		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}
		if (ctx.MEMBER() != null) {
			builder.append(QueryTokens.expression(ctx.MEMBER()));
		}
		if (ctx.OF() != null) {
			builder.append(QueryTokens.expression(ctx.OF()));
		}

		builder.append(visit(ctx.path()));

		return builder;
	}

	@Override
	public QueryTokenStream visitIsDistinctFromPredicate(HqlParser.IsDistinctFromPredicateContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expression(0)));
		builder.append(QueryTokens.expression(ctx.IS()));

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		if (ctx.DISTINCT() != null) {

			builder.append(QueryTokens.expression(ctx.DISTINCT()));
			builder.append(QueryTokens.expression(ctx.FROM()));
			builder.appendExpression(visit(ctx.expression(1)));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitBetweenPredicate(HqlParser.BetweenPredicateContext ctx) {
		return visit(ctx.betweenExpression());
	}

	@Override
	public QueryTokenStream visitContainsPredicate(HqlParser.ContainsPredicateContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expression(0)));

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		if (ctx.CONTAINS() != null) {
			builder.append(QueryTokens.expression(ctx.CONTAINS()));
		}
		if (ctx.INCLUDES() != null) {
			builder.append(QueryTokens.expression(ctx.INCLUDES()));
		}
		if (ctx.INTERSECTS() != null) {
			builder.append(QueryTokens.expression(ctx.INTERSECTS()));
		}

		builder.appendExpression(visit(ctx.expression(1)));

		return builder;

	}

	@Override
	public QueryTokenStream visitOrPredicate(HqlParser.OrPredicateContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.predicate(0)));
		builder.append(QueryTokens.expression(ctx.OR()));
		builder.appendExpression(visit(ctx.predicate(1)));

		return builder;
	}

	@Override
	public QueryTokenStream visitRelationalPredicate(HqlParser.RelationalPredicateContext ctx) {
		return visit(ctx.relationalExpression());
	}

	@Override
	public QueryTokenStream visitExistsPredicate(HqlParser.ExistsPredicateContext ctx) {
		return visit(ctx.existsExpression());
	}

	@Override
	public QueryTokenStream visitAndPredicate(HqlParser.AndPredicateContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.predicate(0)));
		builder.append(QueryTokens.expression(ctx.AND()));
		builder.appendExpression(visit(ctx.predicate(1)));

		return builder;
	}

	@Override
	public QueryTokenStream visitGroupedPredicate(HqlParser.GroupedPredicateContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.predicate()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitLikePredicate(HqlParser.LikePredicateContext ctx) {
		return visit(ctx.stringPatternMatching());
	}

	@Override
	public QueryTokenStream visitInPredicate(HqlParser.InPredicateContext ctx) {
		return visit(ctx.inExpression());
	}

	@Override
	public QueryTokenStream visitNotPredicate(HqlParser.NotPredicateContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_NOT);
		builder.append(visit(ctx.predicate()));

		return builder;
	}

	@Override
	public QueryTokenStream visitExpressionPredicate(HqlParser.ExpressionPredicateContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public QueryTokenStream visitExpressionOrPredicate(HqlParser.ExpressionOrPredicateContext ctx) {

		if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else if (ctx.predicate() != null) {
			return visit(ctx.predicate());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitRelationalExpression(HqlParser.RelationalExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.expression(0)));
		builder.append(QueryTokens.ventilated(ctx.op));
		builder.appendInline(visit(ctx.expression(1)));

		return builder;
	}

	@Override
	public QueryTokenStream visitBetweenExpression(HqlParser.BetweenExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expression(0)));

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		builder.append(QueryTokens.expression(ctx.BETWEEN()));
		builder.appendExpression(visit(ctx.expression(1)));
		builder.append(QueryTokens.expression(ctx.AND()));
		builder.appendExpression(visit(ctx.expression(2)));

		return builder;
	}

	@Override
	public QueryTokenStream visitStringPatternMatching(HqlParser.StringPatternMatchingContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expression(0)));

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		if (ctx.LIKE() != null) {
			builder.append(QueryTokens.expression(ctx.LIKE()));
		} else if (ctx.ILIKE() != null) {
			builder.append(QueryTokens.expression(ctx.ILIKE()));
		}

		builder.appendExpression(visit(ctx.expression(1)));

		if (ctx.ESCAPE() != null) {

			builder.append(QueryTokens.expression(ctx.ESCAPE()));

			if (ctx.stringLiteral() != null) {
				builder.appendExpression(visit(ctx.stringLiteral()));
			} else if (ctx.parameter() != null) {
				builder.appendExpression(visit(ctx.parameter()));
			}
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitInExpression(HqlParser.InExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expression()));

		if (ctx.NOT() != null) {
			builder.append(QueryTokens.expression(ctx.NOT()));
		}

		builder.append(QueryTokens.expression(ctx.IN()));
		builder.appendExpression(visit(ctx.inList()));

		return builder;
	}

	@Override
	public QueryTokenStream visitInList(HqlParser.InListContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.simplePath() != null) {

			if (ctx.ELEMENTS() != null) {
				builder.append(QueryTokens.expression(ctx.ELEMENTS()));
			} else if (ctx.INDICES() != null) {
				builder.append(QueryTokens.expression(ctx.INDICES()));
			}

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.simplePath()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.subquery() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.subquery()));
			builder.append(TOKEN_CLOSE_PAREN);
		} else if (ctx.parameter() != null) {
			builder.append(visit(ctx.parameter()));
		} else if (ctx.expressionOrPredicate() != null) {

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(QueryTokenStream.concat(ctx.expressionOrPredicate(), this::visit, TOKEN_COMMA));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitExistsExpression(HqlParser.ExistsExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.simplePath() != null) {

			builder.append(QueryTokens.expression(ctx.EXISTS()));

			if (ctx.ELEMENTS() != null) {
				builder.append(QueryTokens.expression(ctx.ELEMENTS()));
			} else if (ctx.INDICES() != null) {
				builder.append(QueryTokens.expression(ctx.INDICES()));
			}

			builder.append(TOKEN_OPEN_PAREN);
			builder.append(visit(ctx.simplePath()));
			builder.append(TOKEN_CLOSE_PAREN);

		} else if (ctx.expression() != null) {

			builder.append(QueryTokens.expression(ctx.EXISTS()));
			builder.appendExpression(visit(ctx.expression()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitInstantiationTarget(HqlParser.InstantiationTargetContext ctx) {

		if (ctx.LIST() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.LIST()));
		} else if (ctx.MAP() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.MAP()));
		} else if (ctx.simplePath() != null) {

			return visit(ctx.simplePath());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitInstantiationArguments(HqlParser.InstantiationArgumentsContext ctx) {
		return QueryTokenStream.concat(ctx.instantiationArgument(), this::visit, TOKEN_COMMA);
	}

	@Override
	public QueryTokenStream visitInstantiationArgument(HqlParser.InstantiationArgumentContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.expressionOrPredicate() != null) {
			builder.appendExpression(visit(ctx.expressionOrPredicate()));
		} else if (ctx.instantiation() != null) {
			builder.appendExpression(visit(ctx.instantiation()));
		}

		if (ctx.variable() != null) {
			builder.append(visit(ctx.variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitParameterOrIntegerLiteral(HqlParser.ParameterOrIntegerLiteralContext ctx) {

		if (ctx.parameter() != null) {
			return visit(ctx.parameter());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.INTEGER_LITERAL()));
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitParameterOrNumberLiteral(HqlParser.ParameterOrNumberLiteralContext ctx) {

		if (ctx.parameter() != null) {
			return visit(ctx.parameter());
		} else if (ctx.numericLiteral() != null) {
			return visit(ctx.numericLiteral());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitVariable(HqlParser.VariableContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.identifier() != null) {

			builder.append(QueryTokens.expression(ctx.AS()));
			builder.append(visit(ctx.identifier()));
		} else if (ctx.reservedWord() != null) {
			builder.append(visit(ctx.reservedWord()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitParameter(HqlParser.ParameterContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.prefix.getText().equals(":")) {

			builder.append(TOKEN_COLON);
			builder.append(visit(ctx.identifier()));
		} else if (ctx.prefix.getText().equals("?")) {

			builder.append(TOKEN_QUESTION_MARK);

			if (ctx.INTEGER_LITERAL() != null) {
				builder.append(QueryTokens.expression(ctx.INTEGER_LITERAL()));
			}
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitEntityName(HqlParser.EntityNameContext ctx) {
		return QueryTokenStream.concat(ctx.identifier(), this::visit, TOKEN_DOT);
	}

	@Override
	public QueryTokenStream visitIdentifier(HqlParser.IdentifierContext ctx) {

		if (ctx.reservedWord() != null) {
			return visit(ctx.reservedWord());
		} else {
			return QueryTokenStream.empty();
		}
	}

	@Override
	public QueryTokenStream visitReservedWord(HqlParser.ReservedWordContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.IDENTIFICATION_VARIABLE()));
		} else {
			return QueryRendererBuilder.from(QueryTokens.token(ctx.f));
		}
	}
}
