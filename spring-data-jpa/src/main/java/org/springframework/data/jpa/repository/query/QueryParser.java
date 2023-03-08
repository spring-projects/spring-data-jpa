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

import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.data.domain.Sort;

/**
 * Operations needed to parse a JPA query.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
public interface QueryParser {

	DeclaredQuery getDeclaredQuery();

	default String getQuery() {
		return getDeclaredQuery().getQueryString();
	}

	ParserRuleContext parse();

	default String createQuery(ParserRuleContext parsedQuery, Sort sort) {
		return render(doCreateQuery(parsedQuery, sort));
	}

	default String createCountQuery(ParserRuleContext parsedQuery) {
		return render(doCreateCountQuery(parsedQuery));
	}

	default String projection(ParserRuleContext parsedQuery) {
		return render(doFindProjection(parsedQuery));
	}

	List<QueryParsingToken> doCreateQuery(ParserRuleContext parsedQuery, Sort sort);

	List<QueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery);

	String findAlias(ParserRuleContext parsedQuery);

	List<QueryParsingToken> doFindProjection(ParserRuleContext parsedQuery);

	boolean hasConstructor(ParserRuleContext parsedQuery);

	/**
	 * Render the list of {@link QueryParsingToken}s into a query string.
	 *
	 * @param tokens
	 */
	private String render(List<QueryParsingToken> tokens) {

		if (tokens == null) {
			return "";
		}

		StringBuilder results = new StringBuilder();

		tokens.stream() //
				.filter(token -> !token.isDebugOnly()) //
				.forEach(token -> {
					String tokenValue = token.getToken();
					results.append(tokenValue);
					if (token.getSpace()) {
						results.append(" ");
					}
				});

		return results.toString().trim();
	}
}
