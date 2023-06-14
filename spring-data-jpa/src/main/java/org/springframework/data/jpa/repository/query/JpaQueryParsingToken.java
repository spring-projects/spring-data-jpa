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

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * A value type used to represent a JPA query token. NOTE: Sometimes the token's value is based upon a value found later
 * in the parsing process, so the text itself is wrapped in a {@link Supplier}.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class JpaQueryParsingToken {

	/**
	 * Commonly use tokens.
	 */
	public static final JpaQueryParsingToken TOKEN_COMMA = new JpaQueryParsingToken(",");
	public static final JpaQueryParsingToken TOKEN_DOT = new JpaQueryParsingToken(".", false);
	public static final JpaQueryParsingToken TOKEN_EQUALS = new JpaQueryParsingToken("=");
	public static final JpaQueryParsingToken TOKEN_OPEN_PAREN = new JpaQueryParsingToken("(", false);
	public static final JpaQueryParsingToken TOKEN_CLOSE_PAREN = new JpaQueryParsingToken(")");
	public static final JpaQueryParsingToken TOKEN_ORDER_BY = new JpaQueryParsingToken("order by");
	public static final JpaQueryParsingToken TOKEN_LOWER_FUNC = new JpaQueryParsingToken("lower(", false);
	public static final JpaQueryParsingToken TOKEN_SELECT_COUNT = new JpaQueryParsingToken("select count(", false);
	public static final JpaQueryParsingToken TOKEN_PERCENT = new JpaQueryParsingToken("%");
	public static final JpaQueryParsingToken TOKEN_COUNT_FUNC = new JpaQueryParsingToken("count(", false);
	public static final JpaQueryParsingToken TOKEN_DOUBLE_PIPE = new JpaQueryParsingToken("||");
	public static final JpaQueryParsingToken TOKEN_OPEN_SQUARE_BRACKET = new JpaQueryParsingToken("[", false);
	public static final JpaQueryParsingToken TOKEN_CLOSE_SQUARE_BRACKET = new JpaQueryParsingToken("]");
	public static final JpaQueryParsingToken TOKEN_COLON = new JpaQueryParsingToken(":", false);
	public static final JpaQueryParsingToken TOKEN_QUESTION_MARK = new JpaQueryParsingToken("?", false);
	public static final JpaQueryParsingToken TOKEN_OPEN_BRACE = new JpaQueryParsingToken("{", false);
	public static final JpaQueryParsingToken TOKEN_CLOSE_BRACE = new JpaQueryParsingToken("}");
	public static final JpaQueryParsingToken TOKEN_CLOSE_SQUARE_BRACKET_BRACE = new JpaQueryParsingToken("]}");
	public static final JpaQueryParsingToken TOKEN_CLOSE_PAREN_BRACE = new JpaQueryParsingToken(")}");

	public static final JpaQueryParsingToken TOKEN_DOUBLE_UNDERSCORE = new JpaQueryParsingToken("__");

	public static final JpaQueryParsingToken TOKEN_AS = new JpaQueryParsingToken("AS");

	public static final JpaQueryParsingToken TOKEN_DESC = new JpaQueryParsingToken("desc", false);

	public static final JpaQueryParsingToken TOKEN_ASC = new JpaQueryParsingToken("asc", false);

	public static final JpaQueryParsingToken TOKEN_WITH = new JpaQueryParsingToken("WITH");

	public static final JpaQueryParsingToken TOKEN_NOT = new JpaQueryParsingToken("NOT");

	public static final JpaQueryParsingToken TOKEN_MATERIALIZED = new JpaQueryParsingToken("materialized");

	/**
	 * The text value of the token.
	 */
	private final Supplier<String> token;

	/**
	 * Space|NoSpace after token is rendered?
	 */
	private final boolean space;

	JpaQueryParsingToken(Supplier<String> token, boolean space) {

		this.token = token;
		this.space = space;
	}

	JpaQueryParsingToken(String token, boolean space) {
		this(() -> token, space);
	}

	JpaQueryParsingToken(Supplier<String> token) {
		this(token, true);
	}

	JpaQueryParsingToken(String token) {
		this(() -> token, true);
	}

	JpaQueryParsingToken(TerminalNode node, boolean space) {
		this(node.getText(), space);
	}

	JpaQueryParsingToken(TerminalNode node) {
		this(node.getText());
	}

	JpaQueryParsingToken(Token token, boolean space) {
		this(token.getText(), space);
	}

	JpaQueryParsingToken(Token token) {
		this(token.getText(), true);
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
	 * Switch the last {@link JpaQueryParsingToken}'s spacing to {@literal true}.
	 */
	static void SPACE(List<JpaQueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {

			int index = tokens.size() - 1;

			JpaQueryParsingToken lastTokenWithSpacing = new JpaQueryParsingToken(tokens.get(index).token);
			tokens.remove(index);
			tokens.add(lastTokenWithSpacing);
		}
	}

	/**
	 * Switch the last {@link JpaQueryParsingToken}'s spacing to {@literal false}.
	 */
	static void NOSPACE(List<JpaQueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {

			int index = tokens.size() - 1;

			JpaQueryParsingToken lastTokenWithNoSpacing = new JpaQueryParsingToken(tokens.get(index).token, false);
			tokens.remove(index);
			tokens.add(lastTokenWithNoSpacing);
		}
	}

	/**
	 * Drop the last entry from the list of {@link JpaQueryParsingToken}s.
	 */
	static void CLIP(List<JpaQueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {
			tokens.remove(tokens.size() - 1);
		}
	}

	/**
	 * Render a list of {@link JpaQueryParsingToken}s into a string.
	 *
	 * @param tokens
	 * @return rendered string containing either a query or some subset of that query
	 */
	static String render(List<JpaQueryParsingToken> tokens) {

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
