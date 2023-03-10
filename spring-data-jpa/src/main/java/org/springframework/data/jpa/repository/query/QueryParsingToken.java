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

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * A value type used to represent a JPA query token. NOTE: Sometimes the token's value is based upon a value found later
 * in the parsing process, so the text itself is wrapped in a {@link Supplier}.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class QueryParsingToken {

	/**
	 * Commonly use tokens.
	 */
	public static final QueryParsingToken TOKEN_COMMA = new QueryParsingToken(",");
	public static final QueryParsingToken TOKEN_DOT = new QueryParsingToken(".", false);
	public static final QueryParsingToken TOKEN_EQUALS = new QueryParsingToken("=");
	public static final QueryParsingToken TOKEN_OPEN_PAREN = new QueryParsingToken("(", false);
	public static final QueryParsingToken TOKEN_CLOSE_PAREN = new QueryParsingToken(")");
	public static final QueryParsingToken TOKEN_ORDER_BY = new QueryParsingToken("order by");
	public static final QueryParsingToken TOKEN_LOWER_FUNC = new QueryParsingToken("lower(", false);
	public static final QueryParsingToken TOKEN_SELECT_COUNT = new QueryParsingToken("select count(", false);
	public static final QueryParsingToken TOKEN_PERCENT = new QueryParsingToken("%");
	public static final QueryParsingToken TOKEN_COUNT_FUNC = new QueryParsingToken("count(", false);
	public static final QueryParsingToken TOKEN_DOUBLE_PIPE = new QueryParsingToken("||");
	public static final QueryParsingToken TOKEN_OPEN_SQUARE_BRACKET = new QueryParsingToken("[", false);
	public static final QueryParsingToken TOKEN_CLOSE_SQUARE_BRACKET = new QueryParsingToken("]");
	public static final QueryParsingToken TOKEN_COLON = new QueryParsingToken(":", false);
	public static final QueryParsingToken TOKEN_QUESTION_MARK = new QueryParsingToken("?", false);
	public static final QueryParsingToken TOKEN_CLOSE_BRACE = new QueryParsingToken("}");
	public static final QueryParsingToken TOKEN_CLOSE_SQUARE_BRACKET_BRACE = new QueryParsingToken("]}");
	public static final QueryParsingToken TOKEN_CLOSE_PAREN_BRACE = new QueryParsingToken(")}");

	public static final QueryParsingToken TOKEN_DESC = new QueryParsingToken("desc", false);

	public static final QueryParsingToken TOKEN_ASC = new QueryParsingToken("asc", false);
	/**
	 * The text value of the token.
	 */
	private final Supplier<String> token;

	/**
	 * Space|NoSpace after token is rendered?
	 */
	private final boolean space;

	QueryParsingToken(Supplier<String> token, boolean space) {

		this.token = token;
		this.space = space;
	}

	QueryParsingToken(String token, boolean space) {
		this(() -> token, space);
	}

	QueryParsingToken(Supplier<String> token) {
		this(token, true);
	}

	QueryParsingToken(String token) {
		this(() -> token, true);
	}

	QueryParsingToken(TerminalNode node, boolean space) {
		this(node.getText(), space);
	}

	QueryParsingToken(TerminalNode node) {
		this(node.getText());
	}

	QueryParsingToken(Token token) {
		this(token.getText());
	}

	/**
	 * Extract the token's value from it's {@link Supplier}.
	 */
	String getToken() {
		return this.token.get();
	}

	/**
	 * Should we render a space after the token?
	 */
	boolean getSpace() {
		return this.space;
	}

	/**
	 * Switch the last {@link QueryParsingToken}'s spacing to {@literal true}.
	 */
	static void SPACE(List<QueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {

			int index = tokens.size() - 1;

			QueryParsingToken lastTokenWithSpacing = new QueryParsingToken(tokens.get(index).token);
			tokens.remove(index);
			tokens.add(lastTokenWithSpacing);
		}
	}

	/**
	 * Switch the last {@link QueryParsingToken}'s spacing to {@literal false}.
	 */
	static void NOSPACE(List<QueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {

			int index = tokens.size() - 1;

			QueryParsingToken lastTokenWithNoSpacing = new QueryParsingToken(tokens.get(index).token, false);
			tokens.remove(index);
			tokens.add(lastTokenWithNoSpacing);
		}
	}

	/**
	 * Take a list of {@link QueryParsingToken}s and convert them ALL to {@code space = false} (except possibly the last
	 * one).
	 * 
	 * @param tokens
	 * @param spacelastElement
	 */
	static List<QueryParsingToken> NOSPACE_ALL_BUT_LAST_ELEMENT(List<QueryParsingToken> tokens,
			boolean spacelastElement) {

		List<QueryParsingToken> respacedTokens = tokens.stream() //
				.map(queryParsingToken -> {

					if (queryParsingToken.space == true) {
						return new QueryParsingToken(queryParsingToken.token, false);
					} else {
						return queryParsingToken;
					}
				}) //
				.collect(Collectors.toList());

		if (spacelastElement) {
			SPACE(respacedTokens);
		}

		return respacedTokens;
	}

	/**
	 * Drop the last entry from the list of {@link QueryParsingToken}s.
	 */
	static void CLIP(List<QueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {
			tokens.remove(tokens.size() - 1);
		}
	}

	/**
	 * Render a list of {@link QueryParsingToken}s into a string.
	 *
	 * @param tokens
	 * @return rendered string containing either a query or some subset of that query
	 */
	static String render(List<QueryParsingToken> tokens) {

		if (tokens == null) {
			return "";
		}

		StringBuilder results = new StringBuilder();

		tokens.forEach(token -> {

			results.append(token.getToken());

			if (token.getSpace()) {
				results.append(" ");
			}
		});

		return results.toString().trim();
	}
}
