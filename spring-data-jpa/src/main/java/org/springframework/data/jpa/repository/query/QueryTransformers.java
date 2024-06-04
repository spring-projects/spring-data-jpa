/*
 * Copyright 2024 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.JpaQueryParsingToken.*;

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

	static CountSelectionTokenStream filterCountSelection(QueryTokenStream<QueryToken> selection) {

		List<QueryToken> target = new ArrayList<>(selection.estimatedSize());
		boolean skipNext = false;
		boolean containsNew = false;

		for (QueryToken token : selection) {


			if (skipNext) {
				skipNext = false;
				continue;
			}

			if (token.isA(TOKEN_AS)) {
				skipNext = true;
				continue;
			}

			if (!token.isA(TOKEN_COMMA) && token.isExpression()) {
				token = JpaQueryParsingToken.token(token.value());
			}

			if(!containsNew && token.value().contains("new")) {
				containsNew = true;
			}

			target.add(token);
		}

		return new CountSelectionTokenStream(target, containsNew);
	}

	static class CountSelectionTokenStream implements QueryTokenStream<QueryToken> {

		private final List<QueryToken> tokens;
		private final boolean requiresPrimaryAlias;

		public CountSelectionTokenStream(List<QueryToken> tokens, boolean containsNew) {
			this.tokens = tokens;
			this.requiresPrimaryAlias = containsNew;
		}

		@Override
		public int estimatedSize() {
			return tokens.size();
		}

		@Override
		public Iterator<QueryToken> iterator() {
			return tokens.iterator();
		}

		@Override
		public List<QueryToken> toList() {
			return tokens;
		}

		public boolean requiresPrimaryAlias() {
			return requiresPrimaryAlias;
		}
	}

}
