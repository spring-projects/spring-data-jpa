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

import java.util.List;
import java.util.Set;
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
import org.springframework.util.Assert;

/**
 * Implementation of {@link QueryEnhancer} to enhance JPA queries using ANTLR parsers.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.1
 * @see JpqlQueryParser
 * @see HqlQueryParser
 * @see EqlQueryParser
 */
class JpaQueryEnhancer implements QueryEnhancer {

	private final ParserRuleContext context;
	private final ParsedQueryIntrospector introspector;
	private final String projection;
	private final BiFunction<Sort, String, ParseTreeVisitor<? extends Object>> sortFunction;
	private final BiFunction<String, String, ParseTreeVisitor<? extends Object>> countQueryFunction;

	JpaQueryEnhancer(ParserRuleContext context, ParsedQueryIntrospector introspector,
			@Nullable BiFunction<Sort, String, ParseTreeVisitor<? extends Object>> sortFunction,
			@Nullable BiFunction<String, String, ParseTreeVisitor<? extends Object>> countQueryFunction) {

		this.context = context;
		this.introspector = introspector;
		this.sortFunction = sortFunction;
		this.countQueryFunction = countQueryFunction;
		this.introspector.visit(context);

		List<JpaQueryParsingToken> tokens = introspector.getProjection();
		this.projection = tokens.isEmpty() ? "" : QueryRenderer.TokenRenderer.render(tokens);
	}

	static <P extends Parser> ParserRuleContext parse(String query, Function<CharStream, Lexer> lexerFactoryFunction,
			Function<TokenStream, P> parserFactoryFunction, Function<P, ParserRuleContext> parseFunction) {

		Lexer lexer = lexerFactoryFunction.apply(CharStreams.fromString(query));
		P parser = parserFactoryFunction.apply(new CommonTokenStream(lexer));

		configureParser(query, lexer, parser);

		return parseFunction.apply(parser);
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

	/**
	 * Factory method to create a {@link JpaQueryEnhancer} for {@link DeclaredQuery} using JPQL grammar.
	 *
	 * @param query must not be {@literal null}.
	 * @return a new {@link JpaQueryEnhancer} using JPQL.
	 */
	public static JpaQueryEnhancer forJpql(DeclaredQuery query) {

		Assert.notNull(query, "DeclaredQuery must not be null!");

		return JpqlQueryParser.parseQuery(query.getQueryString());
	}

	/**
	 * Factory method to create a {@link JpaQueryEnhancer} for {@link DeclaredQuery} using HQL grammar.
	 *
	 * @param query must not be {@literal null}.
	 * @return a new {@link JpaQueryEnhancer} using HQL.
	 */
	public static JpaQueryEnhancer forHql(DeclaredQuery query) {

		Assert.notNull(query, "DeclaredQuery must not be null!");

		return HqlQueryParser.parseQuery(query.getQueryString());
	}

	/**
	 * Factory method to create a {@link JpaQueryEnhancer} for {@link DeclaredQuery} using EQL grammar.
	 *
	 * @param query must not be {@literal null}.
	 * @return a new {@link JpaQueryEnhancer} using EQL.
	 * @since 3.2
	 */
	public static JpaQueryEnhancer forEql(DeclaredQuery query) {

		Assert.notNull(query, "DeclaredQuery must not be null!");

		return EqlQueryParser.parseQuery(query.getQueryString());
	}

	/**
	 * Checks if the select clause has a new constructor instantiation in the JPA query.
	 *
	 * @return Guaranteed to return {@literal true} or {@literal false}.
	 */
	@Override
	public boolean hasConstructorExpression() {
		return this.introspector.hasConstructorExpression();
	}

	/**
	 * Resolves the alias for the entity in the FROM clause from the JPA query. Since the {@link JpaQueryParser} can
	 * already find the alias when generating sorted and count queries, this is mainly to serve test cases.
	 */
	@Override
	public String detectAlias() {
		return this.introspector.getAlias();
	}

	/**
	 * Looks up the projection of the JPA query. Since the {@link JpaQueryParser} can already find the projection when
	 * generating sorted and count queries, this is mainly to serve test cases.
	 */
	@Override
	public String getProjection() {
		return this.projection;
	}

	/**
	 * Since the {@link JpaQueryParser} can already fully transform sorted and count queries by itself, this is a
	 * placeholder method.
	 *
	 * @return empty set
	 */
	@Override
	public Set<String> getJoinAliases() {
		return Set.of();
	}

	/**
	 * Look up the {@link DeclaredQuery} from the {@link JpaQueryParser}.
	 */
	@Override
	public DeclaredQuery getQuery() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Adds an {@literal order by} clause to the JPA query.
	 *
	 * @param sort the sort specification to apply.
	 * @return
	 */
	@Override
	public String applySorting(Sort sort) {
		return QueryRenderer.TokenRenderer.render(sortFunction.apply(sort, detectAlias()).visit(context));
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
	 * Creates a count query from the original query, with no count projection.
	 *
	 * @return Guaranteed to be not {@literal null};
	 */
	@Override
	public String createCountQueryFor() {
		return createCountQueryFor(null);
	}

	/**
	 * Create a count query from the original query, with potential custom projection.
	 *
	 * @param countProjection may be {@literal null}.
	 */
	@Override
	public String createCountQueryFor(@Nullable String countProjection) {
		return QueryRenderer.TokenRenderer.render(countQueryFunction.apply(countProjection, detectAlias()).visit(context));
	}

	/**
	 * Implements the {@code HQL} parsing operations of a {@link JpaQueryEnhancer} using the ANTLR-generated
	 * {@link HqlParser} and {@link HqlSortedQueryTransformer}.
	 *
	 * @author Greg Turnquist
	 * @author Mark Paluch
	 * @since 3.1
	 */
	static class HqlQueryParser extends JpaQueryEnhancer {

		private HqlQueryParser(String query) {
			super(parse(query, HqlLexer::new, HqlParser::new, HqlParser::start), new HqlQueryIntrospector(),
					HqlSortedQueryTransformer::new, HqlCountQueryTransformer::new);
		}

		/**
		 * Parse a HQL query.
		 *
		 * @param query
		 * @return the query parser.
		 * @throws BadJpqlGrammarException
		 */
		public static HqlQueryParser parseQuery(String query) throws BadJpqlGrammarException {
			return new HqlQueryParser(query);
		}

	}

	/**
	 * Implements the {@code EQL} parsing operations of a {@link JpaQueryEnhancer} using the ANTLR-generated
	 * {@link EqlParser}.
	 *
	 * @author Greg Turnquist
	 * @author Mark Paluch
	 * @since 3.2
	 */
	static class EqlQueryParser extends JpaQueryEnhancer {

		private EqlQueryParser(String query) {
			super(parse(query, EqlLexer::new, EqlParser::new, EqlParser::start), new EqlQueryIntrospector(),
					EqlSortedQueryTransformer::new, EqlCountQueryTransformer::new);
		}

		/**
		 * Parse a EQL query.
		 *
		 * @param query
		 * @return the query parser.
		 * @throws BadJpqlGrammarException
		 */
		public static EqlQueryParser parseQuery(String query) throws BadJpqlGrammarException {
			return new EqlQueryParser(query);
		}

	}

	/**
	 * Implements the {@code JPQL} parsing operations of a {@link JpaQueryEnhancer} using the ANTLR-generated
	 * {@link JpqlParser} and {@link JpqlSortedQueryTransformer}.
	 *
	 * @author Greg Turnquist
	 * @author Mark Paluch
	 * @since 3.1
	 */
	static class JpqlQueryParser extends JpaQueryEnhancer {

		private JpqlQueryParser(String query) {
			super(parse(query, JpqlLexer::new, JpqlParser::new, JpqlParser::start), new JpqlQueryIntrospector(),
					JpqlSortedQueryTransformer::new, JpqlCountQueryTransformer::new);
		}

		/**
		 * Parse a JPQL query.
		 *
		 * @param query
		 * @return the query parser.
		 * @throws BadJpqlGrammarException
		 */
		public static JpqlQueryParser parseQuery(String query) throws BadJpqlGrammarException {
			return new JpqlQueryParser(query);
		}
	}
}
