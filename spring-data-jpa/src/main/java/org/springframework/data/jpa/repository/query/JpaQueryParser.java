/*
 * Copyright 2022-2024 the original author or authors.
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
import java.util.function.BiFunction;
import java.util.function.Function;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * Operations needed to parse a JPA query.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.1
 */
abstract class JpaQueryParser {

	private final ParserRuleContext context;
	private final ParsedQueryIntrospector introspector;
	private final String projection;
	private final BiFunction<Sort, String, ParseTreeVisitor<? extends Object>> sortFunction;
	private final BiFunction<String, String, ParseTreeVisitor<? extends Object>> countQueryFunction;

	JpaQueryParser(ParserRuleContext context, ParsedQueryIntrospector introspector,
			@Nullable BiFunction<Sort, String, ParseTreeVisitor<? extends Object>> sortFunction,
			@Nullable BiFunction<String, String, ParseTreeVisitor<? extends Object>> countQueryFunction) {

		this.context = context;
		this.introspector = introspector;
		this.sortFunction = sortFunction;
		this.countQueryFunction = countQueryFunction;
		this.introspector.visit(context);

		List<JpaQueryParsingToken> tokens = introspector.getProjection();
		this.projection = tokens.isEmpty() ? "" : render(tokens);
	}

	static <P extends Parser> ParserRuleContext parse(String query, Function<CharStream, Lexer> lexerFactoryFunction,
			Function<TokenStream, P> parserFactoryFunction, Function<P, ParserRuleContext> parseFunction) {

		Lexer lexer = lexerFactoryFunction.apply(CharStreams.fromString(query));
		P parser = parserFactoryFunction.apply(new CommonTokenStream(lexer));

		configureParser(query, lexer, parser);

		return parseFunction.apply(parser);
	}

	/**
	 * Generate a query using the original query with an {@literal order by} clause added (or amended) based upon the
	 * provider {@link Sort} parameter.
	 *
	 * @param sort can be {@literal null}
	 */
	String renderSortedQuery(Sort sort) {
		return render(sortFunction.apply(sort, findAlias()).visit(context));
	}

	/**
	 * Generate a count-based query derived from the original query.
	 *
	 * @param countProjection
	 */
	String createCountQuery(@Nullable String countProjection) {
		return render(countQueryFunction.apply(countProjection, findAlias()).visit(context));
	}

	/**
	 * Find the projection of the query.
	 */
	String getProjection() {
		return this.projection;
	}

	/**
	 * Find the alias of the query's primary FROM clause
	 *
	 * @return can be {@literal null}
	 */
	@Nullable
	String findAlias() {
		return this.introspector.getAlias();
	}

	/**
	 * Discern if the query has a {@code new com.example.Dto()} DTO constructor in the select clause.
	 *
	 * @return Guaranteed to be {@literal true} or {@literal false}.
	 */
	boolean hasConstructorExpression() {
		return this.introspector.hasConstructorExpression();
	}

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

}
