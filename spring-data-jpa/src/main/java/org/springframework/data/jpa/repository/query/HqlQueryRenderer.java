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

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.util.CollectionUtils;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that renders an HQL query without making any changes.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Oscar Fanchin
 * @author Mark Paluch
 * @since 3.1
 */
@SuppressWarnings({ "ConstantConditions", "DuplicatedCode", "UnreachableCode" })
class HqlQueryRenderer extends HqlBaseVisitor<QueryTokenStream> {

	/**
	 * Is this AST tree a {@literal subquery}?
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

	/**
	 * Is this AST tree a {@literal set} query that has been added through {@literal UNION|INTERSECT|EXCEPT}?
	 *
	 * @return boolean
	 */
	static boolean isSetQuery(ParserRuleContext ctx) {

		if (ctx instanceof HqlParser.OrderedQueryContext
				&& ctx.getParent() instanceof HqlParser.QueryExpressionContext qec) {
			if (qec.orderedQuery().indexOf(ctx) != 0) {
				return true;
			}
		}

		return ctx.getParent() != null && isSetQuery(ctx.getParent());
	}

	@Override
	public QueryTokenStream visitStart(HqlParser.StartContext ctx) {
		return visit(ctx.ql_statement());
	}

	@Override
	public QueryTokenStream visitWithClause(HqlParser.WithClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(QueryTokens.expression(ctx.WITH()));
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

		builder.appendInline(visit(ctx.fromRoot()));
		builder.appendInline(QueryTokenStream.concat(ctx.joinSpecifier(), this::visit, EMPTY_TOKEN));

		return builder;
	}

	@Override
	public QueryTokenStream visitRootSubquery(HqlParser.RootSubqueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LATERAL() != null) {
			builder.append(QueryTokens.expression(ctx.LATERAL()));
		}

		builder.appendExpression(QueryTokenStream.group(visit(ctx.subquery())));

		if (ctx.variable() != null) {
			builder.appendExpression(visit(ctx.variable()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSimpleSetReturningFunction(HqlParser.SimpleSetReturningFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.identifier()));

		builder.append(TOKEN_OPEN_PAREN);
		if (ctx.genericFunctionArguments() != null) {
			builder.append(visit(ctx.genericFunctionArguments()));
		}
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitJoin(HqlParser.JoinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_SPACE);
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
	public QueryTokenStream visitJoinSubquery(HqlParser.JoinSubqueryContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.LATERAL() != null) {
			builder.append(QueryTokens.expression(ctx.LATERAL()));
		}

		builder.append(QueryTokenStream.group(visit(ctx.subquery())));

		if (ctx.variable() != null) {
			builder.appendExpression(visit(ctx.variable()));
		}

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
	public QueryTokenStream visitTargetFields(HqlParser.TargetFieldsContext ctx) {
		return QueryTokenStream.group(QueryTokenStream.concat(ctx.simplePath(), this::visit, TOKEN_COMMA));
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
		return QueryTokenStream.group(QueryTokenStream.concat(ctx.expression(), this::visit, TOKEN_COMMA));
	}

	@Override
	public QueryTokenStream visitConflictTarget(HqlParser.ConflictTargetContext ctx) {

		if (ctx.identifier() != null) {
			return QueryTokenStream.concatExpressions(ctx.children, this::visit);
		}

		return QueryTokenStream.group(QueryTokenStream.concat(ctx.simplePath(), this::visit, TOKEN_COMMA));
	}

	@Override
	public QueryTokenStream visitInstantiation(HqlParser.InstantiationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.NEW()));
		builder.append(visit(ctx.instantiationTarget()));
		builder.append(QueryTokenStream.group(visit(ctx.instantiationArguments())));

		return builder;
	}
	public QueryTokenStream visitSelectionList(HqlParser.SelectionListContext ctx) {
		return QueryTokenStream.concat(ctx.selection(), this::visit, TOKEN_COMMA);
	}

	@Override
	public QueryTokenStream visitMapEntrySelection(HqlParser.MapEntrySelectionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.ENTRY()));
		builder.append(QueryTokenStream.group(visit(ctx.path())));

		return builder;
	}

	@Override
	public QueryTokenStream visitJpaSelectObjectSyntax(HqlParser.JpaSelectObjectSyntaxContext ctx) {
		return QueryTokenStream.ofFunction(ctx.OBJECT(), visit(ctx.identifier()));
	}

	@Override
	public QueryTokenStream visitWhereClause(HqlParser.WhereClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.WHERE()));
		builder.append(QueryTokenStream.concatExpressions(ctx.predicate(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitJpaCollectionJoin(HqlParser.JpaCollectionJoinContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_COMMA);
		builder.append(QueryTokens.token(ctx.IN()));
		builder.append(QueryTokenStream.group(visit(ctx.path())));

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
	public QueryTokenStream visitLocalDateTimeLiteral(HqlParser.LocalDateTimeLiteralContext ctx) {

		if (ctx.DATETIME() == null) {
			return QueryTokenStream.group(visit(ctx.localDateTime()));
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitZonedDateTimeLiteral(HqlParser.ZonedDateTimeLiteralContext ctx) {

		if (ctx.DATETIME() == null) {
			return QueryTokenStream.group(visit(ctx.zonedDateTime()));
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitOffsetDateTimeLiteral(HqlParser.OffsetDateTimeLiteralContext ctx) {

		if (ctx.DATETIME() == null) {
			return QueryTokenStream.group(visit(ctx.offsetDateTime()));
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitDateLiteral(HqlParser.DateLiteralContext ctx) {

		if (ctx.DATE() == null) {
			return QueryTokenStream.group(visit(ctx.date()));
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitTimeLiteral(HqlParser.TimeLiteralContext ctx) {

		if (ctx.TIME() == null) {
			return QueryTokenStream.group(visit(ctx.time()));
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitOffsetDateTime(HqlParser.OffsetDateTimeContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.date()));
		builder.appendInline(visit(ctx.time()));
		builder.appendInline(visit(ctx.offset()));

		return builder;
	}

	@Override
	public QueryTokenStream visitOffsetDateTimeWithMinutes(HqlParser.OffsetDateTimeWithMinutesContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.date()));
		builder.appendInline(visit(ctx.time()));
		builder.appendInline(visit(ctx.offsetWithMinutes()));

		return builder;
	}

	@Override
	public QueryTokenStream visitJdbcTimestampLiteral(HqlParser.JdbcTimestampLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_BRACE);
		builder.append(QueryTokens.token("ts"));
		builder.appendInline(visit(ctx.dateTime() != null ? ctx.dateTime() : ctx.genericTemporalLiteralText()));
		builder.append(QueryTokens.TOKEN_CLOSE_BRACE);

		return builder;
	}

	@Override
	public QueryTokenStream visitJdbcDateLiteral(HqlParser.JdbcDateLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_BRACE);
		builder.append(QueryTokens.token("d"));
		builder.append(visit(ctx.date() != null ? ctx.date() : ctx.genericTemporalLiteralText()));
		builder.append(QueryTokens.TOKEN_CLOSE_BRACE);

		return builder;
	}

	@Override
	public QueryTokenStream visitJdbcTimeLiteral(HqlParser.JdbcTimeLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_BRACE);
		builder.append(QueryTokens.token("t"));
		builder.append(visit(ctx.time() != null ? ctx.time() : ctx.genericTemporalLiteralText()));
		builder.append(QueryTokens.TOKEN_CLOSE_BRACE);

		return builder;
	}

	@Override
	public QueryTokenStream visitArrayLiteral(HqlParser.ArrayLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(TOKEN_OPEN_SQUARE_BRACKET);
		builder.append(QueryTokenStream.concat(ctx.expression(), this::visit, TOKEN_COMMA));
		builder.append(TOKEN_CLOSE_SQUARE_BRACKET);

		return builder;
	}

	@Override
	public QueryTokenStream visitGeneralizedLiteral(HqlParser.GeneralizedLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.generalizedLiteralType()));
		builder.append(TOKEN_COLON);
		builder.append(visit(ctx.generalizedLiteralText()));
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	@Override
	public QueryTokenStream visitDate(HqlParser.DateContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(visit(ctx.year()));
		builder.append(TOKEN_DASH);
		builder.append(visit(ctx.month()));
		builder.append(TOKEN_DASH);
		builder.append(visit(ctx.day()));

		return builder;
	}

	@Override
	public QueryTokenStream visitTime(HqlParser.TimeContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(visit(ctx.hour()));
		builder.append(TOKEN_COLON);
		builder.append(visit(ctx.minute()));

		if (ctx.second() != null) {
			builder.append(TOKEN_COLON);
			builder.append(visit(ctx.second()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitOffset(HqlParser.OffsetContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		if (ctx.MINUS() != null) {
			builder.append(QueryTokens.token(ctx.MINUS()));
		} else if (ctx.PLUS() != null) {
			builder.append(QueryTokens.token(ctx.PLUS()));
		}
		builder.append(visit(ctx.hour()));

		if (ctx.minute() != null) {
			builder.append(TOKEN_COLON);
			builder.append(visit(ctx.minute()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitOffsetWithMinutes(HqlParser.OffsetWithMinutesContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.MINUS() != null) {
			builder.append(QueryTokens.token(ctx.MINUS()));
		} else if (ctx.PLUS() != null) {
			builder.append(QueryTokens.token(ctx.PLUS()));
		}

		builder.append(visit(ctx.hour()));
		builder.append(TOKEN_COLON);
		builder.append(visit(ctx.minute()));

		return builder;
	}

	@Override
	public QueryTokenStream visitBinaryLiteral(HqlParser.BinaryLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.BINARY_LITERAL() != null) {
			builder.append(QueryTokens.expression(ctx.BINARY_LITERAL()));
		} else if (ctx.HEX_LITERAL() != null) {

			builder.append(TOKEN_OPEN_BRACE);
			builder.append(QueryTokenStream.concat(ctx.HEX_LITERAL(), QueryTokens::token, TOKEN_COMMA));
			builder.append(TOKEN_CLOSE_BRACE);
		}

		return builder;
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
	public QueryTokenStream visitGroupedExpression(HqlParser.GroupedExpressionContext ctx) {
		return QueryTokenStream.group(visit(ctx.expression()));
	}

	@Override
	public QueryTokenStream visitSignedNumericLiteral(HqlParser.SignedNumericLiteralContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.op));
		builder.append(visit(ctx.numericLiteral()));

		return builder;
	}

	@Override
	public QueryTokenStream visitSubqueryExpression(HqlParser.SubqueryExpressionContext ctx) {
		return QueryTokenStream.group(visit(ctx.subquery()));
	}

	@Override
	public QueryTokenStream visitSignedExpression(HqlParser.SignedExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.op));
		builder.appendInline(visit(ctx.expression()));

		return builder;
	}

	@Override
	public QueryTokenStream visitSyntacticPathExpression(HqlParser.SyntacticPathExpressionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendInline(visit(ctx.syntacticDomainPath()));

		if (ctx.pathContinuation() != null) {
			builder.appendInline(visit(ctx.pathContinuation()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitPathContinuation(HqlParser.PathContinuationContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_DOT);
		builder.append(visit(ctx.simplePath()));

		return builder;
	}

	@Override
	public QueryTokenStream visitEntityTypeReference(HqlParser.EntityTypeReferenceContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.path() != null) {
			builder.appendInline(visit(ctx.path()));
		}

		if (ctx.parameter() != null) {
			builder.appendInline(visit(ctx.parameter()));
		}

		return QueryTokenStream.ofFunction(ctx.TYPE(), builder);
	}

	@Override
	public QueryTokenStream visitEntityIdReference(HqlParser.EntityIdReferenceContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.ID()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.path()));
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinuation() != null) {
			builder.appendInline(visit(ctx.pathContinuation()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitEntityVersionReference(HqlParser.EntityVersionReferenceContext ctx) {
		return QueryTokenStream.ofFunction(ctx.VERSION(), visit(ctx.path()));
	}

	@Override
	public QueryTokenStream visitEntityNaturalIdReference(HqlParser.EntityNaturalIdReferenceContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.NATURALID()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(visit(ctx.path()));
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinuation() != null) {
			builder.appendInline(visit(ctx.pathContinuation()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitSyntacticDomainPath(HqlParser.SyntacticDomainPathContext ctx) {

		if (ctx.treatedNavigablePath() != null) {
			return visit(ctx.treatedNavigablePath());
		}

		if (ctx.collectionValueNavigablePath() != null) {
			return visit(ctx.collectionValueNavigablePath());
		}

		if (ctx.mapKeyNavigablePath() != null) {
			return visit(ctx.mapKeyNavigablePath());
		}

		if (ctx.toOneFkReference() != null) {
			return visit(ctx.toOneFkReference());
		}

		if (ctx.function() != null) {

			QueryRendererBuilder builder = QueryRenderer.builder();

			builder.append(visit(ctx.function()));

			if (ctx.indexedPathAccessFragment() != null) {
				builder.append(visit(ctx.indexedPathAccessFragment()));
			}

			if (ctx.slicedPathAccessFragment() != null) {
				builder.append(visit(ctx.slicedPathAccessFragment()));
			}

			if (ctx.pathContinuation() != null) {
				builder.append(visit(ctx.pathContinuation()));
			}

			return builder;
		}

		if (ctx.indexedPathAccessFragment() != null) {

			QueryRendererBuilder builder = QueryRenderer.builder();

			builder.append(visit(ctx.simplePath()));
			builder.append(visit(ctx.indexedPathAccessFragment()));

			return builder;
		}

		if (ctx.slicedPathAccessFragment() != null) {

			QueryRendererBuilder builder = QueryRenderer.builder();

			builder.append(visit(ctx.simplePath()));
			builder.append(visit(ctx.slicedPathAccessFragment()));

			return builder;
		}

		return QueryRenderer.empty();
	}

	@Override
	public QueryTokenStream visitSlicedPathAccessFragment(HqlParser.SlicedPathAccessFragmentContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_OPEN_SQUARE_BRACKET);
		builder.appendInline(visit(ctx.expression(0)));
		builder.append(TOKEN_COLON);
		builder.appendInline(visit(ctx.expression(1)));
		builder.append(TOKEN_CLOSE_SQUARE_BRACKET);

		return builder;
	}

	@Override
	public QueryTokenStream visitSubstringFunction(HqlParser.SubstringFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.FROM() == null) {
			builder.appendInline(visit(ctx.expression()));
			builder.append(TOKEN_COMMA);
		} else {
			builder.appendExpression(visit(ctx.expression()));
			builder.append(QueryTokens.expression(ctx.FROM()));
		}

		if (ctx.substringFunctionLengthArgument() != null) {

			if (ctx.FOR() == null) {
				builder.appendInline(visit(ctx.substringFunctionStartArgument()));
				builder.append(TOKEN_COMMA);
			} else {
				builder.appendExpression(visit(ctx.substringFunctionStartArgument()));
				builder.append(QueryTokens.expression(ctx.FOR()));
			}

			builder.append(visit(ctx.substringFunctionLengthArgument()));
		} else {
			builder.appendExpression(visit(ctx.substringFunctionStartArgument()));
		}

		return QueryTokenStream.ofFunction(ctx.SUBSTRING(), builder);
	}

	@Override
	public QueryTokenStream visitPadFunction(HqlParser.PadFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expression()));
		builder.append(QueryTokens.expression(ctx.WITH()));
		builder.appendExpression(visit(ctx.padLength()));

		if (ctx.padCharacter() != null) {
			builder.appendExpression(visit(ctx.padSpecification()));
			builder.appendExpression(visit(ctx.padCharacter()));
		} else {
			builder.appendExpression(visit(ctx.padSpecification()));
		}

		return QueryTokenStream.ofFunction(ctx.PAD(), builder);
	}

	@Override
	public QueryTokenStream visitPositionFunction(HqlParser.PositionFunctionContext ctx) {

		QueryRendererBuilder nested = QueryRenderer.builder();

		nested.appendExpression(visit(ctx.positionFunctionPatternArgument()));
		nested.append(QueryTokens.expression(ctx.IN()));
		nested.append(visit(ctx.positionFunctionStringArgument()));

		return QueryTokenStream.ofFunction(ctx.POSITION(), nested);
	}

	@Override
	public QueryTokenStream visitOverlayFunction(HqlParser.OverlayFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.token(ctx.OVERLAY()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendExpression(visit(ctx.overlayFunctionStringArgument()));
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
	public QueryTokenStream visitCurrentDateFunction(HqlParser.CurrentDateFunctionContext ctx) {

		if (ctx.CURRENT_DATE() != null) {
			return QueryTokenStream.ofFunction(ctx.CURRENT_DATE(), QueryTokenStream.empty());
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitCurrentTimeFunction(HqlParser.CurrentTimeFunctionContext ctx) {

		if (ctx.CURRENT_TIME() != null) {
			return QueryTokenStream.ofFunction(ctx.CURRENT_TIME(), QueryTokenStream.empty());
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitCurrentTimestampFunction(HqlParser.CurrentTimestampFunctionContext ctx) {

		if (ctx.CURRENT_TIMESTAMP() != null) {
			return QueryTokenStream.ofFunction(ctx.CURRENT_TIMESTAMP(), QueryTokenStream.empty());
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitInstantFunction(HqlParser.InstantFunctionContext ctx) {

		if (ctx.CURRENT_INSTANT() != null) {
			return QueryTokenStream.ofFunction(ctx.CURRENT_INSTANT(), QueryTokenStream.empty());
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitLocalDateTimeFunction(HqlParser.LocalDateTimeFunctionContext ctx) {

		if (ctx.LOCAL_DATETIME() != null) {
			return QueryTokenStream.ofFunction(ctx.LOCAL_DATETIME(), QueryTokenStream.empty());
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitOffsetDateTimeFunction(HqlParser.OffsetDateTimeFunctionContext ctx) {

		if (ctx.OFFSET_DATETIME() != null) {
			return QueryTokenStream.ofFunction(ctx.OFFSET_DATETIME(), QueryTokenStream.empty());
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitLocalDateFunction(HqlParser.LocalDateFunctionContext ctx) {

		if (ctx.LOCAL_DATE() != null) {
			return QueryTokenStream.ofFunction(ctx.LOCAL_DATE(), QueryTokenStream.empty());
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitLocalTimeFunction(HqlParser.LocalTimeFunctionContext ctx) {

		if (ctx.LOCAL_TIME() != null) {
			return QueryTokenStream.ofFunction(ctx.LOCAL_TIME(), QueryTokenStream.empty());
		}

		return QueryTokenStream.concatExpressions(ctx.children, this::visit);
	}

	@Override
	public QueryTokenStream visitFormatFunction(HqlParser.FormatFunctionContext ctx) {

		QueryRendererBuilder args = QueryRenderer.builder();

		args.appendExpression(visit(ctx.expression()));
		args.append(QueryTokens.expression(ctx.AS()));
		args.appendExpression(visit(ctx.format()));

		return QueryTokenStream.ofFunction(ctx.FORMAT(), args);
	}

	@Override
	public QueryTokenStream visitCollateFunction(HqlParser.CollateFunctionContext ctx) {

		QueryRendererBuilder args = QueryRenderer.builder();

		args.appendExpression(visit(ctx.expression()));
		args.append(QueryTokens.expression(ctx.AS()));
		args.appendExpression(visit(ctx.collation()));

		return QueryTokenStream.ofFunction(ctx.COLLATE(), args);
	}

	@Override
	public QueryTokenStream visitCube(HqlParser.CubeContext ctx) {
		return QueryTokenStream.ofFunction(ctx.CUBE(),
				QueryTokenStream.concat(ctx.expressionOrPredicate(), this::visit, TOKEN_COMMA));
	}

	@Override
	public QueryTokenStream visitRollup(HqlParser.RollupContext ctx) {
		return QueryTokenStream.ofFunction(ctx.ROLLUP(),
				QueryTokenStream.concat(ctx.expressionOrPredicate(), this::visit, TOKEN_COMMA));
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
		return QueryTokenStream.ofFunction(ctx.SIZE(), visit(ctx.path()));
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
	public QueryTokenStream visitJsonArrayFunction(HqlParser.JsonArrayFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(QueryTokenStream.concat(ctx.expressionOrPredicate(), this::visit, TOKEN_COMMA));

		if (ctx.jsonNullClause() != null) {
			builder.appendExpression(visit(ctx.jsonNullClause()));
		}

		return QueryTokenStream.ofFunction(ctx.JSON_ARRAY(), builder);
	}

	@Override
	public QueryTokenStream visitJsonExistsFunction(HqlParser.JsonExistsFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(QueryTokenStream.concat(ctx.expression(), this::visit, TOKEN_COMMA));

		if (ctx.jsonPassingClause() != null) {
			builder.appendExpression(visit(ctx.jsonPassingClause()));
		}

		if (ctx.jsonExistsOnErrorClause() != null) {
			builder.appendExpression(visit(ctx.jsonExistsOnErrorClause()));
		}

		return QueryTokenStream.ofFunction(ctx.JSON_EXISTS(), builder);
	}

	@Override
	public QueryTokenStream visitJsonObjectFunction(HqlParser.JsonObjectFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(QueryTokenStream.concat(ctx.jsonObjectFunctionEntry(), this::visit, TOKEN_COMMA));

		if (ctx.jsonNullClause() != null) {
			builder.appendExpression(visit(ctx.jsonNullClause()));
		}

		return QueryTokenStream.ofFunction(ctx.JSON_OBJECT(), builder);
	}

	@Override
	public QueryTokenStream visitJsonQueryFunction(HqlParser.JsonQueryFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(QueryTokenStream.concat(ctx.expression(), this::visit, TOKEN_COMMA));

		if (ctx.jsonPassingClause() != null) {
			builder.appendExpression(visit(ctx.jsonPassingClause()));
		}

		if (ctx.jsonQueryWrapperClause() != null) {
			builder.appendExpression(visit(ctx.jsonQueryWrapperClause()));
		}

		builder.append(QueryTokenStream.concat(ctx.jsonQueryOnErrorOrEmptyClause(), this::visit, TOKEN_SPACE));

		return QueryTokenStream.ofFunction(ctx.JSON_QUERY(), builder);
	}

	@Override
	public QueryTokenStream visitJsonValueFunction(HqlParser.JsonValueFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokenStream.concat(ctx.expression(), this::visit, TOKEN_COMMA));

		if (ctx.jsonPassingClause() != null) {
			builder.appendExpression(visit(ctx.jsonPassingClause()));
		}

		if (ctx.jsonValueReturningClause() != null) {
			builder.appendExpression(visit(ctx.jsonValueReturningClause()));
		}

		builder.append(QueryTokenStream.concat(ctx.jsonValueOnErrorOrEmptyClause(), this::visit, TOKEN_SPACE));

		return QueryTokenStream.ofFunction(ctx.JSON_VALUE(), builder);
	}

	@Override
	public QueryTokenStream visitJsonArrayAggFunction(HqlParser.JsonArrayAggFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.expressionOrPredicate()));

		if (ctx.jsonNullClause() != null) {
			builder.appendExpression(visit(ctx.jsonNullClause()));
		}

		if (ctx.orderByClause() != null) {
			builder.appendExpression(visit(ctx.orderByClause()));
		}

		QueryTokenStream function = QueryTokenStream.ofFunction(ctx.JSON_ARRAYAGG(), builder);

		if (ctx.filterClause() == null) {
			return function;
		}

		QueryRendererBuilder functionWithFilter = QueryRenderer.builder();
		functionWithFilter.appendExpression(function);
		functionWithFilter.appendExpression(visit(ctx.filterClause()));

		return functionWithFilter.build();
	}

	@Override
	public QueryTokenStream visitJsonObjectAggFunction(HqlParser.JsonObjectAggFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.KEY() != null) {
			builder.append(QueryTokens.expression(ctx.KEY()));
		}

		builder.appendExpression(visit(ctx.expressionOrPredicate(0)));

		if (ctx.VALUE() != null) {
			builder.append(QueryTokens.expression(ctx.VALUE()));
		} else {
			builder.append(TOKEN_COLON);
		}

		builder.appendExpression(visit(ctx.expressionOrPredicate(1)));

		if (ctx.jsonNullClause() != null) {
			builder.appendExpression(visit(ctx.jsonNullClause()));
		}

		if (ctx.jsonUniqueKeysClause() != null) {
			builder.appendExpression(visit(ctx.jsonUniqueKeysClause()));
		}

		QueryTokenStream function = QueryTokenStream.ofFunction(ctx.JSON_OBJECTAGG(), builder);

		if (ctx.filterClause() == null) {
			return function;
		}

		QueryRendererBuilder functionWithFilter = QueryRenderer.builder();
		functionWithFilter.appendExpression(function);
		functionWithFilter.appendExpression(visit(ctx.filterClause()));

		return functionWithFilter.build();
	}

	@Override
	public QueryTokenStream visitJsonPassingClause(HqlParser.JsonPassingClauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(QueryTokens.expression(ctx.PASSING()));
		builder.append(QueryTokenStream.concat(ctx.jsonPassingItem(), this::visit, TOKEN_COMMA));

		return builder;
	}

	@Override
	public QueryTokenStream visitJsonTableFunction(HqlParser.JsonTableFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(QueryTokenStream.concat(ctx.expression(), this::visit, TOKEN_COMMA));

		if (ctx.jsonPassingClause() != null) {
			builder.appendExpression(visit(ctx.jsonPassingClause()));
		}

		builder.appendExpression(visit(ctx.jsonTableColumnsClause()));

		if (ctx.jsonTableErrorClause() != null) {
			builder.appendExpression(visit(ctx.jsonTableErrorClause()));
		}

		return QueryTokenStream.ofFunction(ctx.JSON_TABLE(), builder);
	}

	@Override
	public QueryTokenStream visitJsonTableColumnsClause(HqlParser.JsonTableColumnsClauseContext ctx) {
		return QueryTokenStream.ofFunction(ctx.COLUMNS(), visit(ctx.jsonTableColumns()));
	}

	@Override
	public QueryTokenStream visitJsonTableColumns(HqlParser.JsonTableColumnsContext ctx) {
		return QueryTokenStream.concat(ctx.jsonTableColumn(), this::visit, TOKEN_COMMA);
	}

	@Override
	public QueryTokenStream visitPath(HqlParser.PathContext ctx) {
		return QueryTokenStream.concat(ctx.children, this::visit, EMPTY_TOKEN);
	}

	@Override
	public QueryTokenStream visitGeneralPathFragment(HqlParser.GeneralPathFragmentContext ctx) {
		return QueryTokenStream.concat(ctx.children, this::visit, EMPTY_TOKEN);
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
		return visit(ctx.identifier());
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

		if (ctx.pathContinuation() != null) {
			builder.append(visit(ctx.pathContinuation()));
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
		builder.appendInline(QueryTokenStream.group(nested));

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
	public QueryTokenStream visitCastFunction(HqlParser.CastFunctionContext ctx) {

		QueryRendererBuilder nested = QueryRenderer.builder();
		nested.appendExpression(visit(ctx.expression()));
		nested.append(QueryTokens.expression(ctx.AS()));
		nested.appendExpression(visit(ctx.castTarget()));

		return QueryTokenStream.ofFunction(ctx.CAST(), nested);
	}

	@Override
	public QueryTokenStream visitCastTarget(HqlParser.CastTargetContext ctx) {

		List<TerminalNode> literals = ctx.INTEGER_LITERAL();

		if (!CollectionUtils.isEmpty(literals)) {

			QueryRendererBuilder builder = QueryRenderer.builder();
			builder.append(visit(ctx.castTargetType()));
			builder.append(TOKEN_OPEN_PAREN);

			QueryRendererBuilder args = QueryRenderer.builder();
			for (int i = 0; i < literals.size(); i++) {
				if (i > 0) {
					args.append(TOKEN_COMMA);
				}
				args.append(QueryTokens.token(literals.get(i)));
			}

			builder.appendInline(args.build());
			builder.append(TOKEN_CLOSE_PAREN);

			return builder.build();
		}

		return visit(ctx.castTargetType());
	}

	@Override
	public QueryTokenStream visitCastTargetType(HqlParser.CastTargetTypeContext ctx) {
		return QueryTokens.token(ctx.fullTargetName);
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
			nested.appendExpression(visit(ctx.expression()));

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
	public QueryTokenStream visitTrimFunction(HqlParser.TrimFunctionContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

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

		return QueryTokenStream.ofFunction(ctx.TRIM(), builder);
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

			builder.append(visit(ctx.collectionQuantifier()));

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

			builder.append(visit(ctx.collectionQuantifier()));

			builder.append(TOKEN_OPEN_PAREN);
			builder.appendInline(visit(ctx.simplePath()));
			builder.append(TOKEN_CLOSE_PAREN);
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitTreatedNavigablePath(HqlParser.TreatedNavigablePathContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		QueryRendererBuilder nested = QueryRenderer.builder();

		nested.appendExpression(visit(ctx.path()));
		nested.append(QueryTokens.expression(ctx.AS()));
		nested.append(visit(ctx.simplePath()));

		builder.append(QueryTokenStream.ofFunction(ctx.TREAT(), nested));

		if (ctx.pathContinuation() != null) {
			builder.append(visit(ctx.pathContinuation()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitCollectionValueNavigablePath(HqlParser.CollectionValueNavigablePathContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.elementValueQuantifier()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.path()));
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinuation() != null) {
			builder.append(visit(ctx.pathContinuation()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitMapKeyNavigablePath(HqlParser.MapKeyNavigablePathContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(visit(ctx.indexKeyQuantifier()));
		builder.append(TOKEN_OPEN_PAREN);
		builder.append(visit(ctx.path()));
		builder.append(TOKEN_CLOSE_PAREN);

		if (ctx.pathContinuation() != null) {
			builder.append(visit(ctx.pathContinuation()));
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitToOneFkReference(HqlParser.ToOneFkReferenceContext ctx) {
		return QueryTokenStream.ofFunction(ctx.FK(), visit(ctx.path()));
	}

	@Override
	public QueryTokenStream visitGroupedPredicate(HqlParser.GroupedPredicateContext ctx) {
		return QueryTokenStream.group(visit(ctx.predicate()));
	}

	@Override
	public QueryTokenStream visitInList(HqlParser.InListContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		if (ctx.simplePath() != null) {

			if (ctx.ELEMENTS() != null) {
				builder.append(QueryTokens.token(ctx.ELEMENTS()));
			} else if (ctx.INDICES() != null) {
				builder.append(QueryTokens.token(ctx.INDICES()));
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
				builder.append(QueryTokens.token(ctx.ELEMENTS()));
			} else if (ctx.INDICES() != null) {
				builder.append(QueryTokens.token(ctx.INDICES()));
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
	public QueryTokenStream visitInstantiationArguments(HqlParser.InstantiationArgumentsContext ctx) {
		return QueryTokenStream.concat(ctx.instantiationArgument(), this::visit, TOKEN_COMMA);
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
				builder.append(QueryTokens.token(ctx.INTEGER_LITERAL()));
			}
		}

		return builder;
	}

	@Override
	public QueryTokenStream visitEntityName(HqlParser.EntityNameContext ctx) {
		return QueryTokenStream.concat(ctx.identifier(), this::visit, TOKEN_DOT);
	}

	@Override
	public QueryTokenStream visitChildren(RuleNode node) {

		int childCount = node.getChildCount();

		if (childCount == 1 && node.getChild(0) instanceof RuleContext t) {
			return visit(t);
		}

		if (childCount == 1 && node.getChild(0) instanceof TerminalNode t) {
			return QueryTokens.token(t);
		}

		return QueryTokenStream.concatExpressions(node, this::visit);
	}

}
