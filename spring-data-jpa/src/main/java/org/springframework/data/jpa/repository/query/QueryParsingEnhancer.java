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
import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.domain.Sort;

/**
 * The implementation of {@link QueryEnhancer} using {@link QueryParser}.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
public class QueryParsingEnhancer implements QueryEnhancer {

	private static final Log LOG = LogFactory.getLog(QueryParsingEnhancer.class);

	private QueryParser queryParser;

	public QueryParsingEnhancer(QueryParser queryParser) {
		this.queryParser = queryParser;
	}

	public QueryParser getQueryParsingStrategy() {
		return queryParser;
	}

	@Override
	public String applySorting(Sort sort, String alias) {

		ParserRuleContext parsedQuery = queryParser.parse();

		if (parsedQuery == null) {
			return "";
		}

		queryParser.setSort(sort);
		return render(queryParser.applySorting(parsedQuery));
	}

	@Override
	public String detectAlias() {

		try {
			ParserRuleContext parsedQuery = queryParser.parse();

			if (parsedQuery == null) {

				LOG.warn("Failed to parse " + queryParser.getQuery() + ". See console for more details.");
				return null;
			}

			return queryParser.findAlias(parsedQuery);
		} catch (QueryParsingSyntaxError e) {
			LOG.debug(e);
			return null;
		}
	}

	@Override
	public String createCountQueryFor(String countProjection) {

		ParserRuleContext parsedQuery = queryParser.parse();

		if (parsedQuery == null) {
			return "";
		}

		try {
			return render(queryParser.count(parsedQuery));
		} catch (QueryParsingSyntaxError e) {
			LOG.error(e);
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public String getProjection() {

		ParserRuleContext parsedQuery = queryParser.parse();

		if (parsedQuery == null) {
			return "";
		}

		try {
			return render(queryParser.projection(parsedQuery));
		} catch (QueryParsingSyntaxError e) {
			LOG.debug(e);
			return "";
		}
	}

	@Override
	public Set<String> getJoinAliases() {
		return Set.of();
	}

	@Override
	public DeclaredQuery getQuery() {
		return queryParser.getDeclaredQuery();
	}

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
