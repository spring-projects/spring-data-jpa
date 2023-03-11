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

import static org.springframework.data.jpa.repository.query.JpaQueryParsingToken.*;

import java.util.List;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.lang.Nullable;

/**
 * Operations needed to parse a JPA query.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
abstract class JpaQueryParser {

	private static final Pattern PUNCTUATION_PATTERN = Pattern.compile(".*((?![._])[\\p{Punct}|\\s])");

	private static final String UNSAFE_PROPERTY_REFERENCE = "Sort expression '%s' must only contain property references or "
			+ "aliases used in the select clause; If you really want to use something other than that for sorting, please use "
			+ "JpaSort.unsafe(…)";

	private final DeclaredQuery declaredQuery;

	JpaQueryParser(DeclaredQuery declaredQuery) {
		this.declaredQuery = declaredQuery;
	}

	JpaQueryParser(String query) {
		this(DeclaredQuery.of(query, false));
	}

	DeclaredQuery getDeclaredQuery() {
		return declaredQuery;
	}

	String getQuery() {
		return getDeclaredQuery().getQueryString();
	}

	/**
	 * Generate a query using the original query with an @literal order by} clause added (or amended) based upon the
	 * provider {@link Sort} parameter.
	 *
	 * @param sort can be {@literal null}
	 */
	String createQuery(Sort sort) {

		try {
			ParserRuleContext parsedQuery = parse();

			if (parsedQuery == null) {
				return "";
			}

			return render(doCreateQuery(parsedQuery, sort));
		} catch (JpaQueryParsingSyntaxError e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Generate a count-based query using the original query.
	 *
	 * @param countProjection
	 */
	String createCountQuery(@Nullable String countProjection) {

		try {
			ParserRuleContext parsedQuery = parse();

			if (parsedQuery == null) {
				return "";
			}

			return render(doCreateCountQuery(parsedQuery, countProjection));
		} catch (JpaQueryParsingSyntaxError e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find the projection of the query.
	 *
	 * @param parsedQuery
	 */
	String projection() {

		try {
			ParserRuleContext parsedQuery = parse();

			if (parsedQuery == null) {
				return "";
			}

			return render(doFindProjection(parsedQuery));
		} catch (JpaQueryParsingSyntaxError e) {
			return "";
		}
	}

	/**
	 * Find the alias of the query's primary FROM clause
	 *
	 * @return can be {@literal null}
	 */
	String findAlias() {

		try {
			ParserRuleContext parsedQuery = parse();

			if (parsedQuery == null) {
				return null;
			}

			return doFindAlias(parsedQuery);
		} catch (JpaQueryParsingSyntaxError e) {
			return null;
		}
	}

	/**
	 * Discern if the query has a {@code new com.example.Dto()} DTO constructor in the select clause.
	 *
	 * @return Guaranteed to be {@literal true} or {@literal false}.
	 */
	boolean hasConstructorExpression() {

		try {
			ParserRuleContext parsedQuery = parse();

			if (parsedQuery == null) {
				return false;
			}

			return doCheckForConstructor(parsedQuery);
		} catch (JpaQueryParsingSyntaxError e) {
			return false;
		}
	}

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

	/**
	 * Parse the JPA query using its corresponding ANTLR parser.
	 */
	protected abstract ParserRuleContext parse();

	/**
	 * Create a {@link JpaQueryParsingToken}-based query with an {@literal order by} applied/amended based upon the
	 * {@link Sort} parameter.
	 *
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 */
	protected abstract List<JpaQueryParsingToken> doCreateQuery(ParserRuleContext parsedQuery, Sort sort);

	/**
	 * Create a {@link JpaQueryParsingToken}-based count query.
	 *
	 * @param parsedQuery
	 * @param countProjection
	 */
	protected abstract List<JpaQueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery,
																	 @Nullable String countProjection);

	protected abstract String doFindAlias(ParserRuleContext parsedQuery);

	/**
	 * Find the projection of the query's primary SELECT clause.
	 *
	 * @param parsedQuery
	 */
	protected abstract List<JpaQueryParsingToken> doFindProjection(ParserRuleContext parsedQuery);

	protected abstract boolean doCheckForConstructor(ParserRuleContext parsedQuery);

}
