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

import java.util.function.Supplier;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * A value type used to represent a JPA query token. NOTE: Sometimes the token's value is based upon a value found later
 * in the parsing process, so the text itself is wrapped in a {@link Supplier}.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @since 3.1
 */
class JpaQueryParsingToken implements QueryToken {

	/**
	 * Commonly use tokens.
	 */
	static final JpaQueryParsingToken TOKEN_NONE = JpaQueryParsingToken.token("");
	static final JpaQueryParsingToken TOKEN_COMMA = JpaQueryParsingToken.token(", ");
	static final JpaQueryParsingToken TOKEN_SPACE = JpaQueryParsingToken.token(" ");
	static final JpaQueryParsingToken TOKEN_DOT = JpaQueryParsingToken.token(".");
	static final JpaQueryParsingToken TOKEN_EQUALS = JpaQueryParsingToken.token(" = ");
	static final JpaQueryParsingToken TOKEN_OPEN_PAREN = JpaQueryParsingToken.token("(");
	static final JpaQueryParsingToken TOKEN_CLOSE_PAREN = JpaQueryParsingToken.token(")");
	static final JpaQueryParsingToken TOKEN_ORDER_BY = JpaQueryParsingToken.expression("order by");
	static final JpaQueryParsingToken TOKEN_LOWER_FUNC = JpaQueryParsingToken.token("lower(");
	static final JpaQueryParsingToken TOKEN_SELECT_COUNT = JpaQueryParsingToken.token("select count(");
	static final JpaQueryParsingToken TOKEN_COUNT_FUNC = JpaQueryParsingToken.token("count(");
	static final JpaQueryParsingToken TOKEN_DOUBLE_PIPE = JpaQueryParsingToken.token(" || ");
	static final JpaQueryParsingToken TOKEN_OPEN_SQUARE_BRACKET = JpaQueryParsingToken.token("[");
	static final JpaQueryParsingToken TOKEN_CLOSE_SQUARE_BRACKET = JpaQueryParsingToken.token("]");
	static final JpaQueryParsingToken TOKEN_COLON = JpaQueryParsingToken.token(":");
	static final JpaQueryParsingToken TOKEN_QUESTION_MARK = JpaQueryParsingToken.token("?");
	static final JpaQueryParsingToken TOKEN_OPEN_BRACE = JpaQueryParsingToken.token("{");
	static final JpaQueryParsingToken TOKEN_CLOSE_BRACE = JpaQueryParsingToken.token("}");
	static final JpaQueryParsingToken TOKEN_DOUBLE_UNDERSCORE = JpaQueryParsingToken.token("__");
	static final JpaQueryParsingToken TOKEN_AS = JpaQueryParsingToken.expression("AS");
	static final JpaQueryParsingToken TOKEN_DESC = JpaQueryParsingToken.expression("desc");
	static final JpaQueryParsingToken TOKEN_ASC = JpaQueryParsingToken.expression("asc");
	static final JpaQueryParsingToken TOKEN_WITH = JpaQueryParsingToken.expression("WITH");
	static final JpaQueryParsingToken TOKEN_NOT = JpaQueryParsingToken.expression("NOT");
	static final JpaQueryParsingToken TOKEN_MATERIALIZED = JpaQueryParsingToken.expression("materialized");
	static final JpaQueryParsingToken TOKEN_NULLS = JpaQueryParsingToken.expression("NULLS");
	static final JpaQueryParsingToken TOKEN_FIRST = JpaQueryParsingToken.expression("FIRST");
	static final JpaQueryParsingToken TOKEN_LAST = JpaQueryParsingToken.expression("LAST");

	/**
	 * The text value of the token.
	 */
	private final String token;

	JpaQueryParsingToken(String token) {
		this.token = token;
	}

	static JpaQueryParsingToken token(TerminalNode node) {
		return token(node.getText());
	}

	static JpaQueryParsingToken token(Token token) {
		return token(token.getText());
	}

	static JpaQueryParsingToken token(String token) {
		return new JpaQueryParsingToken(token);
	}

	static JpaQueryParsingToken expression(String expression) {
		return new JpaExpressionToken(expression);
	}

	static JpaQueryParsingToken expression(Token token) {
		return expression(token.getText());
	}

	static JpaQueryParsingToken expression(TerminalNode node) {
		return expression(node.getText());
	}

	static JpaQueryParsingToken ventilated(Token op) {
		return new JpaQueryParsingToken(" " + op.getText() + " ");
	}

	String getToken() {
		return value();
	}

	public String value() {
		return token;
	}

	/**
	 * Compare whether the given {@link JpaQueryParsingToken token} is equal to the one held by this instance.
	 *
	 * @param token must not be {@literal null}.
	 * @return {@literal true} if both tokens are equals (using case-insensitive comparison).
	 */
	public boolean isA(QueryToken token) {
		return token.value().equalsIgnoreCase(this.value());
	}

	@Override
	public String toString() {
		return getToken();
	}

	static class JpaExpressionToken extends JpaQueryParsingToken {

		JpaExpressionToken(String token) {
			super(token);
		}

		public boolean isExpression() {
			return true;
		}
	}
}
