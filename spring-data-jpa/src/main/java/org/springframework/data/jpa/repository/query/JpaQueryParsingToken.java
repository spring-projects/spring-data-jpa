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

import java.util.Collection;
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
	public static final JpaQueryParsingToken TOKEN_NONE = JpaQueryParsingToken.token("");
	public static final JpaQueryParsingToken TOKEN_COMMA = JpaQueryParsingToken.token(", ");
	public static final JpaQueryParsingToken TOKEN_SPACE = JpaQueryParsingToken.token(" ");
	public static final JpaQueryParsingToken TOKEN_DOT = JpaQueryParsingToken.token(".");
	public static final JpaQueryParsingToken TOKEN_EQUALS = JpaQueryParsingToken.token(" = ");
	public static final JpaQueryParsingToken TOKEN_OPEN_PAREN = JpaQueryParsingToken.token("(");
	public static final JpaQueryParsingToken TOKEN_CLOSE_PAREN = JpaQueryParsingToken.token(")");
	public static final JpaQueryParsingToken TOKEN_ORDER_BY = JpaQueryParsingToken.expression("order by");
	public static final JpaQueryParsingToken TOKEN_LOWER_FUNC = new JpaQueryParsingToken("lower(");
	public static final JpaQueryParsingToken TOKEN_SELECT_COUNT = JpaQueryParsingToken.token("select count(");
	public static final JpaQueryParsingToken TOKEN_COUNT_FUNC = JpaQueryParsingToken.token("count(");
	public static final JpaQueryParsingToken TOKEN_DOUBLE_PIPE = JpaQueryParsingToken.token(" || ");
	public static final JpaQueryParsingToken TOKEN_OPEN_SQUARE_BRACKET = JpaQueryParsingToken.token("[");
	public static final JpaQueryParsingToken TOKEN_CLOSE_SQUARE_BRACKET = new JpaQueryParsingToken("]");
	public static final JpaQueryParsingToken TOKEN_COLON = JpaQueryParsingToken.token(":");
	public static final JpaQueryParsingToken TOKEN_QUESTION_MARK = JpaQueryParsingToken.token("?");
	public static final JpaQueryParsingToken TOKEN_OPEN_BRACE = JpaQueryParsingToken.token("{");
	public static final JpaQueryParsingToken TOKEN_CLOSE_BRACE = new JpaQueryParsingToken("}");
	public static final JpaQueryParsingToken TOKEN_DOUBLE_UNDERSCORE = JpaQueryParsingToken.token("__");
	public static final JpaQueryParsingToken TOKEN_AS = JpaQueryParsingToken.expression("AS");
	public static final JpaQueryParsingToken TOKEN_DESC = JpaQueryParsingToken.expression("desc");
	public static final JpaQueryParsingToken TOKEN_ASC = JpaQueryParsingToken.expression("asc");
	public static final JpaQueryParsingToken TOKEN_WITH = JpaQueryParsingToken.expression("WITH");
	public static final JpaQueryParsingToken TOKEN_NOT = JpaQueryParsingToken.expression("NOT");
	public static final JpaQueryParsingToken TOKEN_MATERIALIZED = JpaQueryParsingToken.expression("materialized");
	public static final JpaQueryParsingToken TOKEN_NULLS = JpaQueryParsingToken.expression("NULLS");
	public static final JpaQueryParsingToken TOKEN_FIRST = JpaQueryParsingToken.expression("FIRST");
	public static final JpaQueryParsingToken TOKEN_LAST = JpaQueryParsingToken.expression("LAST");

	/**
	 * The text value of the token.
	 */
	private final String token;

	JpaQueryParsingToken(String token) {
		this.token = token;
	}

	public static JpaQueryParsingToken token(TerminalNode node) {
		return token(node.getText());
	}

	public static JpaQueryParsingToken token(Token token) {
		return token(token.getText());
	}

	static JpaQueryParsingToken token(String token) {
		return new JpaQueryParsingToken(token);
	}

	static JpaQueryParsingToken expression(String expression) {
		return new JpaQueryExpression(expression);
	}

	public static JpaQueryParsingToken expression(Token token) {
		return expression(token.getText());
	}

	public static JpaQueryParsingToken expression(TerminalNode node) {
		return expression(node.getText());
	}

	public static JpaQueryParsingToken ventilated(Token op) {
		return new JpaQueryParsingToken(" " + op.getText() + " ");
	}

	String getToken() {
		return token;
	}

	/**
	 * Compare whether the given {@link JpaQueryParsingToken token} is equal to the one held by this instance.
	 *
	 * @param token must not be {@literal null}.
	 * @return {@literal true} if both tokens are equals (using case-insensitive comparison).
	 */
	boolean isA(JpaQueryParsingToken token) {
		return token.getToken().equalsIgnoreCase(this.getToken());
	}

	@Override
	public String toString() {
		return getToken();
	}

	/**
	 * Render a list of {@link JpaQueryParsingToken}s into a string.
	 *
	 * @param tokens
	 * @return rendered string containing either a query or some subset of that query
	 */
	static String render(Object tokens) {

		if (tokens instanceof Collection tpr) {
			return render(tpr);
		}

		return ((QueryRenderer.QueryRendererBuilder) tokens).build().render();
	}

	/**
	 * Render a list of {@link JpaQueryParsingToken}s into a string.
	 *
	 * @param tokens
	 * @return rendered string containing either a query or some subset of that query
	 */
	static String render(Collection<JpaQueryParsingToken> tokens) {

		StringBuilder results = new StringBuilder();

		boolean previousExpression = false;

		for (JpaQueryParsingToken jpaQueryParsingToken : tokens) {

			if (previousExpression) {
				if (!results.isEmpty() && results.charAt(results.length() - 1) != ' ') {
					results.append(' ');
				}
			}

			previousExpression = jpaQueryParsingToken instanceof JpaQueryExpression;
			results.append(jpaQueryParsingToken.getToken());
		}

		return results.toString();
	}

	static class JpaQueryExpression extends JpaQueryParsingToken {

		JpaQueryExpression(String token) {
			super(token);
		}
	}
}
