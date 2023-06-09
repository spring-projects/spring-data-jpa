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

import org.hibernate.grammars.hql.HqlParser;
import org.hibernate.grammars.hql.HqlParserBaseVisitor;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that renders an HQL query without making any changes.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class Hibernate62HqlQueryRenderer extends HqlParserBaseVisitor<List<JpaQueryParsingToken>> {

	@Override
	public List<JpaQueryParsingToken> visitStatement(HqlParser.StatementContext ctx) {

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
	public List<JpaQueryParsingToken> visitSubquery(HqlParser.SubqueryContext ctx) {
		return visit(ctx.queryExpression());
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
	public List<JpaQueryParsingToken> visitSetClause(HqlParser.SetClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SET()));

		ctx.assignment().forEach(assignmentContext -> {

			tokens.addAll(visit(assignmentContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		SPACE(tokens);

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
			NOSPACE(tokens);
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
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitValues(HqlParser.ValuesContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);

		ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {

			tokens.addAll(visit(expressionOrPredicateContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		tokens.add(TOKEN_CLOSE_PAREN);

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
	public List<JpaQueryParsingToken> visitSimpleQueryGroup(HqlParser.SimpleQueryGroupContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.withClause() != null) {
			tokens.addAll(visit(ctx.withClause()));
		}

		tokens.addAll(visit(ctx.orderedQuery()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSetQueryGroup(HqlParser.SetQueryGroupContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.withClause() != null) {
			tokens.addAll(visit(ctx.withClause()));
		}

		tokens.addAll(visit(ctx.orderedQuery(0)));

		for (int i = 0; i < ctx.setOperator().size(); i++) {

			tokens.addAll(visit(ctx.setOperator(i)));
			tokens.addAll(visit(ctx.orderedQuery(i + 1)));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitQuerySpecExpression(HqlParser.QuerySpecExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.query()));

		if (ctx.queryOrder() != null) {
			tokens.addAll(visit(ctx.queryOrder()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitNestedQueryExpression(HqlParser.NestedQueryExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.queryExpression()));
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.queryOrder() != null) {
			tokens.addAll(visit(ctx.queryOrder()));
		}

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
	public List<JpaQueryParsingToken> visitQuery(HqlParser.QueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.getChild(0) instanceof HqlParser.SelectClauseContext) {

			tokens.addAll(visit(ctx.selectClause()));

			if (ctx.fromClause() != null) {
				tokens.addAll(visit(ctx.fromClause()));
			}
		} else {
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

		if (ctx.getChild(0) instanceof HqlParser.FromClauseContext) {

			if (ctx.selectClause() != null) {
				tokens.addAll(visit(ctx.selectClause()));
			}
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

		ctx.children.forEach(parseTree -> {
			if (parseTree instanceof HqlParser.JoinContext //
					|| parseTree instanceof HqlParser.CrossJoinContext //
					|| parseTree instanceof HqlParser.JpaCollectionJoinContext) {
				tokens.addAll(visit(parseTree));
			}
		});

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitRootEntity(HqlParser.RootEntityContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.entityName()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}
		NOSPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitRootSubquery(HqlParser.RootSubqueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}
		NOSPACE(tokens);

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
	public List<JpaQueryParsingToken> visitVariable(HqlParser.VariableContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identifier() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.AS()));
			tokens.addAll(visit(ctx.identifier()));
		} else if (ctx.nakedIdentifier() != null) {
			tokens.addAll(visit(ctx.nakedIdentifier()));
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

		tokens.addAll(visit(ctx.instantiationArgumentExpression()));

		if (ctx.variable() != null) {
			tokens.addAll(visit(ctx.variable()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInstantiationArgumentExpression(
			HqlParser.InstantiationArgumentExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.expressionOrPredicate() != null) {
			tokens.addAll(visit(ctx.expressionOrPredicate()));
		} else if (ctx.instantiation() != null) {
			tokens.addAll(visit(ctx.instantiation()));
		}

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
	public List<JpaQueryParsingToken> visitPath(HqlParser.PathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.syntacticDomainPath() != null) {

			tokens.addAll(visit(ctx.syntacticDomainPath()));

			if (ctx.pathContinuation() != null) {
				NOSPACE(tokens);
				tokens.addAll(visit(ctx.pathContinuation()));
			}
		} else if (ctx.generalPathFragment() != null) {
			tokens.addAll(visit(ctx.generalPathFragment()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitPathContinuation(HqlParser.PathContinuationContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_DOT);
		tokens.addAll(visit(ctx.simplePath()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSyntacticDomainPath(HqlParser.SyntacticDomainPathContext ctx) {

		if (ctx.treatedNavigablePath() != null) {
			return visit(ctx.treatedNavigablePath());
		} else if (ctx.collectionValueNavigablePath() != null) {
			return visit(ctx.collectionValueNavigablePath());
		} else if (ctx.mapKeyNavigablePath() != null) {
			return visit(ctx.mapKeyNavigablePath());
		} else if (ctx.simplePath() != null) {

			List<JpaQueryParsingToken> tokens = new ArrayList<>();

			tokens.addAll(visit(ctx.simplePath()));
			tokens.addAll(visit(ctx.indexedPathAccessFragment()));

			return tokens;
		} else {
			return List.of();
		}
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
	public List<JpaQueryParsingToken> visitTreatedNavigablePath(HqlParser.TreatedNavigablePathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.TREAT(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		tokens.add(TOKEN_AS);
		tokens.addAll(visit(ctx.simplePath()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinuation() != null) {

			NOSPACE(tokens);
			tokens.addAll(visit(ctx.pathContinuation()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCollectionValueNavigablePath(
			HqlParser.CollectionValueNavigablePathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.VALUE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.VALUE()));
		} else if (ctx.ELEMENT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENT()));
		}

		NOSPACE(tokens);
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinuation() != null) {
			tokens.addAll(visit(ctx.pathContinuation()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitMapKeyNavigablePath(HqlParser.MapKeyNavigablePathContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.KEY() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.KEY(), false));
		} else if (ctx.INDEX() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INDEX(), false));
		}

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinuation() != null) {

			NOSPACE(tokens);
			tokens.addAll(visit(ctx.pathContinuation()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGroupByClause(HqlParser.GroupByClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.GROUP()));
		tokens.add(new JpaQueryParsingToken(ctx.BY()));

		ctx.groupByExpression().forEach(groupByExpressionContext -> {
			tokens.addAll(visit(groupByExpressionContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		SPACE(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGroupByExpression(HqlParser.GroupByExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.identifier() != null) {
			tokens.addAll(visit(ctx.identifier()));
		} else if (ctx.INTEGER_LITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
		} else if (ctx.expression() != null) {
			tokens.addAll(visit(ctx.expression()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitHavingClause(HqlParser.HavingClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.HAVING()));
		tokens.addAll(visit(ctx.predicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOrderByClause(HqlParser.OrderByClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ORDER()));
		tokens.add(new JpaQueryParsingToken(ctx.BY()));

		ctx.sortSpecification().forEach(sortedItemContext -> {
			tokens.addAll(visit(sortedItemContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOrderByFragment(HqlParser.OrderByFragmentContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		ctx.sortSpecification().forEach(sortedItemContext -> {
			tokens.addAll(visit(sortedItemContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSortSpecification(HqlParser.SortSpecificationContext ctx) {

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
	public List<JpaQueryParsingToken> visitCollateFunction(HqlParser.CollateFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.COLLATE()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.expression()));
		tokens.add(TOKEN_AS);
		tokens.addAll(visit(ctx.collation()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCollation(HqlParser.CollationContext ctx) {
		return visit(ctx.simplePath());
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
		} else if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
		} else if (ctx.FLOAT_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FLOAT_LITERAL()));
		} else if (ctx.DOUBLE_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DOUBLE_LITERAL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitWhereClause(HqlParser.WhereClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WHERE()));
		tokens.addAll(visit(ctx.predicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitBetweenPredicate(HqlParser.BetweenPredicateContext ctx) {

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
	public List<JpaQueryParsingToken> visitExistsPredicate(HqlParser.ExistsPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.EXISTS()));
		tokens.addAll(visit(ctx.expression()));

		return tokens;
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

		if (ctx.likeEscape() != null) {
			tokens.addAll(visit(ctx.likeEscape()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInPredicate(HqlParser.InPredicateContext ctx) {

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
	public List<JpaQueryParsingToken> visitComparisonPredicate(HqlParser.ComparisonPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.addAll(visit(ctx.comparisonOperator()));
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExistsCollectionPartPredicate(
			HqlParser.ExistsCollectionPartPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.EXISTS()));
		if (ctx.ELEMENTS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
		} else if (ctx.INDICES() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
		}
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.simplePath()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitNegatedPredicate(HqlParser.NegatedPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.NOT()));
		tokens.addAll(visit(ctx.predicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitBooleanExpressionPredicate(HqlParser.BooleanExpressionPredicateContext ctx) {
		return visit(ctx.expression());
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
	public List<JpaQueryParsingToken> visitMemberOfPredicate(HqlParser.MemberOfPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		if (ctx.NOT() != null) {
			tokens.add(TOKEN_NOT);
		}
		tokens.add(new JpaQueryParsingToken(ctx.MEMBER()));
		if (ctx.OF() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.OF()));
		}
		tokens.addAll(visit(ctx.path()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitIsEmptyPredicate(HqlParser.IsEmptyPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.IS()));
		if (ctx.NOT() != null) {
			tokens.add(TOKEN_NOT);
		}
		tokens.add(new JpaQueryParsingToken(ctx.EMPTY()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitIsNullPredicate(HqlParser.IsNullPredicateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.IS()));
		if (ctx.NOT() != null) {
			tokens.add(TOKEN_NOT);
		}
		tokens.add(new JpaQueryParsingToken(ctx.NULL()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitComparisonOperator(HqlParser.ComparisonOperatorContext ctx) {

		if (ctx.EQUAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.EQUAL()));
		} else if (ctx.NOT_EQUAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NOT_EQUAL()));
		} else if (ctx.GREATER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.GREATER()));
		} else if (ctx.GREATER_EQUAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.GREATER_EQUAL()));
		} else if (ctx.LESS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LESS()));
		} else if (ctx.LESS_EQUAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LESS_EQUAL()));
		} else if (ctx.IS() != null) {

			List<JpaQueryParsingToken> tokens = new ArrayList<>();

			tokens.add(new JpaQueryParsingToken(ctx.IS()));
			if (ctx.NOT() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.NOT()));
			}
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
			tokens.add(new JpaQueryParsingToken(ctx.FROM()));

			return tokens;
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitPersistentCollectionReferenceInList(
			HqlParser.PersistentCollectionReferenceInListContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.ELEMENTS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
		} else if (ctx.INDICES() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
		}
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.simplePath()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExplicitTupleInList(HqlParser.ExplicitTupleInListContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);

		ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {
			tokens.addAll(visit(expressionOrPredicateContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSubqueryInList(HqlParser.SubqueryInListContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitParamInList(HqlParser.ParamInListContext ctx) {
		return visit(ctx.parameter());
	}

	@Override
	public List<JpaQueryParsingToken> visitLikeEscape(HqlParser.LikeEscapeContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ESCAPE()));
		if (ctx.STRING_LITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
		} else if (ctx.JAVA_STRING_LITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.JAVA_STRING_LITERAL()));
		} else if (ctx.parameter() != null) {
			tokens.addAll(visit(ctx.parameter()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.addAll(visit(ctx.additiveOperator()));
		tokens.addAll(visit(ctx.expression(1)));

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
	public List<JpaQueryParsingToken> visitBarePrimaryExpression(HqlParser.BarePrimaryExpressionContext ctx) {
		return visit(ctx.primaryExpression());
	}

	@Override
	public List<JpaQueryParsingToken> visitTupleExpression(HqlParser.TupleExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);

		ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {

			tokens.addAll(visit(expressionOrPredicateContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);

		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitUnaryExpression(HqlParser.UnaryExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.signOperator()));
		tokens.addAll(visit(ctx.expression()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGroupedExpression(HqlParser.GroupedExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.expression()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitConcatenationExpression(HqlParser.ConcatenationExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		tokens.add(TOKEN_DOUBLE_PIPE);
		tokens.addAll(visit(ctx.expression(1)));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.expression(0)));
		NOSPACE(tokens);
		tokens.addAll(visit(ctx.multiplicativeOperator()));
		NOSPACE(tokens);
		tokens.addAll(visit(ctx.expression(1)));

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
	public List<JpaQueryParsingToken> visitSubqueryExpression(HqlParser.SubqueryExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.subquery()));
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitUnaryNumericLiteralExpression(
			HqlParser.UnaryNumericLiteralExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.signOperator()));
		tokens.addAll(visit(ctx.numericLiteral()));

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
	public List<JpaQueryParsingToken> visitEntityTypeExpression(HqlParser.EntityTypeExpressionContext ctx) {
		return visit(ctx.entityTypeReference());
	}

	@Override
	public List<JpaQueryParsingToken> visitEntityIdExpression(HqlParser.EntityIdExpressionContext ctx) {
		return visit(ctx.entityIdReference());
	}

	@Override
	public List<JpaQueryParsingToken> visitEntityVersionExpression(HqlParser.EntityVersionExpressionContext ctx) {
		return visit(ctx.entityVersionReference());
	}

	@Override
	public List<JpaQueryParsingToken> visitEntityNaturalIdExpression(HqlParser.EntityNaturalIdExpressionContext ctx) {
		return visit(ctx.entityNaturalIdReference());
	}

	@Override
	public List<JpaQueryParsingToken> visitToOneFkExpression(HqlParser.ToOneFkExpressionContext ctx) {
		return visit(ctx.toOneFkReference());
	}

	@Override
	public List<JpaQueryParsingToken> visitSyntacticPathExpression(HqlParser.SyntacticPathExpressionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.syntacticDomainPath()));
		if (ctx.pathContinuation() != null) {
			tokens.addAll(visit(ctx.pathContinuation()));
		}

		return tokens;
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
	public List<JpaQueryParsingToken> visitMultiplicativeOperator(HqlParser.MultiplicativeOperatorContext ctx) {

		if (ctx.SLASH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SLASH()));
		} else if (ctx.PERCENT_OP() != null) {
			return List.of(new JpaQueryParsingToken(ctx.PERCENT_OP()));
		} else if (ctx.ASTERISK() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ASTERISK()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitAdditiveOperator(HqlParser.AdditiveOperatorContext ctx) {

		if (ctx.PLUS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.PLUS()));
		} else if (ctx.MINUS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MINUS()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitSignOperator(HqlParser.SignOperatorContext ctx) {

		if (ctx.PLUS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.PLUS()));
		} else if (ctx.MINUS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MINUS()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitEntityTypeReference(HqlParser.EntityTypeReferenceContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.TYPE(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		if (ctx.path() != null) {
			tokens.addAll(visit(ctx.path()));
		} else if (ctx.parameter() != null) {
			tokens.addAll(visit(ctx.parameter()));
		}
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitEntityIdReference(HqlParser.EntityIdReferenceContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ID()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinuation() != null) {
			tokens.addAll(visit(ctx.pathContinuation()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitEntityVersionReference(HqlParser.EntityVersionReferenceContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.VERSION()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitEntityNaturalIdReference(HqlParser.EntityNaturalIdReferenceContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.NATURALID()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinuation() != null) {
			tokens.addAll(visit(ctx.pathContinuation()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitToOneFkReference(HqlParser.ToOneFkReferenceContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.FK()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCaseList(HqlParser.CaseListContext ctx) {

		if (ctx.simpleCaseList() != null) {
			return visit(ctx.simpleCaseList());
		} else if (ctx.searchedCaseList() != null) {
			return visit(ctx.searchedCaseList());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitSimpleCaseList(HqlParser.SimpleCaseListContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CASE()));
		tokens.addAll(visit(ctx.expressionOrPredicate()));
		ctx.simpleCaseWhen().forEach(simpleCaseWhenContext -> {
			tokens.addAll(visit(simpleCaseWhenContext));
		});
		if (ctx.caseOtherwise() != null) {
			tokens.addAll(visit(ctx.caseOtherwise()));
		}
		tokens.add(new JpaQueryParsingToken(ctx.END()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSimpleCaseWhen(HqlParser.SimpleCaseWhenContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WHEN()));
		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.THEN()));
		tokens.addAll(visit(ctx.expressionOrPredicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCaseOtherwise(HqlParser.CaseOtherwiseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ELSE()));
		tokens.addAll(visit(ctx.expressionOrPredicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSearchedCaseList(HqlParser.SearchedCaseListContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CASE()));
		ctx.searchedCaseWhen().forEach(searchedCaseWhenContext -> {
			tokens.addAll(visit(searchedCaseWhenContext));
		});
		if (ctx.caseOtherwise() != null) {
			tokens.addAll(visit(ctx.caseOtherwise()));
		}
		tokens.add(new JpaQueryParsingToken(ctx.END()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSearchedCaseWhen(HqlParser.SearchedCaseWhenContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WHEN()));
		tokens.addAll(visit(ctx.predicate()));
		tokens.add(new JpaQueryParsingToken(ctx.THEN()));
		tokens.addAll(visit(ctx.expressionOrPredicate()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitLiteral(HqlParser.LiteralContext ctx) {

		if (ctx.STRING_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
		} else if (ctx.JAVA_STRING_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.JAVA_STRING_LITERAL()));
		} else if (ctx.NULL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NULL()));
		} else if (ctx.booleanLiteral() != null) {
			return visit(ctx.booleanLiteral());
		} else if (ctx.numericLiteral() != null) {
			return visit(ctx.numericLiteral());
		} else if (ctx.binaryLiteral() != null) {
			return visit(ctx.binaryLiteral());
		} else if (ctx.temporalLiteral() != null) {
			return visit(ctx.temporalLiteral());
		} else if (ctx.generalizedLiteral() != null) {
			return visit(ctx.generalizedLiteral());
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
	public List<JpaQueryParsingToken> visitNumericLiteral(HqlParser.NumericLiteralContext ctx) {

		if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
		} else if (ctx.LONG_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LONG_LITERAL()));
		} else if (ctx.BIG_INTEGER_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.BIG_INTEGER_LITERAL()));
		} else if (ctx.FLOAT_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FLOAT_LITERAL()));
		} else if (ctx.DOUBLE_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DOUBLE_LITERAL()));
		} else if (ctx.BIG_DECIMAL_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.BIG_DECIMAL_LITERAL()));
		} else if (ctx.HEX_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.HEX_LITERAL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitBinaryLiteral(HqlParser.BinaryLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.BINARY_LITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.BINARY_LITERAL()));
		} else if (ctx.LEFT_BRACE() != null) {

			tokens.add(TOKEN_OPEN_BRACE);
			ctx.HEX_LITERAL().forEach(terminalNode -> {
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
	public List<JpaQueryParsingToken> visitTemporalLiteral(HqlParser.TemporalLiteralContext ctx) {

		if (ctx.dateTimeLiteral() != null) {
			return visit(ctx.dateTimeLiteral());
		} else if (ctx.dateLiteral() != null) {
			return visit(ctx.dateLiteral());
		} else if (ctx.timeLiteral() != null) {
			return visit(ctx.timeLiteral());
		} else if (ctx.jdbcTimestampLiteral() != null) {
			return visit(ctx.jdbcTimestampLiteral());
		} else if (ctx.jdbcDateLiteral() != null) {
			return visit(ctx.jdbcDateLiteral());
		} else if (ctx.jdbcTimeLiteral() != null) {
			return visit(ctx.jdbcTimeLiteral());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitDateTimeLiteral(HqlParser.DateTimeLiteralContext ctx) {

		if (ctx.localDateTimeLiteral() != null) {
			return visit(ctx.localDateTimeLiteral());
		} else if (ctx.zonedDateTimeLiteral() != null) {
			return visit(ctx.zonedDateTimeLiteral());
		} else if (ctx.offsetDateTimeLiteral() != null) {
			return visit(ctx.offsetDateTimeLiteral());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitLocalDateTimeLiteral(HqlParser.LocalDateTimeLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LEFT_BRACE() != null) {

			tokens.add(TOKEN_OPEN_BRACE);
			tokens.addAll(visit(ctx.localDateTime()));
			tokens.add(TOKEN_CLOSE_BRACE);
		} else if (ctx.DATETIME() != null) {

			if (ctx.LOCAL() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.LOCAL()));
			}
			tokens.add(new JpaQueryParsingToken(ctx.DATETIME()));
			tokens.addAll(visit(ctx.localDateTime()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitZonedDateTimeLiteral(HqlParser.ZonedDateTimeLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LEFT_BRACE() != null) {

			tokens.add(TOKEN_OPEN_BRACE);
			tokens.addAll(visit(ctx.zonedDateTime()));
			tokens.add(TOKEN_CLOSE_BRACE);
		} else if (ctx.DATETIME() != null) {

			if (ctx.ZONED() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.ZONED()));
			}
			tokens.add(new JpaQueryParsingToken(ctx.DATETIME()));
			tokens.addAll(visit(ctx.zonedDateTime()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOffsetDateTimeLiteral(HqlParser.OffsetDateTimeLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LEFT_BRACE() != null) {

			tokens.add(TOKEN_OPEN_BRACE);
			tokens.addAll(visit(ctx.offsetDateTime()));
			tokens.add(TOKEN_CLOSE_BRACE);
		} else if (ctx.DATETIME() != null) {

			if (ctx.OFFSET() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.OFFSET()));
			}
			tokens.add(new JpaQueryParsingToken(ctx.DATETIME()));
			tokens.addAll(visit(ctx.offsetDateTimeWithMinutes()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDateLiteral(HqlParser.DateLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LEFT_BRACE() != null) {

			tokens.add(TOKEN_OPEN_BRACE);
			tokens.addAll(visit(ctx.date()));
			tokens.add(TOKEN_CLOSE_BRACE);
		} else if (ctx.DATE() != null) {

			if (ctx.LOCAL() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.LOCAL()));
			}
			tokens.add(new JpaQueryParsingToken(ctx.DATE()));
			tokens.addAll(visit(ctx.date()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTimeLiteral(HqlParser.TimeLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LEFT_BRACE() != null) {

			tokens.add(TOKEN_OPEN_BRACE);
			tokens.addAll(visit(ctx.time()));
			tokens.add(TOKEN_CLOSE_BRACE);
		} else if (ctx.TIME() != null) {

			if (ctx.LOCAL() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.LOCAL()));
			}
			tokens.add(new JpaQueryParsingToken(ctx.TIME()));
			tokens.addAll(visit(ctx.time()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDateTime(HqlParser.DateTimeContext ctx) {

		if (ctx.localDateTime() != null) {
			return visit(ctx.localDateTime());
		} else if (ctx.zonedDateTime() != null) {
			return visit(ctx.zonedDateTime());
		} else if (ctx.offsetDateTime() != null) {
			return visit(ctx.offsetDateTime());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitLocalDateTime(HqlParser.LocalDateTimeContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.date()));
		tokens.addAll(visit(ctx.time()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitZonedDateTime(HqlParser.ZonedDateTimeContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.date()));
		tokens.addAll(visit(ctx.time()));
		tokens.addAll(visit(ctx.zoneId()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOffsetDateTime(HqlParser.OffsetDateTimeContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.date()));
		tokens.addAll(visit(ctx.time()));
		tokens.addAll(visit(ctx.offset()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOffsetDateTimeWithMinutes(HqlParser.OffsetDateTimeWithMinutesContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.date()));
		tokens.addAll(visit(ctx.time()));
		tokens.addAll(visit(ctx.offsetWithMinutes()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDate(HqlParser.DateContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.year()));
		tokens.add(new JpaQueryParsingToken(ctx.MINUS(0)));
		tokens.addAll(visit(ctx.month()));
		tokens.add(new JpaQueryParsingToken(ctx.MINUS(1)));
		tokens.addAll(visit(ctx.day()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTime(HqlParser.TimeContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.hour()));
		tokens.add(new JpaQueryParsingToken(ctx.COLON(0)));
		tokens.addAll(visit(ctx.minute()));

		if (ctx.second() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.COLON(1)));
			tokens.addAll(visit(ctx.second()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOffset(HqlParser.OffsetContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.PLUS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.PLUS()));
		} else if (ctx.MINUS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.MINUS()));
		}
		tokens.addAll(visit(ctx.hour()));

		if (ctx.COLON() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.COLON()));
			tokens.addAll(visit(ctx.minute()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOffsetWithMinutes(HqlParser.OffsetWithMinutesContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.PLUS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.PLUS()));
		} else if (ctx.MINUS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.MINUS()));
		}
		tokens.addAll(visit(ctx.hour()));
		tokens.add(new JpaQueryParsingToken(ctx.COLON()));
		tokens.addAll(visit(ctx.minute()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitYear(HqlParser.YearContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitMonth(HqlParser.MonthContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitDay(HqlParser.DayContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitHour(HqlParser.HourContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitMinute(HqlParser.MinuteContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitSecond(HqlParser.SecondContext ctx) {

		if (ctx.INTEGER_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
		} else if (ctx.FLOAT_LITERAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FLOAT_LITERAL()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitZoneId(HqlParser.ZoneIdContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.IDENTIFIER() != null) {

			ctx.IDENTIFIER().forEach(terminalNode -> {
				tokens.add(new JpaQueryParsingToken(terminalNode));
				NOSPACE(tokens);
				tokens.add(TOKEN_SLASH);
			});
			CLIP(tokens);
		} else if (ctx.STRING_LITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJdbcTimestampLiteral(HqlParser.JdbcTimestampLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.TIMESTAMP_ESCAPE_START()));

		if (ctx.dateTime() != null) {
			tokens.addAll(visit(ctx.dateTime()));
		} else if (ctx.genericTemporalLiteralText() != null) {
			tokens.addAll(visit(ctx.genericTemporalLiteralText()));
		}
		tokens.add(TOKEN_CLOSE_BRACE);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJdbcDateLiteral(HqlParser.JdbcDateLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.DATE_ESCAPE_START()));
		if (ctx.date() != null) {
			tokens.addAll(visit(ctx.date()));
		} else if (ctx.genericTemporalLiteralText() != null) {
			tokens.addAll(visit(ctx.genericTemporalLiteralText()));
		}
		tokens.add(TOKEN_CLOSE_BRACE);

		return tokens;
	}

	@Override

	public List<JpaQueryParsingToken> visitJdbcTimeLiteral(HqlParser.JdbcTimeLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.TIME_ESCAPE_START()));

		if (ctx.time() != null) {
			tokens.addAll(visit(ctx.time()));
		} else if (ctx.genericTemporalLiteralText() != null) {
			tokens.addAll(visit(ctx.genericTemporalLiteralText()));
		}
		tokens.add(TOKEN_CLOSE_BRACE);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGenericTemporalLiteralText(HqlParser.GenericTemporalLiteralTextContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitGeneralizedLiteral(HqlParser.GeneralizedLiteralContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(TOKEN_OPEN_BRACE);
		tokens.addAll(visit(ctx.generalizedLiteralType()));
		tokens.add(TOKEN_COLON);
		tokens.addAll(visit(ctx.generalizedLiteralType()));
		tokens.add(TOKEN_CLOSE_BRACE);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGeneralizedLiteralType(HqlParser.GeneralizedLiteralTypeContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitGeneralizedLiteralText(HqlParser.GeneralizedLiteralTextContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitNamedParameter(HqlParser.NamedParameterContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.COLON(), false));
		tokens.addAll(visit(ctx.identifier()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitPositionalParameter(HqlParser.PositionalParameterContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.QUESTION_MARK(), false));
		if (ctx.INTEGER_LITERAL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INTEGER_LITERAL()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFunction(HqlParser.FunctionContext ctx) {

		if (ctx.standardFunction() != null) {
			return visit(ctx.standardFunction());
		} else if (ctx.aggregateFunction() != null) {
			return visit(ctx.aggregateFunction());
		} else if (ctx.collectionSizeFunction() != null) {
			return visit(ctx.collectionSizeFunction());
		} else if (ctx.indexAggregateFunction() != null) {
			return visit(ctx.indexAggregateFunction());
		} else if (ctx.elementAggregateFunction() != null) {
			return visit(ctx.elementAggregateFunction());
		} else if (ctx.collectionFunctionMisuse() != null) {
			return visit(ctx.collectionFunctionMisuse());
		} else if (ctx.jpaNonstandardFunction() != null) {
			return visit(ctx.jpaNonstandardFunction());
		} else if (ctx.genericFunction() != null) {
			return visit(ctx.genericFunction());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitJpaNonstandardFunction(HqlParser.JpaNonstandardFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.FUNCTION(), false));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.jpaNonstandardFunctionName()));
		if (ctx.COMMA() != null) {

			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
			tokens.addAll(visit(ctx.genericFunctionArguments()));
		}
		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJpaNonstandardFunctionName(HqlParser.JpaNonstandardFunctionNameContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitGenericFunction(HqlParser.GenericFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.genericFunctionName()));
		NOSPACE(tokens);
		tokens.add(TOKEN_OPEN_PAREN);
		if (ctx.genericFunctionArguments() != null) {
			tokens.addAll(visit(ctx.genericFunctionArguments()));
		} else if (ctx.ASTERISK() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ASTERISK()));
		}
		tokens.add(TOKEN_CLOSE_PAREN);
		if (ctx.nthSideClause() != null) {
			tokens.addAll(visit(ctx.nthSideClause()));
		}
		if (ctx.nullsClause() != null) {
			tokens.addAll(visit(ctx.nullsClause()));
		}
		if (ctx.withinGroupClause() != null) {
			tokens.addAll(visit(ctx.withinGroupClause()));
		}
		if (ctx.filterClause() != null) {
			tokens.addAll(visit(ctx.filterClause()));
		}
		if (ctx.overClause() != null) {
			tokens.addAll(visit(ctx.overClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitGenericFunctionName(HqlParser.GenericFunctionNameContext ctx) {
		return visit(ctx.simplePath());
	}

	@Override
	public List<JpaQueryParsingToken> visitGenericFunctionArguments(HqlParser.GenericFunctionArgumentsContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		} else if (ctx.datetimeField() != null) {

			tokens.addAll(visit(ctx.datetimeField()));
			tokens.add(TOKEN_COMMA);
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
	public List<JpaQueryParsingToken> visitCollectionSizeFunction(HqlParser.CollectionSizeFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SIZE()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitIndexAggregateFunction(HqlParser.IndexAggregateFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.MAXINDEX() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.MAXINDEX()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.MININDEX() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.MININDEX()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.MAX() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.MAX()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.MIN() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.MIN()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.SUM() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.SUM()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.AVG() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.AVG()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitElementAggregateFunction(HqlParser.ElementAggregateFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.MAXELEMENT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.MAXELEMENT()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.MINELEMENT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.MINELEMENT()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.MAX() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.MAX()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.MIN() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.MIN()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.SUM() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.SUM()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.AVG() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.AVG()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.path()));
			tokens.add(TOKEN_CLOSE_PAREN);
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCollectionFunctionMisuse(HqlParser.CollectionFunctionMisuseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.ELEMENTS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
		} else if (ctx.INDICES() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
		}

		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.path()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitAggregateFunction(HqlParser.AggregateFunctionContext ctx) {

		if (ctx.everyFunction() != null) {
			return visit(ctx.everyFunction());
		} else if (ctx.anyFunction() != null) {
			return visit(ctx.anyFunction());
		} else if (ctx.listaggFunction() != null) {
			return visit(ctx.listaggFunction());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitEveryFunction(HqlParser.EveryFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.EVERY() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.EVERY()));
		} else if (ctx.ALL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ALL()));
		}

		if (ctx.ELEMENTS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
		} else if (ctx.INDICES() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
		}

		NOSPACE(tokens);
		tokens.add(TOKEN_OPEN_PAREN);

		if (ctx.predicate() != null) {
			tokens.addAll(visit(ctx.predicate()));
		} else if (ctx.subquery() != null) {
			tokens.addAll(visit(ctx.subquery()));
		} else if (ctx.simplePath() != null) {
			tokens.addAll(visit(ctx.simplePath()));
		}

		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.filterClause() != null) {
			tokens.addAll(visit(ctx.filterClause()));
		}

		if (ctx.overClause() != null) {
			tokens.addAll(visit(ctx.overClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitAnyFunction(HqlParser.AnyFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.ANY() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ANY()));
		} else if (ctx.SOME() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.SOME()));
		}

		if (ctx.ELEMENTS() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ELEMENTS()));
		} else if (ctx.INDICES() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INDICES()));
		}

		NOSPACE(tokens);
		tokens.add(TOKEN_OPEN_PAREN);

		if (ctx.predicate() != null) {
			tokens.addAll(visit(ctx.predicate()));
		} else if (ctx.subquery() != null) {
			tokens.addAll(visit(ctx.subquery()));
		} else if (ctx.simplePath() != null) {
			tokens.addAll(visit(ctx.simplePath()));
		}

		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.filterClause() != null) {
			tokens.addAll(visit(ctx.filterClause()));
		}

		if (ctx.overClause() != null) {
			tokens.addAll(visit(ctx.overClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitListaggFunction(HqlParser.ListaggFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.LISTAGG()));
		tokens.add(TOKEN_OPEN_PAREN);
		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}
		tokens.addAll(visit(ctx.expressionOrPredicate(0)));
		tokens.add(TOKEN_COMMA);
		tokens.addAll(visit(ctx.expressionOrPredicate(1)));
		if (ctx.onOverflowClause() != null) {
			tokens.addAll(visit(ctx.onOverflowClause()));
		}
		tokens.add(TOKEN_CLOSE_PAREN);

		if (ctx.withinGroupClause() != null) {
			tokens.addAll(visit(ctx.withinGroupClause()));
		}
		if (ctx.filterClause() != null) {
			tokens.addAll(visit(ctx.filterClause()));
		}
		if (ctx.overClause() != null) {
			tokens.addAll(visit(ctx.overClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOnOverflowClause(HqlParser.OnOverflowClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ON()));
		tokens.add(new JpaQueryParsingToken(ctx.OVERFLOW()));

		if (ctx.ERROR() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.ERROR()));
		} else if (ctx.TRUNCATE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.TRUNCATE()));
			if (ctx.expression() != null) {
				tokens.addAll(visit(ctx.expression()));
			}
			if (ctx.WITH() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.WITH()));
			} else if (ctx.WITHOUT() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.WITHOUT()));
			}
			tokens.add(new JpaQueryParsingToken(ctx.COUNT()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitWithinGroupClause(HqlParser.WithinGroupClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WITHIN()));
		tokens.add(new JpaQueryParsingToken(ctx.GROUP()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.orderByClause()));
		tokens.add(TOKEN_CLOSE_PAREN);

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
	public List<JpaQueryParsingToken> visitNullsClause(HqlParser.NullsClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.RESPECT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.RESPECT()));
		} else if (ctx.IGNORE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.IGNORE()));
		}
		tokens.add(new JpaQueryParsingToken(ctx.NULLS()));

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitNthSideClause(HqlParser.NthSideClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.FROM()));

		if (ctx.FIRST() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FIRST()));
		} else if (ctx.LAST() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LAST()));
		}

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
	public List<JpaQueryParsingToken> visitFrameStart(HqlParser.FrameStartContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.PRECEDING() != null) {

			if (ctx.UNBOUNDED() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.UNBOUNDED()));
			} else {
				tokens.addAll(visit(ctx.expression()));
			}

			tokens.add(new JpaQueryParsingToken(ctx.PRECEDING()));
		} else if (ctx.CURRENT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
			tokens.add(new JpaQueryParsingToken(ctx.ROW()));
		} else if (ctx.FOLLOWING() != null) {

			tokens.addAll(visit(ctx.expression()));
			tokens.add(new JpaQueryParsingToken(ctx.FOLLOWING()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFrameEnd(HqlParser.FrameEndContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.PRECEDING() != null) {

			tokens.addAll(visit(ctx.expression()));
			tokens.add(new JpaQueryParsingToken(ctx.PRECEDING()));
		} else if (ctx.CURRENT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
			tokens.add(new JpaQueryParsingToken(ctx.ROW()));
		} else if (ctx.FOLLOWING() != null) {

			if (ctx.UNBOUNDED() == null) {
				tokens.addAll(visit(ctx.expression()));
			} else {
				tokens.add(new JpaQueryParsingToken(ctx.UNBOUNDED()));
			}

			tokens.add(new JpaQueryParsingToken(ctx.FOLLOWING()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFrameExclusion(HqlParser.FrameExclusionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.EXCLUDE()));

		if (ctx.CURRENT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
			tokens.add(new JpaQueryParsingToken(ctx.ROW()));
		} else if (ctx.GROUP() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.GROUP()));
		} else if (ctx.TIES() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.TIES()));
		} else if (ctx.NO() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.NO()));
			tokens.add(new JpaQueryParsingToken(ctx.OTHERS()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitStandardFunction(HqlParser.StandardFunctionContext ctx) {

		if (ctx.castFunction() != null) {
			return visit(ctx.castFunction());
		} else if (ctx.extractFunction() != null) {
			return visit(ctx.extractFunction());
		} else if (ctx.truncFunction() != null) {
			return visit(ctx.truncFunction());
		} else if (ctx.formatFunction() != null) {
			return visit(ctx.formatFunction());
		} else if (ctx.collateFunction() != null) {
			return visit(ctx.collateFunction());
		} else if (ctx.substringFunction() != null) {
			return visit(ctx.substringFunction());
		} else if (ctx.overlayFunction() != null) {
			return visit(ctx.overlayFunction());
		} else if (ctx.trimFunction() != null) {
			return visit(ctx.trimFunction());
		} else if (ctx.padFunction() != null) {
			return visit(ctx.padFunction());
		} else if (ctx.positionFunction() != null) {
			return visit(ctx.positionFunction());
		} else if (ctx.currentDateFunction() != null) {
			return visit(ctx.currentDateFunction());
		} else if (ctx.currentTimeFunction() != null) {
			return visit(ctx.currentTimeFunction());
		} else if (ctx.currentTimestampFunction() != null) {
			return visit(ctx.currentTimestampFunction());
		} else if (ctx.instantFunction() != null) {
			return visit(ctx.instantFunction());
		} else if (ctx.localDateFunction() != null) {
			return visit(ctx.localDateFunction());
		} else if (ctx.localTimeFunction() != null) {
			return visit(ctx.localTimeFunction());
		} else if (ctx.localDateTimeFunction() != null) {
			return visit(ctx.localDateTimeFunction());
		} else if (ctx.offsetDateTimeFunction() != null) {
			return visit(ctx.offsetDateTimeFunction());
		} else if (ctx.cube() != null) {
			return visit(ctx.cube());
		} else if (ctx.rollup() != null) {
			return visit(ctx.rollup());
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitCastFunction(HqlParser.CastFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CAST()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.expression()));
		tokens.add(TOKEN_AS);
		tokens.addAll(visit(ctx.castTarget()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCastTarget(HqlParser.CastTargetContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.addAll(visit(ctx.castTargetType()));

		if (ctx.LEFT_PAREN() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.add(new JpaQueryParsingToken(ctx.INTEGER_LITERAL(0)));

			if (ctx.COMMA() != null) {
				tokens.add(TOKEN_COMMA);
				tokens.add(new JpaQueryParsingToken(ctx.INTEGER_LITERAL(1)));
			}
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCastTargetType(HqlParser.CastTargetTypeContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.fullTargetName));
	}

	@Override
	public List<JpaQueryParsingToken> visitSubstringFunction(HqlParser.SubstringFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.SUBSTRING()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.expression()));

		if (ctx.COMMA() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.COMMA(0)));
			tokens.addAll(visit(ctx.substringFunctionStartArgument()));

			if (ctx.COMMA(1) != null) {

				tokens.add(new JpaQueryParsingToken(ctx.COMMA(1)));
				tokens.addAll(visit(ctx.substringFunctionLengthArgument()));
			}
		} else if (ctx.FROM() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.FROM()));
			tokens.addAll(visit(ctx.substringFunctionStartArgument()));

			if (ctx.FOR() != null) {

				tokens.add(new JpaQueryParsingToken(ctx.FOR()));
				tokens.addAll(visit(ctx.substringFunctionLengthArgument()));
			}
		}

		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSubstringFunctionStartArgument(
			HqlParser.SubstringFunctionStartArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitSubstringFunctionLengthArgument(
			HqlParser.SubstringFunctionLengthArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitTrimFunction(HqlParser.TrimFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.TRIM()));
		tokens.add(TOKEN_OPEN_PAREN);
		if (ctx.trimSpecification() != null) {
			tokens.addAll(visit(ctx.trimSpecification()));
		}
		if (ctx.trimCharacter() != null) {
			tokens.addAll(visit(ctx.trimCharacter()));
		}
		if (ctx.FROM() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FROM()));
		}
		tokens.addAll(visit(ctx.expression()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTrimSpecification(HqlParser.TrimSpecificationContext ctx) {

		if (ctx.LEADING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LEADING()));
		} else if (ctx.TRAILING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TRAILING()));
		} else if (ctx.BOTH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.BOTH()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitTrimCharacter(HqlParser.TrimCharacterContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitPadFunction(HqlParser.PadFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.PAD()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.expression()));
		tokens.add(TOKEN_WITH);
		tokens.addAll(visit(ctx.padLength()));
		tokens.addAll(visit(ctx.padSpecification()));
		if (ctx.padCharacter() != null) {
			tokens.addAll(visit(ctx.padCharacter()));
		}
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitPadSpecification(HqlParser.PadSpecificationContext ctx) {

		if (ctx.LEADING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LEADING()));
		} else if (ctx.TRAILING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TRAILING()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitPadCharacter(HqlParser.PadCharacterContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitPadLength(HqlParser.PadLengthContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitOverlayFunction(HqlParser.OverlayFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.OVERLAY()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.overlayFunctionStringArgument()));
		tokens.add(new JpaQueryParsingToken(ctx.PLACING()));
		tokens.addAll(visit(ctx.overlayFunctionReplacementArgument()));
		tokens.add(new JpaQueryParsingToken(ctx.FROM()));
		tokens.addAll(visit(ctx.overlayFunctionStartArgument()));
		if (ctx.FOR() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.FOR()));
			tokens.addAll(visit(ctx.overlayFunctionLengthArgument()));
		}
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOverlayFunctionStringArgument(
			HqlParser.OverlayFunctionStringArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitOverlayFunctionReplacementArgument(
			HqlParser.OverlayFunctionReplacementArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitOverlayFunctionStartArgument(
			HqlParser.OverlayFunctionStartArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitOverlayFunctionLengthArgument(
			HqlParser.OverlayFunctionLengthArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitCurrentDateFunction(HqlParser.CurrentDateFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.CURRENT_DATE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_DATE()));
			if (ctx.LEFT_PAREN() != null) {

				tokens.add(TOKEN_OPEN_PAREN);
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		} else if (ctx.CURRENT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
			tokens.add(new JpaQueryParsingToken(ctx.DATE()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCurrentTimeFunction(HqlParser.CurrentTimeFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.CURRENT_TIME() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_TIME()));
			if (ctx.LEFT_PAREN() != null) {

				tokens.add(TOKEN_OPEN_PAREN);
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		} else if (ctx.CURRENT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
			tokens.add(new JpaQueryParsingToken(ctx.TIME()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitCurrentTimestampFunction(HqlParser.CurrentTimestampFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.CURRENT_TIMESTAMP() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_TIMESTAMP()));
			if (ctx.LEFT_PAREN() != null) {

				tokens.add(TOKEN_OPEN_PAREN);
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		} else if (ctx.CURRENT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT()));
			tokens.add(new JpaQueryParsingToken(ctx.TIMESTAMP()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitInstantFunction(HqlParser.InstantFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.CURRENT_INSTANT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.CURRENT_INSTANT()));
			if (ctx.LEFT_PAREN() != null) {

				tokens.add(TOKEN_OPEN_PAREN);
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		} else if (ctx.INSTANT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INSTANT()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitLocalDateTimeFunction(HqlParser.LocalDateTimeFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LOCAL_DATETIME() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LOCAL_DATETIME()));
			if (ctx.LEFT_PAREN() != null) {

				tokens.add(TOKEN_OPEN_PAREN);
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		} else if (ctx.LOCAL() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LOCAL()));
			tokens.add(new JpaQueryParsingToken(ctx.DATETIME()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitOffsetDateTimeFunction(HqlParser.OffsetDateTimeFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.OFFSET_DATETIME() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.OFFSET_DATETIME()));
			if (ctx.LEFT_PAREN() != null) {

				tokens.add(TOKEN_OPEN_PAREN);
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		} else if (ctx.OFFSET() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.OFFSET()));
			tokens.add(new JpaQueryParsingToken(ctx.DATETIME()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitLocalDateFunction(HqlParser.LocalDateFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LOCAL_DATE() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LOCAL_DATE()));
			if (ctx.LEFT_PAREN() != null) {

				tokens.add(TOKEN_OPEN_PAREN);
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		} else if (ctx.LOCAL() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LOCAL()));
			tokens.add(new JpaQueryParsingToken(ctx.DATE()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitLocalTimeFunction(HqlParser.LocalTimeFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.LOCAL_TIME() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LOCAL_TIME()));
			if (ctx.LEFT_PAREN() != null) {

				tokens.add(TOKEN_OPEN_PAREN);
				tokens.add(TOKEN_CLOSE_PAREN);
			}
		} else if (ctx.LOCAL() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.LOCAL()));
			tokens.add(new JpaQueryParsingToken(ctx.TIME()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFormatFunction(HqlParser.FormatFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.FORMAT()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.expression()));
		tokens.add(new JpaQueryParsingToken(ctx.AS()));
		tokens.addAll(visit(ctx.format()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFormat(HqlParser.FormatContext ctx) {
		return List.of(new JpaQueryParsingToken(ctx.STRING_LITERAL()));
	}

	@Override
	public List<JpaQueryParsingToken> visitExtractFunction(HqlParser.ExtractFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.EXTRACT() != null) {

			tokens.add(new JpaQueryParsingToken(ctx.EXTRACT()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.extractField()));
			tokens.add(new JpaQueryParsingToken(ctx.FROM()));
			tokens.addAll(visit(ctx.expression()));
			tokens.add(TOKEN_CLOSE_PAREN);
		} else if (ctx.datetimeField() != null) {

			tokens.addAll(visit(ctx.datetimeField()));
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.expression()));
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTruncFunction(HqlParser.TruncFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.TRUNC() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.TRUNC()));
		} else if (ctx.TRUNCATE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.TRUNCATE()));
		}
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.expression(0)));
		if (ctx.COMMA() != null) {
			tokens.add(TOKEN_COMMA);
		}
		if (ctx.datetimeField() != null) {
			tokens.addAll(visit(ctx.datetimeField()));
		}
		if (ctx.expression(1) != null) {
			tokens.addAll(visit(ctx.expression(1)));
		}
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitExtractField(HqlParser.ExtractFieldContext ctx) {

		if (ctx.datetimeField() != null) {
			return visit(ctx.datetimeField());
		} else if (ctx.dayField() != null) {
			return visit(ctx.dayField());
		} else if (ctx.weekField() != null) {
			return visit(ctx.weekField());
		} else if (ctx.timeZoneField() != null) {
			return visit(ctx.timeZoneField());
		} else if (ctx.dateOrTimeField() != null) {
			return visit(ctx.dateOrTimeField());
		} else {
			return List.of();
		}
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
	public List<JpaQueryParsingToken> visitDayField(HqlParser.DayFieldContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.DAY()));
		tokens.add(new JpaQueryParsingToken(ctx.OF()));
		if (ctx.MONTH() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.MONTH()));
		} else if (ctx.WEEK() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.WEEK()));
		} else if (ctx.YEAR() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.YEAR()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitWeekField(HqlParser.WeekFieldContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.WEEK()));
		tokens.add(new JpaQueryParsingToken(ctx.OF()));
		if (ctx.MONTH() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.MONTH()));
		} else if (ctx.YEAR() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.YEAR()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitTimeZoneField(HqlParser.TimeZoneFieldContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.OFFSET() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.OFFSET()));
			if (ctx.HOUR() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.HOUR()));
			}
			if (ctx.MINUTE() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.MINUTE()));
			}
		} else if (ctx.TIMEZONE_HOUR() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.TIMEZONE_HOUR()));
		} else if (ctx.TIMEZONE_MINUTE() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.TIMEZONE_MINUTE()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitDateOrTimeField(HqlParser.DateOrTimeFieldContext ctx) {

		if (ctx.DATE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DATE()));
		} else if (ctx.TIME() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TIME()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitPositionFunction(HqlParser.PositionFunctionContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.POSITION()));
		tokens.add(TOKEN_OPEN_PAREN);
		tokens.addAll(visit(ctx.positionFunctionPatternArgument()));
		tokens.add(new JpaQueryParsingToken(ctx.IN()));
		tokens.addAll(visit(ctx.positionFunctionStringArgument()));
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;

	}

	@Override
	public List<JpaQueryParsingToken> visitPositionFunctionPatternArgument(
			HqlParser.PositionFunctionPatternArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitPositionFunctionStringArgument(
			HqlParser.PositionFunctionStringArgumentContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public List<JpaQueryParsingToken> visitCube(HqlParser.CubeContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.CUBE()));
		tokens.add(TOKEN_OPEN_PAREN);
		ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {

			tokens.addAll(visit(expressionOrPredicateContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitRollup(HqlParser.RollupContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		tokens.add(new JpaQueryParsingToken(ctx.ROLLUP()));
		tokens.add(TOKEN_OPEN_PAREN);
		ctx.expressionOrPredicate().forEach(expressionOrPredicateContext -> {

			tokens.addAll(visit(expressionOrPredicateContext));
			NOSPACE(tokens);
			tokens.add(TOKEN_COMMA);
		});
		CLIP(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitNakedIdentifier(HqlParser.NakedIdentifierContext ctx) {

		if (ctx.IDENTIFIER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.IDENTIFIER()));
		} else if (ctx.QUOTED_IDENTIFIER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.QUOTED_IDENTIFIER()));
		} else if (ctx.ALL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ALL()));
		} else if (ctx.AND() != null) {
			return List.of(new JpaQueryParsingToken(ctx.AND()));
		} else if (ctx.ANY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ANY()));
		} else if (ctx.AS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.AS()));
		} else if (ctx.ASC() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ASC()));
		} else if (ctx.AVG() != null) {
			return List.of(new JpaQueryParsingToken(ctx.AVG()));
		} else if (ctx.BETWEEN() != null) {
			return List.of(new JpaQueryParsingToken(ctx.BETWEEN()));
		} else if (ctx.BOTH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.BOTH()));
		} else if (ctx.BREADTH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.BREADTH()));
		} else if (ctx.BY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.BY()));
		} else if (ctx.CASE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CASE()));
		} else if (ctx.CAST() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CAST()));
		} else if (ctx.COLLATE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.COLLATE()));
		} else if (ctx.COUNT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.COUNT()));
		} else if (ctx.CROSS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CROSS()));
		} else if (ctx.CUBE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CUBE()));
		} else if (ctx.CURRENT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CURRENT()));
		} else if (ctx.CURRENT_DATE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CURRENT_DATE()));
		} else if (ctx.CURRENT_INSTANT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CURRENT_INSTANT()));
		} else if (ctx.CURRENT_TIME() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CURRENT_TIME()));
		} else if (ctx.CURRENT_TIMESTAMP() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CURRENT_TIMESTAMP()));
		} else if (ctx.CYCLE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.CYCLE()));
		} else if (ctx.DATE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DATE()));
		} else if (ctx.DATETIME() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DATETIME()));
		} else if (ctx.DAY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DAY()));
		} else if (ctx.DEFAULT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DEFAULT()));
		} else if (ctx.DELETE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DELETE()));
		} else if (ctx.DEPTH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DEPTH()));
		} else if (ctx.DESC() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DESC()));
		} else if (ctx.DISTINCT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.DISTINCT()));
		} else if (ctx.ELEMENT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ELEMENT()));
		} else if (ctx.ELEMENTS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ELEMENTS()));
		} else if (ctx.ELSE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ELSE()));
		} else if (ctx.EMPTY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.EMPTY()));
		} else if (ctx.END() != null) {
			return List.of(new JpaQueryParsingToken(ctx.END()));
		} else if (ctx.ENTRY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ENTRY()));
		} else if (ctx.EPOCH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.EPOCH()));
		} else if (ctx.ERROR() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ERROR()));
		} else if (ctx.ESCAPE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ESCAPE()));
		} else if (ctx.EVERY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.EVERY()));
		} else if (ctx.EXCEPT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.EXCEPT()));
		} else if (ctx.EXCLUDE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.EXCLUDE()));
		} else if (ctx.EXISTS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.EXISTS()));
		} else if (ctx.EXTRACT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.EXTRACT()));
		} else if (ctx.FETCH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FETCH()));
		} else if (ctx.FILTER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FILTER()));
		} else if (ctx.FIRST() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FIRST()));
		} else if (ctx.FOLLOWING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FOLLOWING()));
		} else if (ctx.FOR() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FOR()));
		} else if (ctx.FORMAT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FORMAT()));
		} else if (ctx.FROM() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FROM()));
		} else if (ctx.FUNCTION() != null) {
			return List.of(new JpaQueryParsingToken(ctx.FUNCTION()));
		} else if (ctx.GROUP() != null) {
			return List.of(new JpaQueryParsingToken(ctx.GROUP()));
		} else if (ctx.GROUPS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.GROUPS()));
		} else if (ctx.HAVING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.HAVING()));
		} else if (ctx.HOUR() != null) {
			return List.of(new JpaQueryParsingToken(ctx.HOUR()));
		} else if (ctx.ID() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ID()));
		} else if (ctx.IGNORE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.IGNORE()));
		} else if (ctx.ILIKE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ILIKE()));
		} else if (ctx.IN() != null) {
			return List.of(new JpaQueryParsingToken(ctx.IN()));
		} else if (ctx.INDEX() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INDEX()));
		} else if (ctx.INDICES() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INDICES()));
		} else if (ctx.INSERT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INSERT()));
		} else if (ctx.INSTANT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INSTANT()));
		} else if (ctx.INTERSECT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTERSECT()));
		} else if (ctx.INTO() != null) {
			return List.of(new JpaQueryParsingToken(ctx.INTO()));
		} else if (ctx.IS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.IS()));
		} else if (ctx.JOIN() != null) {
			return List.of(new JpaQueryParsingToken(ctx.JOIN()));
		} else if (ctx.KEY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.KEY()));
		} else if (ctx.LAST() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LAST()));
		} else if (ctx.LEADING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LEADING()));
		} else if (ctx.LIKE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LIKE()));
		} else if (ctx.LIMIT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LIMIT()));
		} else if (ctx.LIST() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LIST()));
		} else if (ctx.LISTAGG() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LISTAGG()));
		} else if (ctx.LOCAL() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LOCAL()));
		} else if (ctx.LOCAL_DATE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LOCAL_DATE()));
		} else if (ctx.LOCAL_DATETIME() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LOCAL_DATETIME()));
		} else if (ctx.LOCAL_TIME() != null) {
			return List.of(new JpaQueryParsingToken(ctx.LOCAL_TIME()));
		} else if (ctx.MAP() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MAP()));
		} else if (ctx.MATERIALIZED() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MATERIALIZED()));
		} else if (ctx.MAX() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MAX()));
		} else if (ctx.MAXELEMENT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MAXELEMENT()));
		} else if (ctx.MAXINDEX() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MAXINDEX()));
		} else if (ctx.MEMBER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MEMBER()));
		} else if (ctx.MICROSECOND() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MICROSECOND()));
		} else if (ctx.MILLISECOND() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MILLISECOND()));
		} else if (ctx.MIN() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MIN()));
		} else if (ctx.MINELEMENT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MINELEMENT()));
		} else if (ctx.MININDEX() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MININDEX()));
		} else if (ctx.MINUTE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MINUTE()));
		} else if (ctx.MONTH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.MONTH()));
		} else if (ctx.NANOSECOND() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NANOSECOND()));
		} else if (ctx.NATURALID() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NATURALID()));
		} else if (ctx.NEW() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NEW()));
		} else if (ctx.NEXT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NEXT()));
		} else if (ctx.NO() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NO()));
		} else if (ctx.NOT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NOT()));
		} else if (ctx.NULLS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.NULLS()));
		} else if (ctx.OBJECT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.OBJECT()));
		} else if (ctx.OF() != null) {
			return List.of(new JpaQueryParsingToken(ctx.OF()));
		} else if (ctx.OFFSET() != null) {
			return List.of(new JpaQueryParsingToken(ctx.OFFSET()));
		} else if (ctx.OFFSET_DATETIME() != null) {
			return List.of(new JpaQueryParsingToken(ctx.OFFSET_DATETIME()));
		} else if (ctx.ON() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ON()));
		} else if (ctx.ONLY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ONLY()));
		} else if (ctx.OR() != null) {
			return List.of(new JpaQueryParsingToken(ctx.OR()));
		} else if (ctx.ORDER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ORDER()));
		} else if (ctx.OTHERS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.OTHERS()));
		} else if (ctx.OVER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.OVER()));
		} else if (ctx.OVERFLOW() != null) {
			return List.of(new JpaQueryParsingToken(ctx.OVERFLOW()));
		} else if (ctx.OVERLAY() != null) {
			return List.of(new JpaQueryParsingToken(ctx.OVERLAY()));
		} else if (ctx.PAD() != null) {
			return List.of(new JpaQueryParsingToken(ctx.PAD()));
		} else if (ctx.PARTITION() != null) {
			return List.of(new JpaQueryParsingToken(ctx.PARTITION()));
		} else if (ctx.PERCENT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.PERCENT()));
		} else if (ctx.PLACING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.PLACING()));
		} else if (ctx.POSITION() != null) {
			return List.of(new JpaQueryParsingToken(ctx.POSITION()));
		} else if (ctx.PRECEDING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.PRECEDING()));
		} else if (ctx.QUARTER() != null) {
			return List.of(new JpaQueryParsingToken(ctx.QUARTER()));
		} else if (ctx.RANGE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.RANGE()));
		} else if (ctx.RESPECT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.RESPECT()));
		} else if (ctx.ROLLUP() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ROLLUP()));
		} else if (ctx.ROW() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ROW()));
		} else if (ctx.ROWS() != null) {
			return List.of(new JpaQueryParsingToken(ctx.ROWS()));
		} else if (ctx.SEARCH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SEARCH()));
		} else if (ctx.SECOND() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SECOND()));
		} else if (ctx.SELECT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SELECT()));
		} else if (ctx.SET() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SET()));
		} else if (ctx.SIZE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SIZE()));
		} else if (ctx.SOME() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SOME()));
		} else if (ctx.SUBSTRING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SUBSTRING()));
		} else if (ctx.SUM() != null) {
			return List.of(new JpaQueryParsingToken(ctx.SUM()));
		} else if (ctx.THEN() != null) {
			return List.of(new JpaQueryParsingToken(ctx.THEN()));
		} else if (ctx.TIES() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TIES()));
		} else if (ctx.TIME() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TIME()));
		} else if (ctx.TIMESTAMP() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TIMESTAMP()));
		} else if (ctx.TIMEZONE_HOUR() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TIMEZONE_HOUR()));
		} else if (ctx.TIMEZONE_MINUTE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TIMEZONE_MINUTE()));
		} else if (ctx.TO() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TO()));
		} else if (ctx.TRAILING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TRAILING()));
		} else if (ctx.TREAT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TREAT()));
		} else if (ctx.TRIM() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TRIM()));
		} else if (ctx.TRUNC() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TRUNC()));
		} else if (ctx.TRUNCATE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TRUNCATE()));
		} else if (ctx.TYPE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.TYPE()));
		} else if (ctx.UNBOUNDED() != null) {
			return List.of(new JpaQueryParsingToken(ctx.UNBOUNDED()));
		} else if (ctx.UNION() != null) {
			return List.of(new JpaQueryParsingToken(ctx.UNION()));
		} else if (ctx.UPDATE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.UPDATE()));
		} else if (ctx.USING() != null) {
			return List.of(new JpaQueryParsingToken(ctx.USING()));
		} else if (ctx.VALUE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.VALUE()));
		} else if (ctx.VALUES() != null) {
			return List.of(new JpaQueryParsingToken(ctx.VALUES()));
		} else if (ctx.VERSION() != null) {
			return List.of(new JpaQueryParsingToken(ctx.VERSION()));
		} else if (ctx.VERSIONED() != null) {
			return List.of(new JpaQueryParsingToken(ctx.VERSIONED()));
		} else if (ctx.WEEK() != null) {
			return List.of(new JpaQueryParsingToken(ctx.WEEK()));
		} else if (ctx.WHEN() != null) {
			return List.of(new JpaQueryParsingToken(ctx.WHEN()));
		} else if (ctx.WHERE() != null) {
			return List.of(new JpaQueryParsingToken(ctx.WHERE()));
		} else if (ctx.WITH() != null) {
			return List.of(new JpaQueryParsingToken(ctx.WITH()));
		} else if (ctx.WITHIN() != null) {
			return List.of(new JpaQueryParsingToken(ctx.WITHIN()));
		} else if (ctx.WITHOUT() != null) {
			return List.of(new JpaQueryParsingToken(ctx.WITHOUT()));
		} else if (ctx.YEAR() != null) {
			return List.of(new JpaQueryParsingToken(ctx.YEAR()));
		} else {
			return List.of();
		}
	}

	@Override
	public List<JpaQueryParsingToken> visitIdentifier(HqlParser.IdentifierContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>();

		if (ctx.nakedIdentifier() != null) {
			tokens.addAll(visit(ctx.nakedIdentifier()));
		} else if (ctx.FULL() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.FULL()));
		} else if (ctx.INNER() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.INNER()));
		} else if (ctx.LEFT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.LEFT()));
		} else if (ctx.OUTER() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.OUTER()));
		} else if (ctx.RIGHT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.RIGHT()));
		}

		return tokens;
	}
}
