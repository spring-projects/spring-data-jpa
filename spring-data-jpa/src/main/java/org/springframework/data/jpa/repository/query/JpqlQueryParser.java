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
 * Implements the various parsing operations using the ANTLR-generated {@link JpqlParser} and
 * {@link JpqlQueryTransformer}.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class JpqlQueryParser extends QueryParser {

	JpqlQueryParser(DeclaredQuery declaredQuery) {
		super(declaredQuery);
	}

	JpqlQueryParser(String query) {
		super(query);
	}

	/**
	 * Convenience method to parse a JPQL query using the ANTLR-generated {@link JpqlParser}.
	 *
	 * @param query
	 * @return a parsed query, ready for postprocessing
	 */
	static ParserRuleContext parse(String query) {

		JpqlLexer lexer = new JpqlLexer(CharStreams.fromString(query));
		JpqlParser parser = new JpqlParser(new CommonTokenStream(lexer));

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
	 * Use the {@link JpqlQueryTransformer} to transform the parsed query into a query with the {@link Sort} applied.
	 *
	 * @param parsedQuery
	 * @param sort can be {@literal null}
	 * @return list of {@link QueryParsingToken}s
	 */
	@Override
	List<QueryParsingToken> doCreateQuery(ParserRuleContext parsedQuery, Sort sort) {
		return new JpqlQueryTransformer(sort).visit(parsedQuery);
	}

	/**
	 * Use the {@link JpqlQueryTransformer} to transform the parsed query into a count query.
	 *
	 * @param parsedQuery
	 * @return list of {@link QueryParsingToken}s
	 */
	@Override
	List<QueryParsingToken> doCreateCountQuery(ParserRuleContext parsedQuery) {
		return new JpqlQueryTransformer(true).visit(parsedQuery);
	}

	/**
	 * Using the parsed query, run it through the {@link JpqlQueryTransformer} and look up its alias.
	 *
	 * @param parsedQuery
	 * @return can be {@literal null}
	 */
	@Override
	String findAlias(ParserRuleContext parsedQuery) {

		JpqlQueryTransformer transformVisitor = new JpqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getAlias();
	}

	/**
	 * Find the projection portion of the query.
	 *
	 * @param parsedQuery
	 * @return
	 */
	@Override
	List<QueryParsingToken> doFindProjection(ParserRuleContext parsedQuery) {

		JpqlQueryTransformer transformVisitor = new JpqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.getProjection();
	}

	/**
	 * Discern if the query has a new {@code com.example.Dto()} DTO constructor in the select clause.
	 *
	 * @param parsedQuery
	 * @return Guaranteed to be {@literal true} or {@literal false}.
	 */
	@Override
	boolean hasConstructor(ParserRuleContext parsedQuery) {

		JpqlQueryTransformer transformVisitor = new JpqlQueryTransformer();
		transformVisitor.visit(parsedQuery);
		return transformVisitor.hasConstructorExpression();
	}
}
