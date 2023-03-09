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
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.lang.Nullable;
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
	 * Generate a query using the original query with an @literal order by} clause added (or amended) based upon the
	 * provider {@link Sort} parameter.
	 *
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 */
	String createQuery(ParserRuleContext parsedQuery, Sort sort) {

		Assert.notNull(parsedQuery, "parsedQuery cannot be null!");
		return render(doCreateQuery(parsedQuery, sort));
	}

	/**
	 * Generate a count-based query using the original query.
	 *
	 * @param parsedQuery
	 * @param countProjection
	 */
	String createCountQuery(ParserRuleContext parsedQuery, @Nullable String countProjection) {

		Assert.notNull(parsedQuery, "parsedQuery cannot be null!");
		return render(doCreateCountQuery(parsedQuery, countProjection));
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
	 * Create a {@link QueryParsingToken}-based query with an {@literal order by} applied/amended based upon the
	 * {@link Sort} parameter.
	 * 
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 */
	abstract List<QueryParsingToken> doCreateQuery(ParserRuleContext parsedQuery, Sort sort);

	/**
	 * Create a {@link QueryParsingToken}-based count query.
	 *
	 * @param parsedQuery
	 * @param countProjection
	 */
	abstract List<QueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery, @Nullable String countProjection);

	/**
	 * Find the alias of the query's primary FROM clause
	 *
	 * @return can be {@literal null}
	 */
	abstract String findAlias(ParserRuleContext parsedQuery);

	/**
	 * Find the projection of the query's primary SELECT clause.
	 *
	 * @param parsedQuery
	 */
	abstract List<QueryParsingToken> doFindProjection(ParserRuleContext parsedQuery);

	/**
	 * Discern if the query has a {@code new com.example.Dto()} DTO constructor in the select clause.
	 * 
	 * @param parsedQuery
	 * @return Guaranteed to be {@literal true} or {@literal false}.
	 */
	abstract boolean hasConstructor(ParserRuleContext parsedQuery);

	private static final Pattern PUNCTUATION_PATTERN = Pattern.compile(".*((?![._])[\\p{Punct}|\\s])");

	private static final String UNSAFE_PROPERTY_REFERENCE = "Sort expression '%s' must only contain property references or "
			+ "aliases used in the select clause; If you really want to use something other than that for sorting, please use "
			+ "JpaSort.unsafe(…)";

	/**
	 * Check any given {@link JpaSort.JpaOrder#isUnsafe()} order for presence of at least one property offending the
	 * {@link #PUNCTUATION_PATTERN} and throw an {@link Exception} indicating potential unsafe order by expression.
	 *
	 * @param order
	 */
	static void checkSortExpression(Sort.Order order) {

		if (order instanceof JpaSort.JpaOrder && ((JpaSort.JpaOrder) order).isUnsafe()) {
			return;
		}

		if (PUNCTUATION_PATTERN.matcher(order.getProperty()).find()) {
			throw new InvalidDataAccessApiUsageException(String.format(UNSAFE_PROPERTY_REFERENCE, order));
		}
	}

}
