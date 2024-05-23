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

/**
 * Implements the {@code JPQL} parsing operations of a {@link JpaQueryParser} using the ANTLR-generated
 * {@link JpqlParser} and {@link JpqlSortedQueryTransformer}.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.1
 */
class JpqlQueryParser extends JpaQueryParser {

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
