/*
 * Copyright 2023 the original author or authors.
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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;

import java.util.HexFormat;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.JpaSort;

/**
 * Parses the content of {@link JpaSort#unsafe(String...)} as an HQL {@literal sortExpression} and renders that into a
 * JPA Criteria {@link Expression}.
 *
 * @author Greg Turnquist
 * @since 3.2
 */
class HqlOrderByExtractor extends HqlBaseVisitor<JpaOrderByToken> {

	private CriteriaBuilder cb;
	private From<?, ?> from;

	private static String UNSUPPORTED_TEMPLATE = "We can't handle %s in an ORDER BY clause through JpaSort.unsafe";

	HqlOrderByExtractor(CriteriaBuilder cb, From<?, ?> from) {

		this.cb = cb;
		this.from = from;
	}

	/**
	 * Extract the {@link org.springframework.data.jpa.domain.JpaSort.JpaOrder}'s property and parse it as an HQL
	 * {@literal sortExpression}.
	 *
	 * @param jpaOrder
	 * @return criteriaExpression
	 * @since 3.2
	 */
	Expression<?> extractCriteriaExpression(JpaSort.JpaOrder jpaOrder) {

		HqlLexer jpaOrderLexer = new HqlLexer(CharStreams.fromString(jpaOrder.getProperty()));
		HqlParser jpaOrderParser = new HqlParser(new CommonTokenStream(jpaOrderLexer));

		return expression(visit(jpaOrderParser.sortExpression()));
	}

	/**
	 * Given a particular {@link JpaOrderByToken}, transform it into a Jakarta {@link Expression}.
	 *
	 * @param token
	 * @return Expression
	 */
	private Expression<?> expression(JpaOrderByToken token) {

		if (token instanceof JpaOrderByExpressionToken expressionToken) {
			return expressionToken.expression();
		} else if (token instanceof JpaOrderByNamedToken namedToken) {
			return from.get(namedToken.token());
		} else {
			throw new IllegalArgumentException("We can't handle a " + token.getClass() + "!");
		}
	}

	/**
	 * Convert a generic {@link JpaOrderByToken} token into a {@link JpaOrderByNamedToken} and then extract its string
	 * token value.
	 *
	 * @param token
	 * @return string value
	 * @since 3.2
	 */
	private String token(JpaOrderByToken token) {

		if (token instanceof JpaOrderByNamedToken namedToken) {
			return namedToken.token();
		} else {
			throw new IllegalArgumentException("We can't handle a " + token.getClass() + "!");
		}
	}

	@Override
	public JpaOrderByToken visitSortExpression(HqlParser.SortExpressionContext ctx) {

		if (ctx.identifier() != null) {
			return visit(ctx.identifier());
		} else if (ctx.INTEGER_LITERAL() != null) {
			return new JpaOrderByExpressionToken(cb.literal(Integer.valueOf(ctx.INTEGER_LITERAL().getText())));
		} else if (ctx.expression() != null) {
			return visit(ctx.expression());
		} else {
			return null;
		}
	}

	@Override
	public JpaOrderByToken visitRelationalExpression(HqlParser.RelationalExpressionContext ctx) {

		Expression<Comparable> left = (Expression<Comparable>) expression(visit(ctx.expression(0)));
		Expression<Comparable> right = (Expression<Comparable>) expression(visit(ctx.expression(1)));

		if (ctx.op.getText().equals("=")) {
			return new JpaOrderByExpressionToken(cb.equal(left, right));
		} else if (ctx.op.getText().equals(">")) {
			return new JpaOrderByExpressionToken(cb.greaterThan(left, right));
		} else if (ctx.op.getText().equals(">=")) {
			return new JpaOrderByExpressionToken(cb.greaterThanOrEqualTo(left, right));
		} else if (ctx.op.getText().equals("<")) {
			return new JpaOrderByExpressionToken(cb.lessThan(left, right));
		} else if (ctx.op.getText().equals("<=")) {
			return new JpaOrderByExpressionToken(cb.lessThanOrEqualTo(left, right));
		} else if (ctx.op.getText().equals("<>") || ctx.op.getText().equals("!=") || ctx.op.getText().equals("^=")) {
			return new JpaOrderByExpressionToken(cb.notEqual(left, right));
		} else {
			return null;
		}
	}

	@Override
	public JpaOrderByToken visitBetweenExpression(HqlParser.BetweenExpressionContext ctx) {

		Expression<Comparable> condition = (Expression<Comparable>) expression(visit(ctx.expression(0)));
		Expression<Comparable> lower = (Expression<Comparable>) expression(visit(ctx.expression(1)));
		Expression<Comparable> upper = (Expression<Comparable>) expression(visit(ctx.expression(2)));

		if (ctx.NOT() == null) {
			return new JpaOrderByExpressionToken(cb.between(condition, lower, upper));
		} else {
			return new JpaOrderByExpressionToken(cb.between(condition, lower, upper).not());
		}
	}

	@Override
	public JpaOrderByToken visitDealingWithNullExpression(HqlParser.DealingWithNullExpressionContext ctx) {

		if (ctx.NULL() != null) {

			Expression<?> condition = expression(visit(ctx.expression(0)));

			if (ctx.NOT() == null) {
				return new JpaOrderByExpressionToken(cb.isNull(condition));
			} else {
				return new JpaOrderByExpressionToken(cb.isNotNull(condition));
			}
		} else {

			Expression<?> left = expression(visit(ctx.expression(0)));
			Expression<?> right = expression(visit(ctx.expression(1)));

			HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;

			if (ctx.NOT() == null) {
				return new JpaOrderByExpressionToken(hcb.distinctFrom(left, right));
			} else {
				return new JpaOrderByExpressionToken(hcb.notDistinctFrom(left, right));
			}
		}
	}

	@Override
	public JpaOrderByToken visitStringPatternMatching(HqlParser.StringPatternMatchingContext ctx) {

		Expression<String> condition = (Expression<String>) expression(visit(ctx.expression(0)));
		Expression<String> match = (Expression<String>) expression(visit(ctx.expression(1)));
		Expression<Character> escape = ctx.ESCAPE() != null && ctx.stringLiteral() != null //
				? (Expression<Character>) expression(visit(ctx.stringLiteral())) //
				: null;

		if (ctx.LIKE() != null) {

			if (ctx.NOT() == null) {
				return escape == null //
						? new JpaOrderByExpressionToken(cb.like(condition, match)) //
						: new JpaOrderByExpressionToken(cb.like(condition, match, escape));
			} else {
				return escape == null //
						? new JpaOrderByExpressionToken(cb.notLike(condition, match)) //
						: new JpaOrderByExpressionToken(cb.notLike(condition, match, escape));
			}
		} else {

			HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;

			if (ctx.NOT() == null) {
				return escape == null //
						? new JpaOrderByExpressionToken(hcb.ilike(condition, match)) //
						: new JpaOrderByExpressionToken(hcb.ilike(condition, match, escape));
			} else {
				return escape == null //
						? new JpaOrderByExpressionToken(hcb.notIlike(condition, match)) //
						: new JpaOrderByExpressionToken(hcb.notIlike(condition, match, escape));
			}
		}

	}

	@Override
	public JpaOrderByToken visitInExpression(HqlParser.InExpressionContext ctx) {

		if (ctx.inList().simplePath() != null) {
			throw new UnsupportedOperationException(
					String.format(UNSUPPORTED_TEMPLATE, "IN clause with ELEMENTS or INDICES argument"));
		} else if (ctx.inList().subquery() != null) {
			throw new UnsupportedOperationException(String.format(UNSUPPORTED_TEMPLATE, "IN clause with a subquery"));
		} else if (ctx.inList().parameter() != null) {
			throw new UnsupportedOperationException(String.format(UNSUPPORTED_TEMPLATE, "IN clause with a parameter"));
		}

		CriteriaBuilder.In<Object> in = cb.in(expression(visit(ctx.expression())));

		ctx.inList().expressionOrPredicate()
				.forEach(expressionOrPredicateContext -> in.value(expression(visit(expressionOrPredicateContext))));

		if (ctx.NOT() == null) {
			return new JpaOrderByExpressionToken(in);
		} else {
			return new JpaOrderByExpressionToken(in.not());
		}
	}

	@Override
	public JpaOrderByToken visitGenericFunction(HqlParser.GenericFunctionContext ctx) {

		String functionName = token(visit(ctx.functionName()));

		if (ctx.functionArguments() == null) {
			return new JpaOrderByExpressionToken(cb.function(functionName, Object.class));
		} else {

			Expression<?>[] arguments = ctx.functionArguments().expressionOrPredicate().stream() //
					.map(expressionOrPredicateContext -> expression(visit(expressionOrPredicateContext))) //
					.toArray(Expression[]::new);

			return new JpaOrderByExpressionToken(cb.function(functionName, Object.class, arguments));
		}
	}

	@Override
	public JpaOrderByToken visitFunctionName(HqlParser.FunctionNameContext ctx) {

		String functionName = ctx.reservedWord().stream() //
				.map(reservedWordContext -> token(visit(reservedWordContext))) //
				.collect(Collectors.joining("."));

		return new JpaOrderByNamedToken(functionName);
	}

	@Override
	public JpaOrderByToken visitTrimFunction(HqlParser.TrimFunctionContext ctx) {

		CriteriaBuilder.Trimspec trimSpec = null;

		if (ctx.LEADING() != null) {
			trimSpec = CriteriaBuilder.Trimspec.LEADING;
		} else if (ctx.TRAILING() != null) {
			trimSpec = CriteriaBuilder.Trimspec.TRAILING;
		} else if (ctx.BOTH() != null) {
			trimSpec = CriteriaBuilder.Trimspec.BOTH;
		}

		Expression<?> stringLiteral = ctx.stringLiteral() != null //
				? expression(visit(ctx.stringLiteral())) //
				: null;

		Expression<?> expression = expression(visit(ctx.expression()));

		if (trimSpec != null) {
			return stringLiteral != null //
					? new JpaOrderByExpressionToken(
							cb.trim(trimSpec, (Expression<Character>) stringLiteral, (Expression<String>) expression)) //
					: new JpaOrderByExpressionToken(cb.trim(trimSpec, (Expression<String>) expression));
		} else {
			return stringLiteral != null //
					? new JpaOrderByExpressionToken(
							cb.trim((Expression<Character>) stringLiteral, (Expression<String>) expression)) //
					: new JpaOrderByExpressionToken(cb.trim((Expression<String>) expression));
		}
	}

	@Override
	public JpaOrderByToken visitLiteral(HqlParser.LiteralContext ctx) {

		if (ctx.NULL() != null) {
			return new JpaOrderByNamedToken(ctx.getText());
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
			return null;
		}
	}

	@Override
	public JpaOrderByToken visitBooleanLiteral(HqlParser.BooleanLiteralContext ctx) {

		if (ctx.TRUE() != null) {
			return new JpaOrderByExpressionToken(cb.literal(true));
		} else {
			return new JpaOrderByExpressionToken(cb.literal(false));
		}
	}

	@Override
	public JpaOrderByToken visitStringLiteral(HqlParser.StringLiteralContext ctx) {

		if (ctx.STRINGLITERAL() != null) {

			String literal = ctx.STRINGLITERAL().getText();
			return new JpaOrderByExpressionToken(cb.literal(literal.substring(1, literal.length() - 1)));

		} else if (ctx.JAVASTRINGLITERAL() != null) {

			String literal = ctx.JAVASTRINGLITERAL().getText();
			return new JpaOrderByExpressionToken(cb.literal(literal.substring(1, literal.length() - 1)));

		} else if (ctx.CHARACTER() != null) {
			// Skip over the single quote
			return new JpaOrderByExpressionToken(cb.literal(ctx.CHARACTER().getText().charAt(1)));
		} else {
			return null;
		}
	}

	@Override
	public JpaOrderByToken visitNumericLiteral(HqlParser.NumericLiteralContext ctx) {

		if (ctx.INTEGER_LITERAL() != null) {
			return new JpaOrderByExpressionToken(cb.literal(Integer.valueOf(ctx.INTEGER_LITERAL().getText())));
		} else if (ctx.FLOAT_LITERAL() != null) {
			return new JpaOrderByExpressionToken(cb.literal(Float.valueOf(ctx.FLOAT_LITERAL().getText())));
		} else if (ctx.HEXLITERAL() != null) {
			return new JpaOrderByExpressionToken(cb.literal(HexFormat.fromHexDigits(ctx.HEXLITERAL().toString())));
		} else {
			return null;
		}
	}

	@Override
	public JpaOrderByToken visitDateTimeLiteral(HqlParser.DateTimeLiteralContext ctx) {

		if (ctx.LOCAL_DATE() != null) {
			return new JpaOrderByNamedToken(ctx.LOCAL_DATE().getText());
		} else if (ctx.LOCAL_TIME() != null) {
			return new JpaOrderByNamedToken(ctx.LOCAL_TIME().getText());
		} else if (ctx.LOCAL_DATETIME() != null) {
			return new JpaOrderByNamedToken(ctx.LOCAL_DATETIME().getText());
		} else if (ctx.CURRENT_DATE() != null) {
			return new JpaOrderByNamedToken(ctx.CURRENT_DATE().getText());
		} else if (ctx.CURRENT_TIME() != null) {
			return new JpaOrderByNamedToken(ctx.CURRENT_TIME().getText());
		} else if (ctx.CURRENT_TIMESTAMP() != null) {
			return new JpaOrderByNamedToken(ctx.CURRENT_TIMESTAMP().getText());
		}

		if (ctx.DATE() != null) {
			if (ctx.LOCAL() != null) {
				return new JpaOrderByNamedToken(ctx.LOCAL().getText() + " " + ctx.DATE().getText());
			} else {
				return new JpaOrderByNamedToken(ctx.CURRENT().getText() + " " + ctx.TIME().getText());
			}
		}

		if (ctx.DATETIME() != null) {
			if (ctx.LOCAL() != null) {
				return new JpaOrderByNamedToken(ctx.LOCAL().getText() + " " + ctx.DATETIME().getText());
			} else if (ctx.CURRENT() != null) {
				return new JpaOrderByNamedToken(ctx.CURRENT().getText() + " " + ctx.DATETIME().getText());
			} else {
				return new JpaOrderByNamedToken(ctx.OFFSET().getText() + " " + ctx.DATETIME().getText());

			}
		}

		return new JpaOrderByNamedToken(ctx.INSTANT().getText());
	}

	@Override
	public JpaOrderByToken visitGroupedExpression(HqlParser.GroupedExpressionContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public JpaOrderByToken visitTupleExpression(HqlParser.TupleExpressionContext ctx) {
		throw new UnsupportedOperationException(String.format(UNSUPPORTED_TEMPLATE, "a TUPLE argument"));
	}

	@Override
	public JpaOrderByToken visitSubqueryExpression(HqlParser.SubqueryExpressionContext ctx) {
		throw new UnsupportedOperationException(String.format(UNSUPPORTED_TEMPLATE, "a subquery argument"));
	}

	@Override
	public JpaOrderByToken visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {

		Expression<Number> left = (Expression<Number>) expression(visit(ctx.expression(0)));
		Expression<Number> right = (Expression<Number>) expression(visit(ctx.expression(1)));

		if (ctx.op.getText().equals("*")) {
			return new JpaOrderByExpressionToken(cb.prod(left, right));
		} else {
			return new JpaOrderByExpressionToken(cb.quot(left, right));
		}
	}

	@Override
	public JpaOrderByToken visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {

		Expression<Number> left = (Expression<Number>) expression(visit(ctx.expression(0)));
		Expression<Number> right = (Expression<Number>) expression(visit(ctx.expression(1)));

		if (ctx.op.getText().equals("+")) {
			return new JpaOrderByExpressionToken(cb.sum(left, right));
		} else {
			return new JpaOrderByExpressionToken(cb.diff(left, right));
		}
	}

	@Override
	public JpaOrderByToken visitHqlConcatenationExpression(HqlParser.HqlConcatenationExpressionContext ctx) {

		Expression<String> left = (Expression<String>) expression(visit(ctx.expression(0)));
		Expression<String> right = (Expression<String>) expression(visit(ctx.expression(1)));

		return new JpaOrderByExpressionToken(cb.concat(left, right));
	}

	@Override
	public JpaOrderByToken visitSimplePath(HqlParser.SimplePathContext ctx) {

		Path<?> simplePath = (Path<?>) expression(visit(ctx.identifier()));

		for (HqlParser.SimplePathElementContext simplePathElementContext : ctx.simplePathElement()) {
			simplePath = simplePath.get(token(visit(simplePathElementContext)));
		}

		return new JpaOrderByExpressionToken(simplePath);
	}

	@Override
	public JpaOrderByToken visitCaseList(HqlParser.CaseListContext ctx) {

		if (ctx.simpleCaseExpression() != null) {
			return visit(ctx.simpleCaseExpression());
		} else {
			return visit(ctx.searchedCaseExpression());
		}
	}

	@Override
	public JpaOrderByToken visitSimpleCaseExpression(HqlParser.SimpleCaseExpressionContext ctx) {

		CriteriaBuilder.SimpleCase<Object, Object> simpleCase = cb
				.selectCase(expression(visit(ctx.expressionOrPredicate(0))));

		ctx.caseWhenExpressionClause().forEach(caseWhenExpressionClauseContext -> {
			simpleCase.when( //
					expression(visit(caseWhenExpressionClauseContext.expression())), //
					expression(visit(caseWhenExpressionClauseContext.expressionOrPredicate())));
		});

		if (ctx.expressionOrPredicate().size() == 2) {
			simpleCase.otherwise(expression(visit(ctx.expressionOrPredicate(1))));
		}

		return new JpaOrderByExpressionToken(simpleCase);
	}

	@Override
	public JpaOrderByToken visitSearchedCaseExpression(HqlParser.SearchedCaseExpressionContext ctx) {

		CriteriaBuilder.Case<Object> searchedCase = cb.selectCase();

		ctx.caseWhenPredicateClause().forEach(caseWhenPredicateClauseContext -> {
			searchedCase.when( //
					(Expression<Boolean>) expression(visit(caseWhenPredicateClauseContext.predicate())), //
					expression(visit(caseWhenPredicateClauseContext.expressionOrPredicate())));
		});

		if (ctx.expressionOrPredicate() != null) {
			searchedCase.otherwise(expression(visit(ctx.expressionOrPredicate())));
		}

		return new JpaOrderByExpressionToken(searchedCase);
	}

	@Override
	public JpaOrderByToken visitParameter(HqlParser.ParameterContext ctx) {
		throw new UnsupportedOperationException(String.format(UNSUPPORTED_TEMPLATE, "a parameter argument"));
	}

	@Override
	public JpaOrderByToken visitReservedWord(HqlParser.ReservedWordContext ctx) {

		if (ctx.IDENTIFICATION_VARIABLE() != null) {
			return new JpaOrderByNamedToken(ctx.IDENTIFICATION_VARIABLE().getText());
		} else {
			return new JpaOrderByNamedToken(ctx.f.getText());
		}
	}
}
