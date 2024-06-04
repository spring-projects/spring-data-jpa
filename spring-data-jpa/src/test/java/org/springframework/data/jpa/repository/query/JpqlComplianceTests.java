/*
 * Copyright 2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.repository.query.JpaQueryParsingToken.*;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.query.QueryRenderer.TokenRenderer;

/**
 * Test to verify compliance of {@link JpqlParser} with standard SQL. Other than {@link JpqlSpecificationTests} tests in
 * this class check that the parser follows a lenient approach and does not error on well known concepts like numeric
 * suffix.
 * 
 * @author Christoph Strobl
 */
class JpqlComplianceTests {

	private static String parseWithoutChanges(String query) {

		JpqlLexer lexer = new JpqlLexer(CharStreams.fromString(query));
		JpqlParser parser = new JpqlParser(new CommonTokenStream(lexer));

		parser.addErrorListener(new BadJpqlGrammarErrorListener(query));

		JpqlParser.StartContext parsedQuery = parser.start();

		return TokenRenderer.render(new JpqlQueryRenderer().visit(parsedQuery));
	}

	private void assertQuery(String query) {

		String slimmedDownQuery = reduceWhitespace(query);
		assertThat(parseWithoutChanges(slimmedDownQuery)).isEqualTo(slimmedDownQuery);
	}

	private String reduceWhitespace(String original) {

		return original //
				.replaceAll("[ \\t\\n]{1,}", " ") //
				.trim();
	}

	@Test // GH-3277
	void numericLiterals() {

		assertQuery("SELECT e FROM  Employee e WHERE e.id = 1234");
		assertQuery("SELECT e FROM  Employee e WHERE e.id = 1234L");
		assertQuery("SELECT s FROM  Stat s WHERE s.ratio > 3.14");
		assertQuery("SELECT s FROM  Stat s WHERE s.ratio > 3.14F");
		assertQuery("SELECT s FROM  Stat s WHERE s.ratio > 3.14e32D");
	}

	@Test // GH-3308
	void newWithStrings() {
		assertQuery("select new com.example.demo.SampleObject(se.id, se.sampleValue, \"java\") from SampleEntity se");
	}

}
