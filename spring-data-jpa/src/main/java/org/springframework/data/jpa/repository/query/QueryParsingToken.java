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

/**
 * A value type used to represent a JPA query token. NOTE: Sometimes the token's value is based upon a value found later
 * in the parsing process, so we text itself is wrapped in a {@link Supplier}.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class QueryParsingToken {

	/**
	 * The text value of the token.
	 */
	private Supplier<String> token;

	/**
	 * Space|NoSpace after token is rendered?
	 */
	private boolean space;

	QueryParsingToken(Supplier<String> token, boolean space) {

		this.token = token;
		this.space = space;
	}

	QueryParsingToken(Supplier<String> token) {
		this(token, true);
	}

	QueryParsingToken(String token, boolean space) {
		this(() -> token, space);
	}

	QueryParsingToken(String token) {
		this(() -> token, true);
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

	void setSpace(boolean space) {
		this.space = space;
	}

	/**
	 * Switch the last {@link QueryParsingToken}'s spacing to {@literal false}.
	 */
	static void NOSPACE(List<QueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {
			tokens.get(tokens.size() - 1).setSpace(false);
		}
	}

	/**
	 * Switch the last {@link QueryParsingToken}'s spacing to {@literal true}.
	 */
	static void SPACE(List<QueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {
			tokens.get(tokens.size() - 1).setSpace(true);
		}
	}

	/**
	 * Drop the very last entry from the list of {@link QueryParsingToken}s.
	 */
	static void CLIP(List<QueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {
			tokens.remove(tokens.size() - 1);
		}
	}

	/**
	 * Render a list of {@link QueryParsingToken}s into a query string.
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
