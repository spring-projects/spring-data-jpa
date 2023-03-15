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

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;

/**
 * Operations needed to parse a JPA query.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.1
 */
abstract class JpaQueryParserSupport {

	private final ParseState state;

	JpaQueryParserSupport(String query) {
		this.state = new ParseState(query);
	}

	/**
	 * Generate a query using the original query with an @literal order by} clause added (or amended) based upon the
	 * provider {@link Sort} parameter.
	 *
	 * @param sort can be {@literal null}
	 */
	String renderSortedQuery(Sort sort) {

		try {
			return render(applySort(state.getContext(), sort));
		} catch (BadJpqlGrammarException e) {
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
			return render(doCreateCountQuery(state.getContext(), countProjection));
		} catch (BadJpqlGrammarException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Find the projection of the query.
	 */
	String projection() {

		try {
			List<JpaQueryParsingToken> tokens = doFindProjection(state.getContext());
			return tokens.isEmpty() ? "" : render(tokens);
		} catch (BadJpqlGrammarException e) {
			return "";
		}
	}

	/**
	 * Find the alias of the query's primary FROM clause
	 *
	 * @return can be {@literal null}
	 */
	@Nullable
	String findAlias() {

		try {
			return doFindAlias(state.getContext());
		} catch (BadJpqlGrammarException e) {
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
			return doCheckForConstructor(state.getContext());
		} catch (BadJpqlGrammarException e) {
			return false;
		}
	}

	/**
	 * Parse the JPA query using its corresponding ANTLR parser.
	 */
	protected abstract ParserRuleContext parse(String query);

	/**
	 * Apply common configuration (SLL prediction for performance, our own error listeners).
	 *
	 * @param query
	 * @param lexer
	 * @param parser
	 */
	static void configureParser(String query, Lexer lexer, Parser parser) {

		BadJpqlGrammarErrorListener errorListener = new BadJpqlGrammarErrorListener(query);

		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);

		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
	}

	/**
	 * Create a {@link JpaQueryParsingToken}-based query with an {@literal order by} applied/amended based upon the
	 * {@link Sort} parameter.
	 *
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 */
	protected abstract List<JpaQueryParsingToken> applySort(ParserRuleContext parsedQuery, Sort sort);

	/**
	 * Create a {@link JpaQueryParsingToken}-based count query.
	 *
	 * @param parsedQuery
	 * @param countProjection
	 */
	protected abstract List<JpaQueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery,
			@Nullable String countProjection);

	@Nullable
	protected abstract String doFindAlias(ParserRuleContext parsedQuery);

	/**
	 * Find the projection of the query's primary SELECT clause.
	 *
	 * @param parsedQuery
	 */
	protected abstract List<JpaQueryParsingToken> doFindProjection(ParserRuleContext parsedQuery);

	protected abstract boolean doCheckForConstructor(ParserRuleContext parsedQuery);

	/**
	 * Parser state capturing the lazily-parsed parser context.
	 */
	class ParseState {

		private final Lazy<ParserRuleContext> parsedQuery;
		private volatile @Nullable BadJpqlGrammarException error;
		private final String query;

		public ParseState(String query) {
			this.query = query;
			this.parsedQuery = Lazy.of(() -> parse(query));
		}

		public ParserRuleContext getContext() {

			BadJpqlGrammarException error = this.error;

			if (error != null) {
				throw error;
			}

			try {
				return parsedQuery.get();
			} catch (BadJpqlGrammarException e) {
				this.error = error = e;
				throw error;
			}
		}

		public String getQuery() {
			return query;
		}
	}

}
