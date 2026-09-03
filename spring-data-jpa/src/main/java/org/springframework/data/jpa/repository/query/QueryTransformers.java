/*
 * Copyright 2024-present the original author or authors.
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
import java.util.Iterator;
import java.util.List;

/**
 * Utility class encapsulating common query transformations.
 *
 * @author Mark Paluch
 * @since 3.2.5
 */
class QueryTransformers {

	static class CountSelectionTokenStream implements QueryTokenStream {

		private final List<QueryToken> tokens;
		private final boolean requiresPrimaryAlias;

		CountSelectionTokenStream(List<QueryToken> tokens, boolean requiresPrimaryAlias) {
			this.tokens = tokens;
			this.requiresPrimaryAlias = requiresPrimaryAlias;
		}

		static CountSelectionTokenStream create(QueryTokenStream selection) {

			List<QueryToken> target = new ArrayList<>(selection.size());
			boolean skipNext = false;
			boolean containsNew = false;
			int nestingLevel = 0;

			for (QueryToken token : selection) {

				if (skipNext) {
					skipNext = false;
					continue;
				}

				if (token.equals(TOKEN_OPEN_PAREN)) {
					nestingLevel++;
				} else if (token.equals(TOKEN_CLOSE_PAREN)) {
					nestingLevel--;
				}

				if (nestingLevel == 0 && token.equals(TOKEN_AS)) {
					skipNext = true;
					continue;
				}

				if (!containsNew && token.equals(TOKEN_NEW)) {
					containsNew = true;
				}

				target.add(token);
			}

			normalizeSpacing(target);

			return new CountSelectionTokenStream(target, containsNew);
		}

		/**
		 * Flattening a composed {@link QueryTokenStream} into a plain token list loses the spacing information that is
		 * otherwise contributed by the renderer composition. Reconstruct whitespace by rewriting each token into an
		 * expression (rendered with a trailing space) unless it is followed by a token that must not be preceded by a
		 * space (comma, closing or opening parenthesis).
		 *
		 * @param tokens the token list to normalize.
		 */
		private static void normalizeSpacing(List<QueryToken> tokens) {

			for (int i = 0; i < tokens.size(); i++) {

				QueryToken token = tokens.get(i);
				QueryToken next = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

				boolean spaceAfter = next != null && !token.equals(TOKEN_OPEN_PAREN) && !token.equals(TOKEN_COMMA)
						&& !token.equals(TOKEN_DOT) && !next.equals(TOKEN_CLOSE_PAREN) && !next.equals(TOKEN_OPEN_PAREN)
						&& !next.equals(TOKEN_COMMA) && !next.equals(TOKEN_DOT);

				if (spaceAfter != token.isExpression()) {
					tokens.set(i, spaceAfter ? QueryTokens.expression(token.value()) : QueryTokens.token(token.value()));
				}
			}
		}

		/**
		 * Filter constructor expression and return the selection list of the constructor.
		 *
		 * @return the selection list of the constructor without {@code NEW}, class name, and the first level of
		 *         parentheses.
		 * @since 3.5.2
		 */
		public CountSelectionTokenStream withoutConstructorExpression() {

			if (!requiresPrimaryAlias()) {
				return this;
			}

			List<QueryToken> target = new ArrayList<>(size());
			int nestingLevel = 0;

			for (QueryToken token : this) {

				if (token.equals(TOKEN_OPEN_PAREN)) {

					// skip the constructor parenthesis only, retain nested parentheses (e.g. function calls)
					if (++nestingLevel == 1) {
						continue;
					}
				} else if (token.equals(TOKEN_CLOSE_PAREN)) {

					if (--nestingLevel == 0) {
						continue;
					}
				}

				if (nestingLevel > 0) {
					target.add(token);
				}
			}

			return new CountSelectionTokenStream(target, requiresPrimaryAlias());
		}

		@Override
		public Iterator<QueryToken> iterator() {
			return tokens.iterator();
		}

		@Override
		public List<QueryToken> toList() {
			return tokens;
		}

		@Override
		public int size() {
			return tokens.size();
		}

		@Override
		public boolean isExpression() {
			return true;
		}

		@Override
		public boolean isEmpty() {
			return tokens.isEmpty();
		}

		public boolean requiresPrimaryAlias() {
			return requiresPrimaryAlias;
		}

	}

}
