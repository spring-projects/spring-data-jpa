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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * Implements the {@code JPQL} parsing operations of a {@link JpaQueryParserSupport} using the ANTLR-generated
 * {@link JpqlParser} and {@link JpqlQueryTransformer}.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.1
 */
class JpqlQueryParser extends JpaQueryParserSupport {

	JpqlQueryParser(String query) {
		super(query);
	}

	/**
	 * Convenience method to parse a JPQL query. Will throw a {@link BadJpqlGrammarException} if the query is invalid.
	 *
	 * @param query
	 * @return a parsed query, ready for postprocessing
	 */
	public static ParserRuleContext parseQuery(String query) {

		JpqlLexer lexer = new JpqlLexer(CharStreams.fromString(query));
		JpqlParser parser = new JpqlParser(new CommonTokenStream(lexer));

		configureParser(query, lexer, parser);

		return parser.start();
	}


	/**
	 * Parse the query using {@link #parseQuery(String)}.
	 *
	 * @return a parsed query
	 */
	@Override
	protected ParserRuleContext parse(String query) {
		return parseQuery(query);
	}

	/**
	 * Use the {@link JpqlQueryTransformer} to transform the original query into a query with the {@link Sort} applied.
	 *
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 * @return list of {@link JpaQueryParsingToken}s
	 */
	@Override
	protected List<JpaQueryParsingToken> applySort(ParserRuleContext parsedQuery, Sort sort) {
		return new JpqlQueryTransformer(sort).visit(parsedQuery);
	}

	/**
	 * Use the {@link JpqlQueryTransformer} to transform the original query into a count query.
	 *
	 * @param parsedQuery
	 * @param countProjection
	 * @return list of {@link JpaQueryParsingToken}s
	 */
	@Override
	protected List<JpaQueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery,
			@Nullable String countProjection) {
		return new JpqlQueryTransformer(true, countProjection).visit(parsedQuery);
	}

	/**
	 * Run the parsed query through {@link JpqlQueryTransformer} to find the primary FROM clause's alias.
	 *
	 * @param parsedQuery
	 * @return can be {@literal null}
	 */
	@Override
	protected String doFindAlias(ParserRuleContext parsedQuery) {

		JpqlQueryTransformer transformVisitor = new JpqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getAlias();
	}

	/**
	 * Use {@link JpqlQueryTransformer} to find the projection of the query.
	 *
	 * @param parsedQuery
	 * @return
	 */
	@Override
	protected List<JpaQueryParsingToken> doFindProjection(ParserRuleContext parsedQuery) {

		JpqlQueryTransformer transformVisitor = new JpqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getProjection();
	}

	/**
	 * Use {@link JpqlQueryTransformer} to detect if the query uses a {@code new com.example.Dto()} DTO constructor in the
	 * primary select clause.
	 *
	 * @param parsedQuery
	 * @return Guaranteed to be {@literal true} or {@literal false}.
	 */
	@Override
	protected boolean doCheckForConstructor(ParserRuleContext parsedQuery) {

		JpqlQueryTransformer transformVisitor = new JpqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.hasConstructorExpression();
	}
}
