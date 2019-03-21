/*
 * Copyright 2018 the original author or authors.
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

import static java.util.Arrays.*;

import lombok.Value;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A parser that parses SQL, JQL and HQL queries in order to extract information needed in order to manipulate the
 * queries.
 *
 * @author Jens Schauder
 * @since 2.2.0
 */
@UtilityClass
class QlParser {

	private static final Set<String> KEYWORDS = new HashSet<>(asList("order", "where", "join", "from", "case"));

	// Used Regex/Unicode categories (see http://www.unicode.org/reports/tr18/#General_Category_Property):
	// Z Separator
	// Cc Control
	// Cf Format
	// P Punctuation
	private static final Pattern IDENTIFIER = Pattern.compile("[._[\\P{Z}&&\\P{Cc}&&\\P{Cf}&&\\P{P}]]+");

	/**
	 * Parses any String enclosed in single quotes. Result does contain the quotes.
	 */
	static final Parser<String, String> quotedLiteral = StringParsers.regex("'.*?'");

	/**
	 * Parses white space to {@code null}.
	 */
	private static final Parser<String, String> ws = StringParsers.regex("\\s+").map(w -> null);

	/**
	 * Parses an inline comment and maps it to {@code null}.
	 */
	private static final Parser<String, String> inlineComment = StringParsers.regex("--.*").map(c -> null);

	/**
	 * Parses anything enclosed with double quotes.
	 */
	static final Parser<String, String> doubleQuotedIdentifier = StringParsers.regex("\".*?\"");

	/**
	 * Parses a block comment enclosed by /* and * / (without the space).
	 */
	static final Parser<String, String> blockComment = StringParsers.regex("/\\*.*?\\*/").map(c -> null);

	private static final Parser<String, String> openParen = StringParsers.keyword("(");
	private static final Parser<String, String> closingParen = StringParsers.keyword(")");
	private static final Parser<String, String> comma = StringParsers.keyword(",");

	/**
	 * This is intended to parse any part of a SQL string that isn't recognised by other parsers.
	 */
	private static final Parser<String, String> word = StringParsers.regex("[%&*+\\-\\./\\\\:;<?\\[\\]\\^_|{}\\w]+");

	/**
	 * Parses all tokens not enclosed in parenthesis.
	 */
	private static final Parser<String, String> noParensToken = Parsers.either(ws, blockComment, inlineComment,
			quotedLiteral, doubleQuotedIdentifier, comma, word);

	/**
	 * Parses all tokens including those included in parenthesis.
	 */
	private static final Parser<String, Object> token = Parsers.either(noParensToken, balancedParens());

	private static final Parser<List<Object>, TableAlias> tableAlias = Parsers.concat( //
			() -> keyword("from"), //
			QlParser::any, // table expression
			() -> Parsers.opt(keyword("as")), //
			QlParser::identifier //
	).map(l -> new TableAlias((String) l.get(3)));

	private static final Parser<List<Object>, JoinAlias> joinAlias = Parsers.concat( //
			() -> keyword("join"), //
			() -> Parsers.opt(keyword("fetch")), //
			QlParser::identifier, //
			() -> Parsers.opt(keyword("as")), //
			QlParser::identifier).map(l -> new JoinAlias((String) l.get(4)));

	private static final Parser<List<Object>, FunctionAlias> functionAlias = Parsers.concat( //
			QlParser::identifier, //
			QlParser::parameterList, //
			() -> Parsers.opt(keyword("as")), //
			QlParser::identifier //
	).map(l -> new FunctionAlias((String) l.get(3)));

	private static final Parser<List<Object>, OrderBy> orderBy = Parsers.concat( //
			() -> keyword("order"), //
			() -> keyword("by")).map(l -> OrderBy.INSTANCE);

	private Parser<List<Object>, SqlInfo> sqlInfoParser = Parsers.many(Parsers.either( //
			joinAlias, //
			functionAlias, //
			tableAlias, //
			orderBy, //
			any()) // ignore all other elements
	).map(QlParser::sqlElementsToSqlInfo);

	/**
	 * Converts a list of {@link JoinAlias}, {@link TableAlias}, {@link OrderBy}, and {@link FunctionAlias} into a single
	 * SqlInfo instance;
	 *
	 * @param list list of objects to parse. Must not be {@code null}.
	 * @return a {@link SqlInfo} instance with the condensed information from the list. Guaranteed to be not {@code null}.
	 */
	private static SqlInfo sqlElementsToSqlInfo(List<Object> list) {

		SqlInfo sqlInfo = new SqlInfo();

		list.forEach(e -> {

			if (e instanceof JoinAlias) {
				sqlInfo.joinAliases.add(((JoinAlias) e).alias);
			} else if (e instanceof TableAlias && sqlInfo.tableAlias == null) {
				sqlInfo.tableAlias = ((TableAlias) e).alias;
			} else if (e instanceof FunctionAlias) {
				sqlInfo.functionAliases.add(((FunctionAlias) e).alias);
			} else if (e instanceof OrderBy) {
				sqlInfo.orderBy = true;
			}

		});

		return sqlInfo;
	}

	private static Parser<List<Object>, List> parameterList() {

		return ListParsers.element( //
				List.class, //
				l -> l.size() >= 2 //
						&& l.get(0).equals("(") //
						&& l.get(l.size() - 1).equals(")") //
		);
	}

	/**
	 * Parses a SQL, JQL or HQL query and returns the essential information about the query.
	 *
	 * @param query the query to parse. Must not be {@code null}.
	 * @return a {@link SqlInfo} instance. Guaranteed to be not {@code null}.
	 */
	static SqlInfo parseSql(String query) {

		Parser.ParseResult<String, List<Object>> parseResult = tokenizer.parse(query);

		if (parseResult instanceof Parser.Success) {

			Parser.ParseResult<List<Object>, SqlInfo> sqlInfoParseResult = sqlInfoParser
					.parse(((Parser.Success<String, List<Object>>) parseResult).getResult());

			if (sqlInfoParseResult instanceof Parser.Success) {
				return ((Parser.Success<List<Object>, SqlInfo>) sqlInfoParseResult).getResult();
			}
		}
		return new SqlInfo();
	}

	@Value
	private static class FunctionAlias {
		String alias;
	}

	private static Parser<List<Object>, String> identifier() {

		return ListParsers.element(String.class,
				s -> !KEYWORDS.contains(s.toLowerCase()) && IDENTIFIER.matcher(s).matches());
	}

	private static Parser<List<Object>, Object> any() {
		return ListParsers.element(Object.class, s -> true);
	}

	private static Parser<List<Object>, String> keyword(String keyword) {
		return ListParsers.element(String.class, s -> s.equalsIgnoreCase(keyword));
	}

	static final Parser<String, List<Object>> tokenizer = Parsers.many(token)
			.map(l -> l.stream().filter(Objects::nonNull).collect(Collectors.toList()));

	private static Parser<String, List<Object>> balancedParens() {
		return Parsers.concat(() -> openParen, () -> tokenizer, () -> closingParen);
	}

	enum OrderBy {
		INSTANCE
	}

	static class SqlInfo {
		final Set<String> joinAliases = new HashSet<>();
		final Set<String> functionAliases = new HashSet<>();
		String tableAlias;
		boolean orderBy;
	}

	@Value
	private static class TableAlias {
		String alias;
	}

	@Value
	private static class JoinAlias {
		String alias;
	}
}
