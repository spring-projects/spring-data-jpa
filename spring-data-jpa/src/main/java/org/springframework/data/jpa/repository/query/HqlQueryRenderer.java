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

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that renders an HQL query without making any changes.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class HqlQueryRenderer extends HqlBaseVisitor<List<JpaQueryParsingToken>> {

	@Override
	public List<JpaQueryParsingToken> visitStart(HqlParser.StartContext ctx) {
		return visit(ctx.ql_statement());
	}

	@Override
	public List<JpaQueryParsingToken> visitQl_statement(HqlParser.Ql_statementContext ctx) {

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
	public List<JpaQueryParsingToken> visitSelectStatement(HqlParser.SelectStatementContext ctx) {
		return visit(ctx.queryExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitQueryExpression(HqlParser.QueryExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.withClause() != null) {
			tokens.addAll(visit(ctx.withClause()));
		}

		tokens.addAll(visit(ctx.orderedQuery(0)));

		for (int i = 1; i < ctx.orderedQuery().size(); i++) {

			tokens.addAll(visit(ctx.setOperator(i - 1)));
			tokens.addAll(visit(ctx.orderedQuery(i)));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitWithClause(HqlParser.WithClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_WITH);

		ctx.cte().forEach(cteContext -> {

			tokens.addAll(visit(cteContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCte(HqlParser.CteContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.identifier()));
		tokens.add(TOKEN_AS);
		NOSPACE(tokens);

		if (ctx.NOT() != null) {
			tokens.add(TOKEN_NOT);
		}
		if (ctx.MATERIALIZED() != null) {
			tokens.add(TOKEN_MATERIALIZED);
		}

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.queryExpression()));
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.searchClause() != null) {
			tokens.addAll(visit(ctx.searchClause()));
		}
		if (ctx.cycleClause() != null) {
			tokens.addAll(visit(ctx.cycleClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSearchClause(HqlParser.SearchClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SEARCH().getText()));

		if (ctx.BREADTH() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.BREADTH().getText()));
		} else if (ctx.DEPTH() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DEPTH().getText()));
		}

		tokens.add(new JpaQueryParsingToken(ctx.FIRST().getText()));
		tokens.add(new JpaQueryParsingToken(ctx.BY().getText()));
		tokens.addAll(visit(ctx.searchSpecifications()));
		tokens.add(new JpaQueryParsingToken(ctx.SET().getText()));
		tokens.addAll(visit(ctx.identifier()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSearchSpecifications(HqlParser.SearchSpecificationsContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		ctx.searchSpecification().forEach(searchSpecificationContext -> {

			tokens.addAll(visit(searchSpecificationContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSearchSpecification(HqlParser.SearchSpecificationContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.identifier()));

		if (ctx.sortDirection() != null) {
			tokens.addAll(visit(ctx.sortDirection()));
		}

		if (ctx.nullsPrecedence() != null) {
			tokens.addAll(visit(ctx.nullsPrecedence()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCycleClause(HqlParser.CycleClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CYCLE().getText()));
		tokens.addAll(visit(ctx.cteAttributes()));
		tokens.add(new JpaQueryParsingToken(ctx.SET().getText()));
		tokens.addAll(visit(ctx.identifier(0)));

		if (ctx.TO() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.TO().getText()));
			tokens.addAll(visit(ctx.literal(0)));
			tokens.add(new JpaQueryParsingToken(ctx.DEFAULT().getText()));
			tokens.addAll(visit(ctx.literal(1)));
		}

		if (ctx.USING() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.USING().getText()));
			tokens.addAll(visit(ctx.identifier(1)));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCteAttributes(HqlParser.CteAttributesContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		ctx.identifier().forEach(identifierContext -> {

			tokens.addAll(visit(identifierContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
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

		if (ctx.queryOrder() != null) {
			tokens.addAll(visit(ctx.queryOrder()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelectQuery(HqlParser.SelectQueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitFromQuery(HqlParser.FromQueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.fromClause()));

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

		tokens.addAll(visit(ctx.orderByClause()));

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
	public List<JpaQueryParsingToken> visitFromClause(HqlParser.FromClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		// TODO: Read up on Framework's LeastRecentlyUsedCache
		tokens.add(new JpaQueryParsingToken(ctx.FROM()));

		ctx.entityWithJoins().forEach(entityWithJoinsContext -> {
			tokens.addAll(visit(entityWithJoinsContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitEntityWithJoins(HqlParser.EntityWithJoinsContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.fromRoot()));
		SPACE(tokens);

		ctx.joinSpecifier().forEach(joinSpecifierContext -> {
			tokens.addAll(visit(joinSpecifierContext));
		});

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoinSpecifier(HqlParser.JoinSpecifierContext ctx) {

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
	public List<JpaQueryParsingToken> visitFromRoot(HqlParser.FromRootContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.entityName() != null) {

			tokens.addAll(visit(ctx.entityName()));

			if (ctx.variable() != null) {
				tokens.addAll(visit(ctx.variable()));
			}
			NOSPACE(tokens);
		} else if (ctx.subquery() != null) {

			if (ctx.LATERAL() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.LATERAL()));
			}
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			tokens.add(TOKEN_CLOSE_PAREN);

			if (ctx.variable() != null) {
				tokens.addAll(visit(ctx.variable()));
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin(HqlParser.JoinContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.joinType()));
		tokens.add(new JpaQueryParsingToken(ctx.JOIN()));

		if (ctx.FETCH() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FETCH()));
		}

		tokens.addAll(visit(ctx.joinTarget()));

		if (ctx.joinRestriction() != null) {
			tokens.addAll(visit(ctx.joinRestriction()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoinPath(HqlParser.JoinPathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.path()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoinSubquery(HqlParser.JoinSubqueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LATERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LATERAL()));
		}

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitUpdateStatement(HqlParser.UpdateStatementContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.UPDATE()));

		if (ctx.VERSIONED() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.VERSIONED()));
		}

		tokens.addAll(visit(ctx.targetEntity()));
		tokens.addAll(visit(ctx.setClause()));

		if (ctx.whereClause() != null) {
			tokens.addAll(visit(ctx.whereClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTargetEntity(HqlParser.TargetEntityContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.entityName()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSetClause(HqlParser.SetClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SET()));

		ctx.assignment().forEach(assignmentContext -> {
			tokens.addAll(visit(assignmentContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitAssignment(HqlParser.AssignmentContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.simplePath()));
		tokens.add(TOKEN_EQUALS);
		tokens.addAll(visit(ctx.expressionOrPredicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDeleteStatement(HqlParser.DeleteStatementContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.DELETE()));

		if (ctx.FROM() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FROM()));
		}

		tokens.addAll(visit(ctx.targetEntity()));

		if (ctx.whereClause() != null) {
			tokens.addAll(visit(ctx.whereClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInsertStatement(HqlParser.InsertStatementContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.INSERT()));

		if (ctx.INTO() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INTO()));
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
	public List<JpaQueryParsingToken> visitTargetFields(HqlParser.TargetFieldsContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);

		ctx.simplePath().forEach(simplePathContext -> {
			tokens.addAll(visit(simplePathContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitValuesList(HqlParser.ValuesListContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.VALUES()));

		ctx.values().forEach(valuesContext -> {
			tokens.addAll(visit(valuesContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitValues(HqlParser.ValuesContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);

		ctx.expression().forEach(expressionContext -> {
			tokens.addAll(visit(expressionContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInstantiation(HqlParser.InstantiationContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.NEW()));
		tokens.addAll(visit(ctx.instantiationTarget()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.instantiationArguments()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitAlias(HqlParser.AliasContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.AS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.AS()));
		}

		tokens.addAll(visit(ctx.identifier()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGroupedItem(HqlParser.GroupedItemContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
		} else if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitSortedItem(HqlParser.SortedItemContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitSortExpression(HqlParser.SortExpressionContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
		} else if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitSortDirection(HqlParser.SortDirectionContext ctx) {

		if (ctx.ASC() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ASC()));
		} else if (ctx.DESC() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DESC()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitNullsPrecedence(HqlParser.NullsPrecedenceContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.NULLS()));

		if (ctx.FIRST() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FIRST()));
		} else if (ctx.LAST() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LAST()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitLimitClause(HqlParser.LimitClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.LIMIT()));
		tokens.addAll(visit(ctx.parameterOrIntegerLiteral()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOffsetClause(HqlParser.OffsetClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.OFFSET()));
		tokens.addAll(visit(ctx.parameterOrIntegerLiteral()));

		if (ctx.ROW() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ROW()));
		} else if (ctx.ROWS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ROWS()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFetchClause(HqlParser.FetchClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.FETCH()));

		if (ctx.FIRST() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FIRST()));
		} else if (ctx.NEXT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.NEXT()));
		}

		if (ctx.parameterOrIntegerLiteral() != null) {
			tokens.addAll(visit(ctx.parameterOrIntegerLiteral()));
		} else if (ctx.parameterOrNumberLiteral() != null) {

			tokens.addAll(visit(ctx.parameterOrNumberLiteral()));
			tokens.add(TOKEN_PERCENT);
		}

		if (ctx.ROW() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ROW()));
		} else if (ctx.ROWS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ROWS()));
		}

		if (ctx.ONLY() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ONLY()));
		} else if (ctx.WITH() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.WITH()));
			tokens.add(new JpaQueryParsingToken(ctx.TIES()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSubquery(HqlParser.SubqueryContext ctx) {
		return visit(ctx.queryExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitSelectClause(HqlParser.SelectClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SELECT()));

		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}

		tokens.addAll(visit(ctx.selectionList()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelectionList(HqlParser.SelectionListContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		ctx.selection().forEach(selectionContext -> {
			tokens.addAll(visit(selectionContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelection(HqlParser.SelectionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.selectExpression()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelectExpression(HqlParser.SelectExpressionContext ctx) {

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
	public List<JpaQueryParsingToken> visitMapEntrySelection(HqlParser.MapEntrySelectionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ENTRY()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJpaSelectObjectSyntax(HqlParser.JpaSelectObjectSyntaxContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.OBJECT(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.identifier()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitWhereClause(HqlParser.WhereClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WHERE()));

		ctx.predicate().forEach(predicateContext -> {
			tokens.addAll(visit(predicateContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoinType(HqlParser.JoinTypeContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.INNER() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INNER()));
		}
		if (ctx.LEFT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LEFT()));
		}
		if (ctx.RIGHT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.RIGHT()));
		}
		if (ctx.FULL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FULL()));
		}
		if (ctx.OUTER() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.OUTER()));
		}
		if (ctx.CROSS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.CROSS()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCrossJoin(HqlParser.CrossJoinContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CROSS()));
		tokens.add(new JpaQueryParsingToken(ctx.JOIN()));
		tokens.addAll(visit(ctx.entityName()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoinRestriction(HqlParser.JoinRestrictionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.ON() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ON()));
		} else if (ctx.WITH() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.WITH()));
		}

		tokens.addAll(visit(ctx.predicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_COMMA);
		tokens.add(new JpaQueryParsingToken(ctx.IN(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGroupByClause(HqlParser.GroupByClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.GROUP()));
		tokens.add(new JpaQueryParsingToken(ctx.BY()));

		ctx.groupedItem().forEach(groupedItemContext -> {
			tokens.addAll(visit(groupedItemContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOrderByClause(HqlParser.OrderByClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ORDER()));
		tokens.add(new JpaQueryParsingToken(ctx.BY()));

		ctx.sortedItem().forEach(sortedItemContext -> {
			tokens.addAll(visit(sortedItemContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitHavingClause(HqlParser.HavingClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.HAVING()));

		ctx.predicate().forEach(predicateContext -> {
			tokens.addAll(visit(predicateContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSetOperator(HqlParser.SetOperatorContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.UNION() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.UNION()));
		} else if (ctx.INTERSECT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INTERSECT()));
		} else if (ctx.EXCEPT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.EXCEPT()));
		}

		if (ctx.ALL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ALL()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitLiteral(HqlParser.LiteralContext ctx) {

		if (ctx.NULL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NULL()));
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
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitBooleanLiteral(HqlParser.BooleanLiteralContext ctx) {

		if (ctx.TRUE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TRUE()));
		} else if (ctx.FALSE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FALSE()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitStringLiteral(HqlParser.StringLiteralContext ctx) {

		if (ctx.STRINGLITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.STRINGLITERAL()));
		} else if (ctx.CHARACTER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CHARACTER()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitNumericLiteral(HqlParser.NumericLiteralContext ctx) {

		if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
		} else if (ctx.FLOAT_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FLOAT_LITERAL()));
		} else if (ctx.HEXLITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.HEXLITERAL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitDateTimeLiteral(HqlParser.DateTimeLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LOCAL_DATE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LOCAL_DATE()));
		} else if (ctx.LOCAL_TIME() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LOCAL_TIME()));
		} else if (ctx.LOCAL_DATETIME() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LOCAL_DATETIME()));
		} else if (ctx.CURRENT_DATE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_DATE()));
		} else if (ctx.CURRENT_TIME() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_TIME()));
		} else if (ctx.CURRENT_TIMESTAMP() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_TIMESTAMP()));
		} else if (ctx.OFFSET_DATETIME() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.OFFSET_DATETIME()));
		} else {

			if (ctx.LOCAL() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.LOCAL()));
			} else if (ctx.CURRENT() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
			} else if (ctx.OFFSET() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.OFFSET()));
			}

			if (ctx.DATE() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.DATE()));
			} else if (ctx.TIME() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.TIME()));
			} else if (ctx.DATETIME() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.DATETIME()));
			}

			if (ctx.INSTANT() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.INSTANT()));
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDatetimeField(HqlParser.DatetimeFieldContext ctx) {

		if (ctx.YEAR() != null) {
			return List.of(new JpaQueryParsingToken(ctx.YEAR()));
		} else if (ctx.MONTH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MONTH()));
		} else if (ctx.DAY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DAY()));
		} else if (ctx.WEEK() != null) {
			return List.of(new JpaQueryParsingToken(ctx.WEEK()));
		} else if (ctx.QUARTER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.QUARTER()));
		} else if (ctx.HOUR() != null) {
			return List.of(new JpaQueryParsingToken(ctx.HOUR()));
		} else if (ctx.MINUTE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MINUTE()));
		} else if (ctx.SECOND() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SECOND()));
		} else if (ctx.NANOSECOND() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NANOSECOND()));
		} else if (ctx.EPOCH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.EPOCH()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitBinaryLiteral(HqlParser.BinaryLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.BINARY_LITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.BINARY_LITERAL()));
		} else if (ctx.HEXLITERAL() != null) {

			tokens.add(TOKEN_OPEN_BRACE);
			ctx.HEXLITERAL().forEach(terminalNode -> {
				tokens.add(new JpaQueryParsingToken(terminalNode));
				NOSPACE(tokens);
				tokens.add(TOKEN_COMMA);
			});
			CLIP(tokens);
			tokens.add(TOKEN_CLOSE_BRACE);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitPlainPrimaryExpression(HqlParser.PlainPrimaryExpressionContext ctx) {
		return visit(ctx.primaryExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitTupleExpression(HqlParser.TupleExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);

		ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {
			tokens.addAll(visit(expressionOrPredicateContext));
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitHqlConcatenationExpression(HqlParser.HqlConcatenationExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(TOKEN_DOUBLE_PIPE);
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGroupedExpression(HqlParser.GroupedExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.expression()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(new JpaQueryParsingToken(ctx.op));
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSignedNumericLiteral(HqlParser.SignedNumericLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.op));
		tokens.addAll(visit(ctx.numericLiteral()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		NOSPACE(tokens);
		tokens.add(new JpaQueryParsingToken(ctx.op, false));
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSubqueryExpression(HqlParser.SubqueryExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSignedExpression(HqlParser.SignedExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.op));
		tokens.addAll(visit(ctx.expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitToDurationExpression(HqlParser.ToDurationExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.addAll(visit(ctx.datetimeField()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFromDurationExpression(HqlParser.FromDurationExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.BY()));
		tokens.addAll(visit(ctx.datetimeField()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCaseExpression(HqlParser.CaseExpressionContext ctx) {
		return visit(ctx.caseList());
	}

	@Override
	public List<JpaQueryParsingToken> visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
		return visit(ctx.literal());
	}

	@Override
	public List<JpaQueryParsingToken> visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
		return visit(ctx.parameter());
	}

	@Override
	public List<JpaQueryParsingToken> visitFunctionExpression(HqlParser.FunctionExpressionContext ctx) {
		return visit(ctx.function());
	}

	@Override
	public List<JpaQueryParsingToken> visitGeneralPathExpression(HqlParser.GeneralPathExpressionContext ctx) {
		return visit(ctx.generalPathFragment());
	}

	@Override
	public List<JpaQueryParsingToken> visitIdentificationVariable(HqlParser.IdentificationVariableContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		} else if (ctx.simplePath() != null) {
			return visit(ctx.simplePath());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitPath(HqlParser.PathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.treatedPath() != null) {

			tokens.addAll(visit(ctx.treatedPath()));

			if (ctx.pathContinutation() != null) {
				NOSPACE(tokens);
				tokens.addAll(visit(ctx.pathContinutation()));
			}
		} else if (ctx.generalPathFragment() != null) {
			tokens.addAll(visit(ctx.generalPathFragment()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGeneralPathFragment(HqlParser.GeneralPathFragmentContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.simplePath()));

		if (ctx.indexedPathAccessFragment() != null) {
			tokens.addAll(visit(ctx.indexedPathAccessFragment()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitIndexedPathAccessFragment(HqlParser.IndexedPathAccessFragmentContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_SQUARE_BRACKET);
		tokens.addAll(visit(ctx.expression()));
		tokens.add(TOKEN_CLOSE_SQUARE_BRACKET);

		if (ctx.generalPathFragment() != null) {

			tokens.add(TOKEN_DOT);
			tokens.addAll(visit(ctx.generalPathFragment()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSimplePath(HqlParser.SimplePathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.identifier()));
		NOSPACE(tokens);

		ctx.simplePathElement().forEach(simplePathElementContext -> {
			tokens.addAll(visit(simplePathElementContext));
			NOSPACE(tokens);
		});
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSimplePathElement(HqlParser.SimplePathElementContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_DOT);
		tokens.addAll(visit(ctx.identifier()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCaseList(HqlParser.CaseListContext ctx) {

		if (ctx.simpleCaseExpression() != null) {
			return visit(ctx.simpleCaseExpression());
		} else if (ctx.searchedCaseExpression() != null) {
			return visit(ctx.searchedCaseExpression());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitSimpleCaseExpression(HqlParser.SimpleCaseExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CASE()));
		tokens.addAll(visit(ctx.expressionOrPredicate(0)));

		ctx.caseWhenExpressionClause().forEach(caseWhenExpressionClauseContext -> {
			tokens.addAll(visit(caseWhenExpressionClauseContext));
		});

		if (ctx.ELSE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.ELSE()));
			tokens.addAll(visit(ctx.expressionOrPredicate(1)));
		}

		tokens.add(new JpaQueryParsingToken(ctx.END()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSearchedCaseExpression(HqlParser.SearchedCaseExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CASE()));

		ctx.caseWhenPredicateClause().forEach(caseWhenPredicateClauseContext -> {
			tokens.addAll(visit(caseWhenPredicateClauseContext));
		});

		if (ctx.ELSE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.ELSE()));
			tokens.addAll(visit(ctx.expressionOrPredicate()));
		}

		tokens.add(new JpaQueryParsingToken(ctx.END()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCaseWhenExpressionClause(HqlParser.CaseWhenExpressionClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WHEN()));
		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.THEN()));
		tokens.addAll(visit(ctx.expressionOrPredicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCaseWhenPredicateClause(HqlParser.CaseWhenPredicateClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WHEN()));
		tokens.addAll(visit(ctx.predicate()));
		tokens.add(new JpaQueryParsingToken(ctx.THEN()));
		tokens.addAll(visit(ctx.expressionOrPredicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGenericFunction(HqlParser.GenericFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.functionName()));
		NOSPACE(tokens);
		tokens.add(TOKEN_OPEN_PAREN);

		if (ctx.functionArguments() != null) {
			tokens.addAll(visit(ctx.functionArguments()));
		} else if (ctx.ASTERISK() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ASTERISK()));
		}

		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinutation() != null) {
			NOSPACE(tokens);
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
	public List<JpaQueryParsingToken> visitFunctionWithSubquery(HqlParser.FunctionWithSubqueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.functionName()));
		NOSPACE(tokens);
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCastFunctionInvocation(HqlParser.CastFunctionInvocationContext ctx) {
		return visit(ctx.castFunction());
	}

	@Override
	public List<JpaQueryParsingToken> visitExtractFunctionInvocation(HqlParser.ExtractFunctionInvocationContext ctx) {
		return visit(ctx.extractFunction());
	}

	@Override
	public List<JpaQueryParsingToken> visitTrimFunctionInvocation(HqlParser.TrimFunctionInvocationContext ctx) {
		return visit(ctx.trimFunction());
	}

	@Override
	public List<JpaQueryParsingToken> visitEveryFunctionInvocation(HqlParser.EveryFunctionInvocationContext ctx) {
		return visit(ctx.everyFunction());
	}

	@Override
	public List<JpaQueryParsingToken> visitAnyFunctionInvocation(HqlParser.AnyFunctionInvocationContext ctx) {
		return visit(ctx.anyFunction());
	}

	@Override
	public List<JpaQueryParsingToken> visitTreatedPathInvocation(HqlParser.TreatedPathInvocationContext ctx) {
		return visit(ctx.treatedPath());
	}

	@Override
	public List<JpaQueryParsingToken> visitFunctionArguments(HqlParser.FunctionArgumentsContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}

		ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {
			tokens.addAll(visit(expressionOrPredicateContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFilterClause(HqlParser.FilterClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.FILTER()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.whereClause()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitWithinGroup(HqlParser.WithinGroupContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WITHIN()));
		tokens.add(new JpaQueryParsingToken(ctx.GROUP()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.orderByClause()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOverClause(HqlParser.OverClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.OVER()));
		tokens.add(TOKEN_OPEN_PAREN);

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
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitPartitionClause(HqlParser.PartitionClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.PARTITION()));
		tokens.add(new JpaQueryParsingToken(ctx.BY()));

		ctx.expression().forEach(expressionContext -> {
			tokens.addAll(visit(expressionContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFrameClause(HqlParser.FrameClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.RANGE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.RANGE()));
		} else if (ctx.ROWS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ROWS()));
		} else if (ctx.GROUPS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.GROUPS()));
		}

		if (ctx.BETWEEN() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.BETWEEN()));
		}

		tokens.addAll(visit(ctx.frameStart()));

		if (ctx.AND() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.AND()));
			tokens.addAll(visit(ctx.frameEnd()));
		}

		if (ctx.frameExclusion() != null) {
			tokens.addAll(visit(ctx.frameExclusion()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitUnboundedPrecedingFrameStart(
			HqlParser.UnboundedPrecedingFrameStartContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.UNBOUNDED()));
		tokens.add(new JpaQueryParsingToken(ctx.PRECEDING()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExpressionPrecedingFrameStart(
			HqlParser.ExpressionPrecedingFrameStartContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.PRECEDING()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCurrentRowFrameStart(HqlParser.CurrentRowFrameStartContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
		tokens.add(new JpaQueryParsingToken(ctx.ROW()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExpressionFollowingFrameStart(
			HqlParser.ExpressionFollowingFrameStartContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.FOLLOWING()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCurrentRowFrameExclusion(HqlParser.CurrentRowFrameExclusionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.EXCLUDE()));
		tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
		tokens.add(new JpaQueryParsingToken(ctx.ROW()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGroupFrameExclusion(HqlParser.GroupFrameExclusionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.EXCLUDE()));
		tokens.add(new JpaQueryParsingToken(ctx.GROUP()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTiesFrameExclusion(HqlParser.TiesFrameExclusionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.EXCLUDE()));
		tokens.add(new JpaQueryParsingToken(ctx.TIES()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitNoOthersFrameExclusion(HqlParser.NoOthersFrameExclusionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.EXCLUDE()));
		tokens.add(new JpaQueryParsingToken(ctx.NO()));
		tokens.add(new JpaQueryParsingToken(ctx.OTHERS()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExpressionPrecedingFrameEnd(HqlParser.ExpressionPrecedingFrameEndContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.PRECEDING()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCurrentRowFrameEnd(HqlParser.CurrentRowFrameEndContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
		tokens.add(new JpaQueryParsingToken(ctx.ROW()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExpressionFollowingFrameEnd(HqlParser.ExpressionFollowingFrameEndContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.FOLLOWING()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitUnboundedFollowingFrameEnd(HqlParser.UnboundedFollowingFrameEndContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.UNBOUNDED()));
		tokens.add(new JpaQueryParsingToken(ctx.FOLLOWING()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCastFunction(HqlParser.CastFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CAST(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.AS()));
		tokens.addAll(visit(ctx.castTarget()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCastTarget(HqlParser.CastTargetContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.castTargetType()));

		if (ctx.INTEGER_LITERAL() != null && !ctx.INTEGER_LITERAL().isEmpty()) {

			tokens.add(TOKEN_OPEN_PAREN);

			ctx.INTEGER_LITERAL().forEach(terminalNode -> {

				tokens.add(new JpaQueryParsingToken(terminalNode));
				tokens.add(TOKEN_COMMA);
			});
			CLIP(tokens);
			NOSPACE(tokens);

			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCastTargetType(HqlParser.CastTargetTypeContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.fullTargetName));
	}

	@Override
	public List<JpaQueryParsingToken> visitExtractFunction(HqlParser.ExtractFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.EXTRACT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.EXTRACT()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.expression(0)));
			tokens.add(new JpaQueryParsingToken(ctx.FROM()));
			tokens.addAll(visit(ctx.expression(1)));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.dateTimeFunction() != null) {

			tokens.addAll(visit(ctx.dateTimeFunction()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.expression(0)));
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTrimFunction(HqlParser.TrimFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.TRIM()));
		tokens.add(TOKEN_OPEN_PAREN);

		if (ctx.LEADING() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LEADING()));
		} else if (ctx.TRAILING() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.TRAILING()));
		} else if (ctx.BOTH() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.BOTH()));
		}

		if (ctx.stringLiteral() != null) {
			tokens.addAll(visit(ctx.stringLiteral()));
		}

		if (ctx.FROM() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FROM()));
		}

		tokens.addAll(visit(ctx.expression()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDateTimeFunction(HqlParser.DateTimeFunctionContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.d));
	}

	@Override
	public List<JpaQueryParsingToken> visitEveryFunction(HqlParser.EveryFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.every));

		if (ctx.ELEMENTS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
		} else if (ctx.INDICES() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
		}

		tokens.add(TOKEN_OPEN_PAREN);

		if (ctx.predicate() != null) {
			tokens.addAll(visit(ctx.predicate()));
		} else if (ctx.subquery() != null) {
			tokens.addAll(visit(ctx.subquery()));
		} else if (ctx.simplePath() != null) {
			tokens.addAll(visit(ctx.simplePath()));
		}

		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitAnyFunction(HqlParser.AnyFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.any));

		if (ctx.ELEMENTS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
		} else if (ctx.INDICES() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
		}

		tokens.add(TOKEN_OPEN_PAREN);

		if (ctx.predicate() != null) {
			tokens.addAll(visit(ctx.predicate()));
		} else if (ctx.subquery() != null) {
			tokens.addAll(visit(ctx.subquery()));
		} else if (ctx.simplePath() != null) {
			tokens.addAll(visit(ctx.simplePath()));
		}

		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTreatedPath(HqlParser.TreatedPathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.TREAT(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		tokens.add(new JpaQueryParsingToken(ctx.AS()));
		tokens.addAll(visit(ctx.simplePath()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinutation() != null) {
			NOSPACE(tokens);
			tokens.addAll(visit(ctx.pathContinutation()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitPathContinutation(HqlParser.PathContinutationContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_DOT);
		tokens.addAll(visit(ctx.simplePath()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitNullExpressionPredicate(HqlParser.NullExpressionPredicateContext ctx) {
		return visit(ctx.dealingWithNullExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitBetweenPredicate(HqlParser.BetweenPredicateContext ctx) {
		return visit(ctx.betweenExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitOrPredicate(HqlParser.OrPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.predicate(0)));
		tokens.add(new JpaQueryParsingToken(ctx.OR()));
		tokens.addAll(visit(ctx.predicate(1)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitRelationalPredicate(HqlParser.RelationalPredicateContext ctx) {
		return visit(ctx.relationalExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitExistsPredicate(HqlParser.ExistsPredicateContext ctx) {
		return visit(ctx.existsExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitCollectionPredicate(HqlParser.CollectionPredicateContext ctx) {
		return visit(ctx.collectionExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitAndPredicate(HqlParser.AndPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.predicate(0)));
		tokens.add(new JpaQueryParsingToken(ctx.AND()));
		tokens.addAll(visit(ctx.predicate(1)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGroupedPredicate(HqlParser.GroupedPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.predicate()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitLikePredicate(HqlParser.LikePredicateContext ctx) {
		return visit(ctx.stringPatternMatching());
	}

	@Override
	public List<JpaQueryParsingToken> visitInPredicate(HqlParser.InPredicateContext ctx) {
		return visit(ctx.inExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitNotPredicate(HqlParser.NotPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_NOT);
		tokens.addAll(visit(ctx.predicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExpressionPredicate(HqlParser.ExpressionPredicateContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitExpressionOrPredicate(HqlParser.ExpressionOrPredicateContext ctx) {

		if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else if (ctx.predicate() != null) {
			return visit(ctx.predicate());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitRelationalExpression(HqlParser.RelationalExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(new JpaQueryParsingToken(ctx.op));
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitBetweenExpression(HqlParser.BetweenExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));

		if (ctx.NOT() != null) {
			tokens.add(TOKEN_NOT);
		}

		tokens.add(new JpaQueryParsingToken(ctx.BETWEEN()));
		tokens.addAll(visit(ctx.expression(1)));
		tokens.add(new JpaQueryParsingToken(ctx.AND()));
		tokens.addAll(visit(ctx.expression(2)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDealingWithNullExpression(HqlParser.DealingWithNullExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(new JpaQueryParsingToken(ctx.IS()));

		if (ctx.NOT() != null) {
			tokens.add(TOKEN_NOT);
		}

		if (ctx.NULL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.NULL()));
		} else if (ctx.DISTINCT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
			tokens.add(new JpaQueryParsingToken(ctx.FROM()));
			tokens.addAll(visit(ctx.expression(1)));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitStringPatternMatching(HqlParser.StringPatternMatchingContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));

		if (ctx.NOT() != null) {
			tokens.add(TOKEN_NOT);
		}

		if (ctx.LIKE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LIKE()));
		} else if (ctx.ILIKE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ILIKE()));
		}

		tokens.addAll(visit(ctx.expression(1)));

		if (ctx.ESCAPE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.ESCAPE()));

			if (ctx.stringLiteral() != null) {
				tokens.addAll(visit(ctx.stringLiteral()));
			} else if (ctx.parameter() != null) {
				tokens.addAll(visit(ctx.parameter()));
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInExpression(HqlParser.InExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));

		if (ctx.NOT() != null) {
			tokens.add(TOKEN_NOT);
		}

		tokens.add(new JpaQueryParsingToken(ctx.IN()));
		tokens.addAll(visit(ctx.inList()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInList(HqlParser.InListContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.simplePath() != null) {

			if (ctx.ELEMENTS() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
			} else if (ctx.INDICES() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
			}

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.simplePath()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.subquery() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			NOSPACE(tokens);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.parameter() != null) {
			tokens.addAll(visit(ctx.parameter()));
		} else if (ctx.expressionOrPredicate() != null) {

			tokens.add(TOKEN_OPEN_PAREN);

			ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {
				tokens.addAll(visit(expressionOrPredicateContext));
				NOSPACE(tokens);
				tokens.add(TOKEN_COMMA);
			});
			CLIP(tokens);

			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExistsExpression(HqlParser.ExistsExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.simplePath() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.EXISTS()));

			if (ctx.ELEMENTS() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
			} else if (ctx.INDICES() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
			}

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.simplePath()));
			tokens.add(TOKEN_CLOSE_PAREN);

		} else if (ctx.expression() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.EXISTS()));
			tokens.addAll(visit(ctx.expression()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCollectionExpression(HqlParser.CollectionExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));

		if (ctx.IS() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.IS()));

			if (ctx.NOT() != null) {
				tokens.add(TOKEN_NOT);
			}

			tokens.add(new JpaQueryParsingToken(ctx.EMPTY()));
		} else if (ctx.MEMBER() != null) {

			if (ctx.NOT() != null) {
				tokens.add(TOKEN_NOT);
			}

			tokens.add(new JpaQueryParsingToken(ctx.MEMBER()));
			tokens.add(new JpaQueryParsingToken(ctx.OF()));
			tokens.addAll(visit(ctx.path()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInstantiationTarget(HqlParser.InstantiationTargetContext ctx) {

		if (ctx.LIST() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LIST()));
		} else if (ctx.MAP() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MAP()));
		} else if (ctx.simplePath() != null) {

			List<JpaQueryParsingToken> tokens = visit(ctx.simplePath());
			NOSPACE(tokens);
			return tokens;
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitInstantiationArguments(HqlParser.InstantiationArgumentsContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		ctx.instantiationArgument().forEach(instantiationArgumentContext -> {
			tokens.addAll(visit(instantiationArgumentContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInstantiationArgument(HqlParser.InstantiationArgumentContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

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
	public List<JpaQueryParsingToken> visitParameterOrIntegerLiteral(HqlParser.ParameterOrIntegerLiteralContext ctx) {

		if (ctx.parameter() != null) {
			return visit(ctx.parameter());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitParameterOrNumberLiteral(HqlParser.ParameterOrNumberLiteralContext ctx) {

		if (ctx.parameter() != null) {
			return visit(ctx.parameter());
		} else if (ctx.numericLiteral() != null) {
			return visit(ctx.numericLiteral());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitVariable(HqlParser.VariableContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identifier() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.AS()));
			tokens.addAll(visit(ctx.identifier()));
		} else if (ctx.reservedWord() != null) {
			tokens.addAll(visit(ctx.reservedWord()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitParameter(HqlParser.ParameterContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.prefix.getText().equals(":")) {

			tokens.add(TOKEN_COLON);
			tokens.addAll(visit(ctx.identifier()));
		} else if (ctx.prefix.getText().equals("?")) {

			tokens.add(TOKEN_QUESTION_MARK);

			if (ctx.INTEGER_LITERAL() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitEntityName(HqlParser.EntityNameContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		ctx.identifier().forEach(identifierContext -> {
			tokens.addAll(visit(identifierContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_DOT);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitIdentifier(HqlParser.IdentifierContext ctx) {

		if (ctx.reservedWord() != null) {
			return visit(ctx.reservedWord());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitCharacter(HqlParser.CharacterContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.CHARACTER()));
	}

	@Override
	public List<JpaQueryParsingToken> visitFunctionName(HqlParser.FunctionNameContext ctx) {
		return visit(ctx.reservedWord());
	}

	@Override
	public List<JpaQueryParsingToken> visitReservedWord(HqlParser.ReservedWordContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.IDENTIFICATION_VARIABLE()));
		} else {
			return List.of(new JpaQueryParsingToken(ctx.f));
		}
	}
}
