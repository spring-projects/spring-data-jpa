/*
 * Copyright 2022-2025 the original author or authors.
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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ReturnedType;
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
@SuppressWarnings("removal")
class JpaQueryEnhancer<Q extends QueryInformation> implements QueryEnhancer {

	private final ParserRuleContext context;
	private final Q queryInformation;
	private final String projection;
	private final SortedQueryRewriteFunction<Q> sortFunction;
	private final BiFunction<String, Q, ParseTreeVisitor<QueryTokenStream>> countQueryFunction;

	JpaQueryEnhancer(ParserRuleContext context, ParsedQueryIntrospector<Q> introspector,
			SortedQueryRewriteFunction<Q> sortFunction,
			BiFunction<String, Q, ParseTreeVisitor<QueryTokenStream>> countQueryFunction) {

		this.context = context;
		this.sortFunction = sortFunction;
		this.countQueryFunction = countQueryFunction;
		introspector.visit(context);

		this.queryInformation = introspector.getParsedQueryInformation();

		List<QueryToken> tokens = queryInformation.getProjection();
		this.projection = tokens.isEmpty() ? "" : new QueryRenderer.TokenRenderer(tokens).render();
	}

	/**
	 * Parse the query and return the parser context (AST). This method attempts parsing the query using
	 * {@link PredictionMode#SLL} first to attempt a fast-path parse without using the context. If that fails, it retries
	 * using {@link PredictionMode#LL} which is much slower, however it allows for contextual ambiguity resolution.
	 */
	static <P extends Parser> ParserRuleContext parse(String query, Function<CharStream, Lexer> lexerFactoryFunction,
			Function<TokenStream, P> parserFactoryFunction, Function<P, ParserRuleContext> parseFunction) {

		P parser = getParser(query, lexerFactoryFunction, parserFactoryFunction);

		parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
		parser.setErrorHandler(new BailErrorStrategy() {
			@Override
			public void reportError(Parser recognizer, RecognitionException e) {

				// avoid BadJpqlGrammarException creation in the first pass.
				// recover(…) is going to handle cancellation.
			}
		});

		try {

			return parseFunction.apply(parser);
		} catch (BadJpqlGrammarException | ParseCancellationException e) {

			parser = getParser(query, lexerFactoryFunction, parserFactoryFunction);
			// fall back to LL(*)-based parsing
			parser.getInterpreter().setPredictionMode(PredictionMode.LL);

			return parseFunction.apply(parser);
		}
	}

	private static <P extends Parser> P getParser(String query, Function<CharStream, Lexer> lexerFactoryFunction,
			Function<TokenStream, P> parserFactoryFunction) {

		Lexer lexer = lexerFactoryFunction.apply(CharStreams.fromString(query));
		P parser = parserFactoryFunction.apply(new CommonTokenStream(lexer));

		String grammar = lexer.getGrammarFileName();
		int dot = grammar.lastIndexOf('.');
		if (dot != -1) {
			grammar = grammar.substring(0, dot);
		}

		configureParser(query, grammar.toUpperCase(), lexer, parser);

		return parser;
	}

	/**
	 * Apply common configuration.
	 *
	 * @param query the query input to parse.
	 * @param grammar name of the grammar.
	 * @param lexer lexer to configure.
	 * @param parser parser to configure.
	 */
	static void configureParser(String query, String grammar, Lexer lexer, Parser parser) {

		BadJpqlGrammarErrorListener errorListener = new BadJpqlGrammarErrorListener(query, grammar);

		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);

		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
	}

	/**
	 * Factory method to create a {@link JpaQueryEnhancer} for {@link IntrospectedQuery} using JPQL grammar.
	 *
	 * @param query must not be {@literal null}.
	 * @return a new {@link JpaQueryEnhancer} using JPQL.
	 */
	public static JpaQueryEnhancer<QueryInformation> forJpql(String query) {
		return JpqlQueryParser.parseQuery(query);
	}

	/**
	 * Factory method to create a {@link JpaQueryEnhancer} for {@link IntrospectedQuery} using HQL grammar.
	 *
	 * @param query must not be {@literal null}.
	 * @return a new {@link JpaQueryEnhancer} using HQL.
	 */
	public static JpaQueryEnhancer<HibernateQueryInformation> forHql(String query) {
		return HqlQueryParser.parseQuery(query);
	}

	/**
	 * Factory method to create a {@link JpaQueryEnhancer} for {@link IntrospectedQuery} using EQL grammar.
	 *
	 * @param query must not be {@literal null}.
	 * @return a new {@link JpaQueryEnhancer} using EQL.
	 * @since 3.2
	 */
	public static JpaQueryEnhancer<QueryInformation> forEql(String query) {
		return EqlQueryParser.parseQuery(query);
	}

	/**
	 * @return the parser context (AST) representing the parsed query.
	 */
	ParserRuleContext getContext() {
		return context;
	}

	/**
	 * @return the parsed query information.
	 */
	Q getQueryInformation() {
		return queryInformation;
	}

	/**
	 * Checks if the select clause has a new constructor instantiation in the JPA query.
	 *
	 * @return Guaranteed to return {@literal true} or {@literal false}.
	 */
	@Override
	public boolean hasConstructorExpression() {
		return this.queryInformation.hasConstructorExpression();
	}

	/**
	 * Resolves the alias for the entity in the FROM clause from the JPA query. Since the {@link JpaQueryParser} can
	 * already find the alias when generating sorted and count queries, this is mainly to serve test cases.
	 */
	@Override
	public @Nullable String detectAlias() {
		return this.queryInformation.getAlias();
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
	 * Since the parser can already fully transform sorted and count queries by itself, this is a placeholder method.
	 *
	 * @return empty set
	 */
	@Override
	public Set<String> getJoinAliases() {
		return Set.of();
	}

	/**
	 * Look up the {@link DeclaredQuery} from the query parser.
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
		return QueryRenderer.TokenRenderer.render(sortFunction.apply(sort, this.queryInformation, null).visit(context));
	}

	@Override
	public String rewrite(QueryRewriteInformation rewriteInformation) {
		return QueryRenderer.TokenRenderer.render(
				sortFunction.apply(rewriteInformation.getSort(), this.queryInformation, rewriteInformation.getReturnedType())
						.visit(context));
	}

	/**
	 * Because the parser can find the alias of the FROM clause, there is no need to "find it" in advance.
	 *
	 * @param sort the sort specification to apply.
	 * @param alias IGNORED
	 * @return
	 */
	@Override
	public String applySorting(Sort sort, @Nullable String alias) {
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
		return QueryRenderer.TokenRenderer
				.render(countQueryFunction.apply(countProjection, this.queryInformation).visit(context));
	}

	/**
	 * Functional interface to rewrite a query considering {@link Sort} and {@link ReturnedType}. The function returns a
	 * visitor object that can visit the parsed query tree.
	 *
	 * @since 3.5
	 */
	@FunctionalInterface
	interface SortedQueryRewriteFunction<Q> {

		ParseTreeVisitor<QueryTokenStream> apply(Sort sort, Q queryInformation, @Nullable ReturnedType returnedType);

	}

	/**
	 * Implements the {@code HQL} parsing operations of a {@link JpaQueryEnhancer} using the ANTLR-generated
	 * {@link HqlParser} and {@link HqlSortedQueryTransformer}.
	 *
	 * @author Greg Turnquist
	 * @author Mark Paluch
	 * @since 3.1
	 */
	static class HqlQueryParser extends JpaQueryEnhancer<HibernateQueryInformation> {

		private HqlQueryParser(String query) {
			super(parse(query, HqlLexer::new, HqlParser::new, HqlParser::start), new HqlQueryIntrospector(),
					HqlSortedQueryTransformer::new, HqlCountQueryTransformer::new);
		}

		/**
		 * Parse a HQL query.
		 *
		 * @param query the query to parse.
		 * @return the query parser.
		 * @throws BadJpqlGrammarException in case of malformed query.
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
	static class EqlQueryParser extends JpaQueryEnhancer<QueryInformation> {

		private EqlQueryParser(String query) {
			super(parse(query, EqlLexer::new, EqlParser::new, EqlParser::start), new EqlQueryIntrospector(),
					EqlSortedQueryTransformer::new, EqlCountQueryTransformer::new);
		}

		/**
		 * Parse a EQL query.
		 *
		 * @param query the query to parse.
		 * @return the query parser.
		 * @throws BadJpqlGrammarException in case of malformed query.
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
	static class JpqlQueryParser extends JpaQueryEnhancer<QueryInformation> {

		private JpqlQueryParser(String query) {
			super(parse(query, JpqlLexer::new, JpqlParser::new, JpqlParser::start), new JpqlQueryIntrospector(),
					JpqlSortedQueryTransformer::new, JpqlCountQueryTransformer::new);
		}

		/**
		 * Parse a JPQL query.
		 *
		 * @param query the query to parse.
		 * @return the query parser.
		 * @throws BadJpqlGrammarException in case of malformed query.
		 */
		public static JpqlQueryParser parseQuery(String query) throws BadJpqlGrammarException {
			return new JpqlQueryParser(query);
		}
	}

}
