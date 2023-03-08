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

import static org.springframework.data.jpa.repository.query.QueryParsingToken.*;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Operations needed to parse a JPA query.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
abstract class QueryParser {

	private final DeclaredQuery declaredQuery;

	QueryParser(DeclaredQuery declaredQuery) {
		this.declaredQuery = declaredQuery;
	}

	QueryParser(String query) {
		this(DeclaredQuery.of(query, false));
	}

	DeclaredQuery getDeclaredQuery() {
		return declaredQuery;
	}

	String getQuery() {
		return getDeclaredQuery().getQueryString();
	}

	/**
	 * Parse the JPA query using its corresponding ANTLR parser.
	 */
	abstract ParserRuleContext parse();

	/**
	 * Create a string-based query using the original query with an @literal order by} added (or amended) based upon
	 * {@link Sort}
	 *
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 */
	String createQuery(ParserRuleContext parsedQuery, Sort sort) {

		Assert.notNull(parsedQuery, "parsedQuery cannot be null!");
		return render(doCreateQuery(parsedQuery, sort));
	}

	/**
	 * Create a string-based count query using the original query.
	 *
	 * @param parsedQuery
	 */
	String createCountQuery(ParserRuleContext parsedQuery) {

		Assert.notNull(parsedQuery, "parsedQuery cannot be null!");
		return render(doCreateCountQuery(parsedQuery));
	}

	/**
	 * Find the projection of the query.
	 * 
	 * @param parsedQuery
	 */
	String projection(ParserRuleContext parsedQuery) {

		Assert.notNull(parsedQuery, "parsedQuery cannot be null!");
		return render(doFindProjection(parsedQuery));
	}

	/**
	 * Create a {@link QueryParsingToken}-based query with an {@literal order by} applied/amended based upon {@link Sort}.
	 * 
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 */
	abstract List<QueryParsingToken> doCreateQuery(ParserRuleContext parsedQuery, Sort sort);

	/**
	 * Create a {@link QueryParsingToken}-based count query.
	 * 
	 * @param parsedQuery
	 */
	abstract List<QueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery);

	/**
	 * Find the alias of the query's FROM clause
	 *
	 * @return can be {@literal null}
	 */
	abstract String findAlias(ParserRuleContext parsedQuery);

	/**
	 * Find the projection of the query's selection clause.
	 *
	 * @param parsedQuery
	 */
	abstract List<QueryParsingToken> doFindProjection(ParserRuleContext parsedQuery);

	/**
	 * Discern if the query has a new {@code com.example.Dto()} DTO constructor in the select clause.
	 * 
	 * @param parsedQuery
	 * @return Guaranteed to be {@literal true} or {@literal false}.
	 */
	abstract boolean hasConstructor(ParserRuleContext parsedQuery);

}
