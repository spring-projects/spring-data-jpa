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

/**
 * Implements the various parsing operations using the ANTLR-generated {@link HqlParser} and
 * {@link HqlQueryTransformer}.
 * 
 * @author Greg Turnquist
 * @since 3.1
 */
class HqlQueryParser extends QueryParser {

	HqlQueryParser(DeclaredQuery declaredQuery) {
		super(declaredQuery);
	}

	HqlQueryParser(String query) {
		super(query);
	}

	/**
	 * Convenience method to parse an HQL query using the ANTLR-generated {@link HqlParser}.
	 *
	 * @param query
	 * @return a parsed query, ready for postprocessing
	 */
	static ParserRuleContext parse(String query) {

		HqlLexer lexer = new HqlLexer(CharStreams.fromString(query));
		HqlParser parser = new HqlParser(new CommonTokenStream(lexer));

		parser.addErrorListener(new QueryParsingSyntaxErrorListener());

		return parser.start();
	}

	/**
	 * Parse the query using {@link #parse(String)}.
	 *
	 * @return a parsed query
	 */
	@Override
	ParserRuleContext parse() {
		return parse(getQuery());
	}

	/**
	 * Use the {@link HqlQueryTransformer} to transform the parsed query into a query with the {@link Sort} applied.
	 *
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 * @return list of {@link QueryParsingToken}s
	 */
	@Override
	List<QueryParsingToken> doCreateQuery(ParserRuleContext parsedQuery, Sort sort) {
		return new HqlQueryTransformer(sort).visit(parsedQuery);
	}

	/**
	 * Use the {@link HqlQueryTransformer} to transform the parsed query into a count query.
	 *
	 * @param parsedQuery
	 * @return list of {@link QueryParsingToken}s
	 */
	@Override
	List<QueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery) {
		return new HqlQueryTransformer(true).visit(parsedQuery);
	}

	/**
	 * Using the parsed query, run it through the {@link HqlQueryTransformer} and look up its alias.
	 *
	 * @param parsedQuery
	 * @return can be {@literal null}
	 */
	@Override
	String findAlias(ParserRuleContext parsedQuery) {

		HqlQueryTransformer transformVisitor = new HqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getAlias();
	}

	/**
	 * Discern if the query has a new {@code com.example.Dto()} DTO constructor in the select clause.
	 *
	 * @param parsedQuery
	 * @return Guaranteed to be {@literal true} or {@literal false}.
	 */
	@Override
	List<QueryParsingToken> doFindProjection(ParserRuleContext parsedQuery) {

		HqlQueryTransformer transformVisitor = new HqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getProjection();
	}

	@Override
	boolean hasConstructor(ParserRuleContext parsedQuery) {

		HqlQueryTransformer transformVisitor = new HqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.hasConstructorExpression();
	}
}
