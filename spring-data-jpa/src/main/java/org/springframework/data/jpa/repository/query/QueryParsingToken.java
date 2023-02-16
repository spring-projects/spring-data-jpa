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

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * A value type used to represent a JPQL token.
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
	 * The surrounding contextual information of the parsing rule the token came from.
	 */
	private ParserRuleContext context;

	/**
	 * Space|NoSpace after token is rendered?
	 */
	private boolean space = true;

	/**
	 * Indicates if a line break should be rendered before the token itself is rendered (DEBUG only)
	 */
	private boolean lineBreak = false;

	/**
	 * Is this token for debug purposes only?
	 */
	private boolean debugOnly = false;

	public QueryParsingToken(Supplier<String> token, ParserRuleContext context) {

		this.token = token;
		this.context = context;
	}

	public QueryParsingToken(Supplier<String> token, ParserRuleContext context, boolean space) {

		this(token, context);
		this.space = space;
	}

	public QueryParsingToken(String token, ParserRuleContext ctx) {
		this(() -> token, ctx);
	}

	public QueryParsingToken(String token, ParserRuleContext ctx, boolean space) {
		this(() -> token, ctx, space);
	}

	public String getToken() {
		return this.token.get();
	}

	public ParserRuleContext getContext() {
		return context;
	}

	public boolean getSpace() {
		return this.space;
	}

	public void setSpace(boolean space) {
		this.space = space;
	}

	public boolean isLineBreak() {
		return lineBreak;
	}

	public boolean isDebugOnly() {
		return debugOnly;
	}

	@Override
	public String toString() {
		return "QueryParsingToken{" + "token='" + token + '\'' + ", context=" + context + ", space=" + space + ", lineBreak="
				+ lineBreak + ", debugOnly=" + debugOnly + '}';
	}

	/**
	 * Switch the last {@link QueryParsingToken}'s spacing to {@literal false}.
	 */
	static List<QueryParsingToken> NOSPACE(List<QueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {
			tokens.get(tokens.size() - 1).setSpace(false);
		}
		return tokens;
	}

	/**
	 * Switch the last {@link QueryParsingToken}'s spacing to {@literal true}.
	 */
	static List<QueryParsingToken> SPACE(List<QueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {
			tokens.get(tokens.size() - 1).setSpace(true);
		}

		return tokens;
	}

	/**
	 * Drop the very last entry from the list of {@link QueryParsingToken}s.
	 */
	static List<QueryParsingToken> CLIP(List<QueryParsingToken> tokens) {

		if (!tokens.isEmpty()) {
			tokens.remove(tokens.size() - 1);
		}
		return tokens;
	}

}
