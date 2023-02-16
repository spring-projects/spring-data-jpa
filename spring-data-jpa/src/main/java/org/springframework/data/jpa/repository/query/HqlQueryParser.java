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
 * Implements the parsing operations of a {@link JpaQueryParser} using the ANTLR-generated {@link HqlParser} and
 * {@link HqlQueryTransformer}.
 * 
 * @author Greg Turnquist
 * @since 3.1
 */
class HqlQueryParser extends JpaQueryParser {

	HqlQueryParser(DeclaredQuery declaredQuery) {
		super(declaredQuery);
	}

	HqlQueryParser(String query) {
		super(query);
	}

	/**
	 * Convenience method to parse an HQL query. Will throw a {@link JpaQueryParsingSyntaxError} if the query is invalid.
	 *
	 * @param query
	 * @return a parsed query, ready for postprocessing
	 */
	static ParserRuleContext parse(String query) {

		HqlLexer lexer = new HqlLexer(CharStreams.fromString(query));
		HqlParser parser = new HqlParser(new CommonTokenStream(lexer));

		parser.addErrorListener(new JpaQueryParsingSyntaxErrorListener());

		return parser.start();
	}

	/**
	 * Parse the query using {@link #parse(String)}.
	 *
	 * @return a parsed query
	 */
	@Override
	protected ParserRuleContext parse() {
		return parse(getQuery());
	}

	/**
	 * Use the {@link HqlQueryTransformer} to transform the original query into a query with the {@link Sort} applied.
	 *
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 * @return list of {@link JpaQueryParsingToken}s
	 */
	@Override
	protected List<JpaQueryParsingToken> doCreateQuery(ParserRuleContext parsedQuery, Sort sort) {
		return new HqlQueryTransformer(sort).visit(parsedQuery);
	}

	/**
	 * Use the {@link HqlQueryTransformer} to transform the original query into a count query.
	 *
	 * @param parsedQuery
	 * @param countProjection
	 * @return list of {@link JpaQueryParsingToken}s
	 */
	@Override
	protected List<JpaQueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery,
			@Nullable String countProjection) {
		return new HqlQueryTransformer(true, countProjection).visit(parsedQuery);
	}

	/**
	 * Run the parsed query through {@link HqlQueryTransformer} to find the primary FROM clause's alias.
	 *
	 * @param parsedQuery
	 * @return can be {@literal null}
	 */
	@Override
	protected String doFindAlias(ParserRuleContext parsedQuery) {

		HqlQueryTransformer transformVisitor = new HqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getAlias();
	}

	/**
	 * Use {@link HqlQueryTransformer} to find the projection of the query.
	 *
	 * @param parsedQuery
	 * @return
	 */
	@Override
	protected List<JpaQueryParsingToken> doFindProjection(ParserRuleContext parsedQuery) {

		HqlQueryTransformer transformVisitor = new HqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getProjection();
	}

	/**
	 * Use {@link HqlQueryTransformer} to detect if the query uses a {@code new com.example.Dto()} DTO constructor in the
	 * primary select clause.
	 *
	 * @param parsedQuery
	 * @return Guaranteed to be {@literal true} or {@literal false}.
	 */
	@Override
	protected boolean doCheckForConstructor(ParserRuleContext parsedQuery) {

		HqlQueryTransformer transformVisitor = new HqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.hasConstructorExpression();
	}
}
