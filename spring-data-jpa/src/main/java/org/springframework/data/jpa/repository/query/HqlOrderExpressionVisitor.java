/*
 * Copyright 2025 the original author or authors.
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

import static java.time.format.DateTimeFormatter.*;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.LocalDateTimeField;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.TemporalField;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.BiFunction;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.util.Assert;

/**
 * Parses the content of {@link JpaSort#unsafe(String...)} as an HQL {@literal sortExpression} and renders that into a
 * JPA Criteria {@link Expression}.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 4.0
 */
@SuppressWarnings({ "unchecked", "rawtypes", "ConstantValue", "NullAway" })
class HqlOrderExpressionVisitor extends HqlBaseVisitor<Expression<?>> {

	private static final DateTimeFormatter DATE_TIME = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.append(ISO_LOCAL_DATE).optionalStart().appendLiteral(' ').optionalEnd().optionalStart().appendLiteral('T')
			.optionalEnd().append(ISO_LOCAL_TIME).optionalStart().appendLiteral(' ').optionalEnd().optionalStart()
			.appendZoneOrOffsetId().optionalEnd().toFormatter();

	private static final DateTimeFormatter DATE_TIME_FORMATTER_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd",
			Locale.ENGLISH);

	private static final DateTimeFormatter DATE_TIME_FORMATTER_TIME = DateTimeFormatter.ofPattern("HH:mm:ss",
			Locale.ENGLISH);

	private static final String UNSUPPORTED_TEMPLATE = "We can't handle %s in an ORDER BY clause through JpaSort.unsafe(…)";

	private final CriteriaBuilder cb;
	private final Path<?> from;
	private final BiFunction<From<?, ?>, PropertyPath, Expression<?>> expressionFactory;

	/**
	 * @param cb criteria builder.
	 * @param from from path (i.e. root entity).
	 * @param expressionFactory factory to create expressions such as
	 *          {@link QueryUtils#toExpressionRecursively(From, PropertyPath)}.
	 */
	HqlOrderExpressionVisitor(CriteriaBuilder cb, Path<?> from,
			BiFunction<From<?, ?>, PropertyPath, Expression<?>> expressionFactory) {
		this.cb = cb;
		this.from = from;
		this.expressionFactory = expressionFactory;
	}

	/**
	 * Extract the {@link org.springframework.data.jpa.domain.JpaSort.JpaOrder}'s property and parse it as an HQL
	 * {@literal sortExpression}.
	 *
	 * @param jpaOrder must not be {@literal null}.
	 * @return criteriaExpression
	 * @throws IllegalArgumentException thrown if the order yields no sort expression.
	 * @throws UnsupportedOperationException thrown if the order contains an unsupported expression.
	 * @throws BadJpqlGrammarException thrown if the order contains a syntax errors.
	 */
	Expression<?> createCriteriaExpression(Sort.Order jpaOrder) {

		String orderByProperty = jpaOrder.getProperty();
		HqlLexer lexer = new HqlLexer(CharStreams.fromString(orderByProperty));
		HqlParser parser = new HqlParser(new CommonTokenStream(lexer));

		JpaQueryEnhancer.configureParser(orderByProperty, "ORDER BY expression", lexer, parser);

		HqlParser.SortExpressionContext ctx = parser.sortExpression();

		if (ctx == null) {
			throw new IllegalArgumentException("No sort expression provided");
		}

		return visitRequired(ctx);
	}

	@Override
	public @Nullable Expression<?> visitSortExpression(HqlParser.SortExpressionContext ctx) {

		if (ctx.identifier() != null) {
			HqlParser.IdentifierContext identifier = ctx.identifier();

			return from.get(getString(identifier));
		} else if (ctx.INTEGER_LITERAL() != null) {
			return cb.literal(Integer.valueOf(ctx.INTEGER_LITERAL().getText()));
		} else if (ctx.expression() != null) {
			return visitRequired(ctx.expression());
		} else {
			return null;
		}
	}

	@Override
	public Expression<?> visitRelationalExpression(HqlParser.RelationalExpressionContext ctx) {

		Expression<Comparable> left = visitRequired(ctx.expression(0));
		Expression<Comparable> right = visitRequired(ctx.expression(1));
		String op = ctx.op.getText();

		return switch (op) {
			case "=" -> cb.equal(left, right);
			case ">" -> cb.greaterThan(left, right);
			case ">=" -> cb.greaterThanOrEqualTo(left, right);
			case "<" -> cb.lessThan(left, right);
			case "<=" -> cb.lessThanOrEqualTo(left, right);
			case "<>", "!=", "^=" -> cb.notEqual(left, right);
			default -> throw new UnsupportedOperationException("Unsupported comparison operator: " + op);
		};
	}

	@Override
	public Expression<?> visitBetweenExpression(HqlParser.BetweenExpressionContext ctx) {

		Expression<Comparable> condition = visitRequired(ctx.expression(0));
		Expression<Comparable> lower = visitRequired(ctx.expression(1));
		Expression<Comparable> upper = visitRequired(ctx.expression(2));

		if (ctx.NOT() == null) {
			return cb.between(condition, lower, upper);
		} else {
			return cb.between(condition, lower, upper).not();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Expression<?> visitIsBooleanPredicate(HqlParser.IsBooleanPredicateContext ctx) {

		Expression<?> condition = visitRequired(ctx.expression());

		if (ctx.NULL() != null) {
			if (ctx.NOT() == null) {
				return cb.isNull(condition);
			} else {
				return cb.isNotNull(condition);
			}
		}

		if (ctx.EMPTY() != null) {
			if (ctx.NOT() == null) {
				return cb.isEmpty((Expression<? extends Collection<?>>) condition);
			} else {
				return cb.isNotEmpty((Expression<? extends Collection<?>>) condition);
			}
		}

		if (ctx.TRUE() != null) {
			if (ctx.NOT() == null) {
				return cb.isTrue((Expression<Boolean>) condition);
			} else {
				return cb.isFalse((Expression<Boolean>) condition);
			}
		}

		if (ctx.FALSE() != null) {
			if (ctx.NOT() == null) {
				return cb.isFalse((Expression<Boolean>) condition);
			} else {
				return cb.isTrue((Expression<Boolean>) condition);
			}
		}

		return null;
	}

	@Override
	public Expression<?> visitStringPatternMatching(HqlParser.StringPatternMatchingContext ctx) {
		Expression<String> condition = visitRequired(ctx.expression(0));
		Expression<String> match = visitRequired(ctx.expression(1));
		Expression<Character> escape = ctx.ESCAPE() != null ? charLiteralOf(ctx.ESCAPE()) : null;

		if (ctx.LIKE() != null) {
			if (ctx.NOT() == null) {
				return escape == null //
						? cb.like(condition, match) //
						: cb.like(condition, match, escape);
			} else {
				return escape == null //
						? cb.notLike(condition, match) //
						: cb.notLike(condition, match, escape);
			}
		} else {
			HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
			if (ctx.NOT() == null) {
				return escape == null //
						? hcb.ilike(condition, match) //
						: hcb.ilike(condition, match, escape);
			} else {
				return escape == null //
						? hcb.notIlike(condition, match) //
						: hcb.notIlike(condition, match, escape);
			}
		}
	}

	@Override
	public Expression<?> visitInExpression(HqlParser.InExpressionContext ctx) {

		if (ctx.inList().simplePath() != null) {
			throw new UnsupportedOperationException(
					String.format(UNSUPPORTED_TEMPLATE, "IN clause with ELEMENTS or INDICES argument"));
		} else if (ctx.inList().subquery() != null) {
			throw new UnsupportedOperationException(String.format(UNSUPPORTED_TEMPLATE, "IN clause with a subquery"));
		} else if (ctx.inList().parameter() != null) {
			throw new UnsupportedOperationException(String.format(UNSUPPORTED_TEMPLATE, "IN clause with a parameter"));
		}

		CriteriaBuilder.In<Object> in = cb.in(visit(ctx.expression()));

		ctx.inList().expressionOrPredicate()
				.forEach(expressionOrPredicateContext -> in.value(visit(expressionOrPredicateContext)));

		if (ctx.NOT() == null) {
			return in;
		}
		return in.not();

	}

	@Override
	public Expression<?> visitGenericFunction(HqlParser.GenericFunctionContext ctx) {

		String functionName = ctx.genericFunctionName().getText();

		if (ctx.genericFunctionArguments() == null) {
			return cb.function(functionName, Object.class);
		}

		Expression<?>[] arguments = ctx.genericFunctionArguments().expressionOrPredicate().stream() //
				.map(this::visitRequired) //
				.toArray(Expression[]::new);
		return cb.function(functionName, Object.class, arguments);

	}

	@Override
	public Expression<?> visitCastFunction(HqlParser.CastFunctionContext ctx) {
		throw new UnsupportedOperationException("Sorting using CAST ist not supported");
	}

	@Override
	public Expression<?> visitTreatedNavigablePath(HqlParser.TreatedNavigablePathContext ctx) {
		throw new UnsupportedOperationException("Sorting using TREAT ist not supported");
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Expression<?> visitExtractFunction(HqlParser.ExtractFunctionContext ctx) {

		Expression expr = visitRequired(ctx.expression());
		TemporalField temporalField = ctx.extractField() != null ? getTemporalField(ctx.extractField())
				: getTemporalField(ctx.datetimeField());

		return cb.extract(temporalField, expr);
	}

	private TemporalField<?, ? extends Temporal> getTemporalField(HqlParser.DatetimeFieldContext ctx) {

		if (ctx.YEAR() != null) {
			return LocalDateTimeField.YEAR;
		}

		if (ctx.MONTH() != null) {
			return LocalDateTimeField.MONTH;
		}

		if (ctx.QUARTER() != null) {
			return LocalDateTimeField.QUARTER;
		}

		if (ctx.WEEK() != null) {
			return LocalDateTimeField.WEEK;
		}

		if (ctx.DAY() != null) {
			return LocalDateTimeField.DAY;
		}

		if (ctx.HOUR() != null) {
			return LocalDateTimeField.HOUR;
		}

		if (ctx.MINUTE() != null) {
			return LocalDateTimeField.MINUTE;
		}

		if (ctx.SECOND() != null) {
			return LocalDateTimeField.SECOND;
		}

		throw new UnsupportedOperationException("Unsupported extract field: " + ctx.getText());
	}

	private TemporalField<?, ? extends Temporal> getTemporalField(HqlParser.ExtractFieldContext ctx) {

		if (ctx.dateOrTimeField() != null) {

			if (ctx.dateOrTimeField().DATE() != null) {
				return LocalDateTimeField.DATE;
			}

			if (ctx.dateOrTimeField().TIME() != null) {
				return LocalDateTimeField.DATE;
			}
		} else if (ctx.datetimeField() != null) {

			if (ctx.datetimeField().YEAR() != null) {
				return LocalDateTimeField.YEAR;
			}

			if (ctx.datetimeField().MONTH() != null) {
				return LocalDateTimeField.MONTH;
			}

			if (ctx.datetimeField().QUARTER() != null) {
				return LocalDateTimeField.QUARTER;
			}

			if (ctx.datetimeField().WEEK() != null) {
				return LocalDateTimeField.WEEK;
			}

			if (ctx.datetimeField().DAY() != null) {
				return LocalDateTimeField.DAY;
			}

			if (ctx.datetimeField().HOUR() != null) {
				return LocalDateTimeField.HOUR;
			}

			if (ctx.datetimeField().MINUTE() != null) {
				return LocalDateTimeField.MINUTE;
			}

			if (ctx.datetimeField().SECOND() != null) {
				return LocalDateTimeField.SECOND;
			}
		} else if (ctx.weekField() != null) {

			if (ctx.weekField().WEEK() != null) {
				return LocalDateTimeField.WEEK;
			}

			if (ctx.weekField().MONTH() != null) {
				return LocalDateTimeField.MONTH;
			}

			if (ctx.weekField().YEAR() != null) {
				return LocalDateTimeField.YEAR;
			}
		}

		throw new UnsupportedOperationException("Unsupported extract field: " + ctx.getText());
	}

	@Override
	public Expression<?> visitTruncFunction(HqlParser.TruncFunctionContext ctx) {

		Expression expr = visitRequired(ctx.expression().get(0));

		if (ctx.datetimeField() != null) {
			TemporalField temporalField = getTemporalField(ctx.datetimeField());

			return cb.function("trunc", Object.class, expr, cb.literal(temporalField));
		} else if (ctx.expression().size() > 1) {

			return cb.function("trunc", Object.class, expr, visitRequired(ctx.expression().get(1)));
		}

		return cb.function("trunc", Object.class, expr);
	}

	@Override
	public Expression<?> visitTrimFunction(HqlParser.TrimFunctionContext ctx) {

		CriteriaBuilder.Trimspec trimSpec = null;

		HqlParser.TrimSpecificationContext tsc = ctx.trimSpecification();

		if (tsc.LEADING() != null) {
			trimSpec = CriteriaBuilder.Trimspec.LEADING;
		} else if (tsc.TRAILING() != null) {
			trimSpec = CriteriaBuilder.Trimspec.TRAILING;
		} else if (tsc.BOTH() != null) {
			trimSpec = CriteriaBuilder.Trimspec.BOTH;
		}

		Expression<Character> stringLiteral = charLiteralOf(ctx.trimCharacter().STRING_LITERAL());
		Expression<String> expression = visitRequired(ctx.expression());

		if (trimSpec != null) {
			return stringLiteral != null //
					? cb.trim(trimSpec, stringLiteral, expression) //
					: cb.trim(trimSpec, expression);
		} else {
			return stringLiteral != null //
					? cb.trim(stringLiteral, expression) //
					: cb.trim(expression);
		}
	}

	@Override
	public Expression<?> visitSubstringFunction(HqlParser.SubstringFunctionContext ctx) {

		Expression<Integer> start = visitRequired(ctx.substringFunctionStartArgument().expression());

		if (ctx.substringFunctionLengthArgument() != null) {
			Expression<Integer> length = visitRequired(ctx.substringFunctionLengthArgument().expression());
			return cb.substring(visitRequired(ctx.expression()), start, length);
		}

		return cb.substring(visitRequired(ctx.expression()), start);
	}

	@Override
	public Expression<?> visitLiteral(HqlParser.LiteralContext ctx) {

		if (ctx.booleanLiteral() != null) {
			return visitRequired(ctx.booleanLiteral());
		} else if (ctx.JAVA_STRING_LITERAL() != null) {
			return literalOf(ctx.JAVA_STRING_LITERAL());
		} else if (ctx.STRING_LITERAL() != null) {
			return literalOf(ctx.STRING_LITERAL());
		} else if (ctx.numericLiteral() != null) {
			return visitRequired(ctx.numericLiteral());
		} else if (ctx.temporalLiteral() != null) {
			return visitRequired(ctx.temporalLiteral());
		} else if (ctx.binaryLiteral() != null) {
			return visitRequired(ctx.binaryLiteral());
		} else {
			return null;
		}
	}

	private Expression<String> literalOf(TerminalNode node) {

		String text = node.getText();
		return cb.literal(unquoteStringLiteral(text));
	}

	private Expression<Character> charLiteralOf(TerminalNode node) {

		String text = node.getText();
		return cb.literal(text.charAt(0));
	}

	@Override
	public Expression<?> visitBooleanLiteral(HqlParser.BooleanLiteralContext ctx) {
		if (ctx.TRUE() != null) {
			return cb.literal(true);
		} else {
			return cb.literal(false);
		}
	}

	@Override
	public Expression<?> visitNumericLiteral(HqlParser.NumericLiteralContext ctx) {
		return cb.literal(getLiteralValue(ctx));
	}

	private Number getLiteralValue(HqlParser.NumericLiteralContext ctx) {

		if (ctx.INTEGER_LITERAL() != null) {
			return Integer.valueOf(getDecimals(ctx.INTEGER_LITERAL()));
		} else if (ctx.LONG_LITERAL() != null) {
			return Long.valueOf(getDecimals(ctx.LONG_LITERAL()));
		} else if (ctx.FLOAT_LITERAL() != null) {
			return Float.valueOf(getDecimals(ctx.FLOAT_LITERAL()));
		} else if (ctx.DOUBLE_LITERAL() != null) {
			return Double.valueOf(getDecimals(ctx.DOUBLE_LITERAL()));
		} else if (ctx.BIG_INTEGER_LITERAL() != null) {
			return new BigInteger(getDecimals(ctx.BIG_INTEGER_LITERAL()));
		} else if (ctx.BIG_DECIMAL_LITERAL() != null) {
			return new BigDecimal(getDecimals(ctx.BIG_DECIMAL_LITERAL()));
		} else if (ctx.HEX_LITERAL() != null) {
			return HexFormat.fromHexDigits(ctx.HEX_LITERAL().toString().substring(2));
		}

		throw new UnsupportedOperationException("Unsupported literal: " + ctx.getText());
	}

	@Override
	public Expression<?> visitDateTimeLiteral(HqlParser.DateTimeLiteralContext ctx) {

		if (ctx.offsetDateTimeLiteral() != null) {
			return visit(ctx.offsetDateTimeLiteral());
		} else if (ctx.localDateTimeLiteral() != null) {
			return visit(ctx.localDateTimeLiteral());
		} else if (ctx.zonedDateTimeLiteral() != null) {
			return visit(ctx.zonedDateTimeLiteral());
		}

		return null;
	}

	@Override
	public Expression<?> visitJdbcTimeLiteral(HqlParser.JdbcTimeLiteralContext ctx) {

		if (ctx.time() != null) {
			return visitRequired(ctx.time());
		}

		return cb.literal(DATE_TIME_FORMATTER_TIME.parse(unquoteTemporal(ctx.genericTemporalLiteralText())));
	}

	@Override
	public Expression<?> visitDate(HqlParser.DateContext ctx) {
		return cb.literal(LocalDate.from(DATE_TIME_FORMATTER_DATE.parse(unquoteTemporal(ctx))));
	}

	@Override
	public Expression<?> visitTime(HqlParser.TimeContext ctx) {
		return cb.literal(LocalTime.from(DATE_TIME_FORMATTER_TIME.parse(unquoteTemporal(ctx))));
	}

	@Override
	public Expression<?> visitJdbcDateLiteral(HqlParser.JdbcDateLiteralContext ctx) {

		if (ctx.date() != null) {
			return visitRequired(ctx.date());
		}

		return cb
				.literal(LocalDate.from(DATE_TIME_FORMATTER_DATE.parse(unquoteTemporal(ctx.genericTemporalLiteralText()))));
	}

	@Override
	public Expression<?> visitJdbcTimestampLiteral(HqlParser.JdbcTimestampLiteralContext ctx) {

		if (ctx.dateTime() != null) {
			return visitRequired(ctx.dateTime());
		}

		return cb.literal(LocalDateTime.from(DATE_TIME.parse(unquoteTemporal(ctx.genericTemporalLiteralText()))));
	}

	@Override
	public Expression<?> visitLocalDateTime(HqlParser.LocalDateTimeContext ctx) {
		return cb.literal(LocalDateTime.from(DATE_TIME.parse(unquoteTemporal(ctx.getText()))));
	}

	@Override
	public Expression<?> visitZonedDateTime(HqlParser.ZonedDateTimeContext ctx) {
		return cb.literal(ZonedDateTime.parse(ctx.getText()));
	}

	@Override
	public Expression<?> visitOffsetDateTime(HqlParser.OffsetDateTimeContext ctx) {
		return cb.literal(OffsetDateTime.parse(ctx.getText()));
	}

	@Override
	public Expression<?> visitOffsetDateTimeWithMinutes(HqlParser.OffsetDateTimeWithMinutesContext ctx) {
		return cb.literal(OffsetDateTime.parse(ctx.getText()));
	}

	@Override
	public Expression<?> visitLocalDateTimeLiteral(HqlParser.LocalDateTimeLiteralContext ctx) {
		return visitRequired(ctx.localDateTime());
	}

	@Override
	public Expression<?> visitZonedDateTimeLiteral(HqlParser.ZonedDateTimeLiteralContext ctx) {
		return visitRequired(ctx.zonedDateTime());
	}

	@Override
	public Expression<?> visitOffsetDateTimeLiteral(HqlParser.OffsetDateTimeLiteralContext ctx) {
		return visitRequired(ctx.offsetDateTime() != null ? ctx.offsetDateTime() : ctx.offsetDateTimeWithMinutes());
	}

	@Override
	public Expression<?> visitDateLiteral(HqlParser.DateLiteralContext ctx) {
		return visitRequired(ctx.date());
	}

	@Override
	public Expression<?> visitTimeLiteral(HqlParser.TimeLiteralContext ctx) {
		return visitRequired(ctx.time());
	}

	@Override
	public Expression<?> visitDateTime(HqlParser.DateTimeContext ctx) {
		return super.visitDateTime(ctx);
	}

	@Override
	public Expression<?> visitGroupedExpression(HqlParser.GroupedExpressionContext ctx) {
		return visit(ctx.expression());
	}

	@Override
	public Expression<?> visitTupleExpression(HqlParser.TupleExpressionContext ctx) {
		return (Expression<?>) cb
				.tuple(ctx.expressionOrPredicate().stream().map(this::visitRequired).toArray(Expression[]::new));
	}

	@Override
	public Expression<?> visitSubqueryExpression(HqlParser.SubqueryExpressionContext ctx) {
		throw new UnsupportedOperationException(String.format(UNSUPPORTED_TEMPLATE, "a subquery argument"));
	}

	@Override
	public Expression<?> visitMultiplicationExpression(HqlParser.MultiplicationExpressionContext ctx) {

		Expression<Number> left = visitRequired(ctx.expression(0));
		Expression<Number> right = visitRequired(ctx.expression(1));

		if (ctx.op.getText().equals("*")) {
			return cb.prod(left, right);
		} else {
			return cb.quot(left, right);
		}
	}

	@Override
	public Expression<?> visitAdditionExpression(HqlParser.AdditionExpressionContext ctx) {

		Expression<Number> left = visitRequired(ctx.expression(0));
		Expression<Number> right = visitRequired(ctx.expression(1));

		if (ctx.op.getText().equals("+")) {
			return cb.sum(left, right);
		} else {
			return cb.diff(left, right);
		}
	}

	@Override
	public Expression<?> visitHqlConcatenationExpression(HqlParser.HqlConcatenationExpressionContext ctx) {

		Expression<String> left = visitRequired(ctx.expression(0));
		Expression<String> right = visitRequired(ctx.expression(1));

		return cb.concat(left, right);
	}

	@Override
	public Expression<?> visitSimplePath(HqlParser.SimplePathContext ctx) {
		return expressionFactory.apply((From<?, ?>) from, PropertyPath.from(ctx.getText(), from.getJavaType()));
	}

	@Override
	public Expression<?> visitCaseList(HqlParser.CaseListContext ctx) {
		return visit(ctx.simpleCaseExpression() != null ? ctx.simpleCaseExpression() : ctx.searchedCaseExpression());
	}

	@Override
	public Expression<?> visitSimpleCaseExpression(HqlParser.SimpleCaseExpressionContext ctx) {

		CriteriaBuilder.SimpleCase<Object, Object> simpleCase = cb.selectCase(visit(ctx.expressionOrPredicate(0)));

		ctx.caseWhenExpressionClause().forEach(caseWhenExpressionClauseContext -> {
			simpleCase.when( //
					visitRequired(caseWhenExpressionClauseContext.expression()), //
					visitRequired(caseWhenExpressionClauseContext.expressionOrPredicate()));
		});

		if (ctx.expressionOrPredicate().size() == 2) {
			simpleCase.otherwise(visitRequired(ctx.expressionOrPredicate(1)));
		}

		return simpleCase;
	}

	@Override
	public Expression<?> visitSearchedCaseExpression(HqlParser.SearchedCaseExpressionContext ctx) {

		CriteriaBuilder.Case<Object> searchedCase = cb.selectCase();

		ctx.caseWhenPredicateClause().forEach(caseWhenPredicateClauseContext -> {
			searchedCase.when( //
					visitRequired(caseWhenPredicateClauseContext.predicate()), //
					visit(caseWhenPredicateClauseContext.expressionOrPredicate()));
		});

		if (ctx.expressionOrPredicate() != null) {
			searchedCase.otherwise(visit(ctx.expressionOrPredicate()));
		}

		return searchedCase;
	}

	@Override
	public Expression<?> visitParameter(HqlParser.ParameterContext ctx) {
		throw new UnsupportedOperationException(String.format(UNSUPPORTED_TEMPLATE, "a parameter argument"));
	}

	private <T> Expression<T> visitRequired(ParseTree ctx) {

		Expression<?> expression = visit(ctx);

		if (expression == null) {
			throw new UnsupportedOperationException("No result for expression: " + ctx.getText());
		}

		return (Expression<T>) expression;
	}

	private String getString(HqlParser.IdentifierContext context) {

		HqlParser.NakedIdentifierContext ni = context.nakedIdentifier();

		String text = context.getText();
		if (ni != null) {
			if (ni.QUOTED_IDENTIFIER() != null) {
				text = unquoteIdentifier(ni.getText());
			}
		}
		return text;
	}

	private static String getDecimals(TerminalNode input) {

		String text = input.getText();
		StringBuilder result = new StringBuilder(text.length());

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isDigit(c) || c == '-' || c == '+' || c == '.') {
				result.append(c);
			}
		}

		return result.toString();
	}

	private static String unquoteTemporal(ParseTree node) {
		return unquoteTemporal(node.getText());
	}

	private static String unquoteTemporal(String temporal) {
		if (temporal.startsWith("'") && temporal.endsWith("'")) {
			temporal = temporal.substring(1, temporal.length() - 1);
		}
		return temporal;
	}

	private static String unquoteIdentifier(String text) {

		int end = text.length() - 1;

		Assert.isTrue(text.charAt(0) == '`' && text.charAt(end) == '`',
				"Quoted identifier does not end with the same delimiter");

		// Unquote a parsed quoted identifier and handle escape sequences
		StringBuilder sb = new StringBuilder(text.length() - 2);
		for (int i = 1; i < end; i++) {

			char c = text.charAt(i);
			if (c == '\\') {
				if (i + 1 < end) {
					char nextChar = text.charAt(++i);
					switch (nextChar) {
						case 'b':
							c = '\b';
							break;
						case 't':
							c = '\t';
							break;
						case 'n':
							c = '\n';
							break;
						case 'f':
							c = '\f';
							break;
						case 'r':
							c = '\r';
							break;
						case '\\':
							c = '\\';
							break;
						case '\'':
							c = '\'';
							break;
						case '"':
							c = '"';
							break;
						case '`':
							c = '`';
							break;
						case 'u':
							c = (char) Integer.parseInt(text.substring(i + 1, i + 5), 16);
							i += 4;
							break;
						default:
							sb.append('\\');
							c = nextChar;
							break;
					}
				}
			}
			sb.append(c);
		}
		return sb.toString();
	}

	private static String unquoteStringLiteral(String text) {

		int end = text.length() - 1;
		char delimiter = text.charAt(0);
		Assert.isTrue(delimiter == text.charAt(end), "Quoted identifier does not end with the same delimiter");

		// Unescape the parsed literal
		StringBuilder sb = new StringBuilder(text.length() - 2);
		for (int i = 1; i < end; i++) {
			char c = text.charAt(i);
			switch (c) {
				case '\'':
					if (delimiter == '\'') {
						i++;
					}
					break;
				case '"':
					if (delimiter == '"') {
						i++;
					}
					break;
				default:
					break;
			}
			sb.append(c);
		}
		return sb.toString();
	}

}
