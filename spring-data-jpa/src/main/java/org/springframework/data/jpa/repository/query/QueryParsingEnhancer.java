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

import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.domain.Sort;

import com.mysema.commons.lang.Assert;

/**
 * Implementation of {@link QueryEnhancer} using a {@link QueryParser}.<br/>
 * <br/>
 * NOTE: The parser can find everything it needs for create sorted and count queries. Thus, looking up the alias or the
 * projection isn't needed for its primary function, and are simply implemented for test purposes.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class QueryParsingEnhancer implements QueryEnhancer {

	private static final Log LOG = LogFactory.getLog(QueryParsingEnhancer.class);

	private final QueryParser queryParser;

	/**
	 * Initialize with an {@link QueryParser}.
	 * 
	 * @param queryParser
	 */
	public QueryParsingEnhancer(QueryParser queryParser) {

		Assert.notNull(queryParser, "queryParse must not be null!");
		this.queryParser = queryParser;
	}

	public QueryParser getQueryParsingStrategy() {
		return queryParser;
	}

	/**
	 * Adds an {@literal order by} clause to the JPA query.
	 *
	 * @param sort the sort specification to apply.
	 * @return
	 */
	@Override
	public String applySorting(Sort sort) {

		try {
			ParserRuleContext parsedQuery = queryParser.parse();

			if (parsedQuery == null) {
				return "";
			}

			return queryParser.createQuery(parsedQuery, sort);
		} catch (QueryParsingSyntaxError e) {
			LOG.warn(e);
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Because the parser can find the alias of the FROM clause, there is no need to "find it" in advance.
	 *
	 * @param sort the sort specification to apply.
	 * @param alias IGNORED
	 * @return
	 */
	@Override
	public String applySorting(Sort sort, String alias) {
		return applySorting(sort);
	}

	/**
	 * Resolves the alias for the entity in the FROM clause from the JPA query. Since the {@link QueryParser} can already
	 * find the alias when generating sorted and count queries, this is mainly to serve test cases.
	 */
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
			LOG.warn(e);
			return null;
		}
	}

	/**
	 * Creates a count query from the original query.
	 * 
	 * @return Guaranteed to be not {@literal null};
	 */
	@Override
	public String createCountQueryFor() {

		try {
			ParserRuleContext parsedQuery = queryParser.parse();

			if (parsedQuery == null) {
				return "";
			}

			return queryParser.createCountQuery(parsedQuery);
		} catch (QueryParsingSyntaxError e) {
			LOG.warn(e);
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Because the parser can handle projections, there is no need to "find it" in advance to create the count query.
	 *
	 * @param countProjection IGNORED
	 * @return
	 */
	@Override
	public String createCountQueryFor(String countProjection) {
		return createCountQueryFor();
	}

	/**
	 * Checks if the select clause has a new constructor instantiation in the JPA query.
	 *
	 * @return Guaranteed to return {@literal true} or {@literal false}.
	 */
	@Override
	public boolean hasConstructorExpression() {

		try {
			ParserRuleContext parsedQuery = queryParser.parse();

			if (parsedQuery == null) {
				return false;
			}

			return queryParser.hasConstructor(parsedQuery);
		} catch (QueryParsingSyntaxError e) {
			LOG.warn(e);
			return false;
		}
	}

	/**
	 * Looks up the projection of the JPA query. Since the {@link QueryParser} can already find the projection when
	 * generating sorted and count queries, this is mainly to serve test cases.
	 */
	@Override
	public String getProjection() {

		try {
			ParserRuleContext parsedQuery = queryParser.parse();

			if (parsedQuery == null) {
				return "";
			}

			return queryParser.projection(parsedQuery);
		} catch (QueryParsingSyntaxError e) {
			LOG.debug(e);
			return "";
		}
	}

	/**
	 * Since the {@link QueryParser} can already fully transform sorted and count queries by itself, this is a placeholder
	 * method.
	 *
	 * @return empty set
	 */
	@Override
	public Set<String> getJoinAliases() {
		return Set.of();
	}

	@Override
	public DeclaredQuery getQuery() {
		return queryParser.getDeclaredQuery();
	}
}
