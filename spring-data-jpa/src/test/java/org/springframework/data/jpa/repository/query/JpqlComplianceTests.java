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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

		return QueryRenderer.render(new JpqlQueryRenderer().visit(parsedQuery));
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

		assertQuery("SELECT e FROM Employee e WHERE e.id = 1234");
		assertQuery("SELECT e FROM Employee e WHERE e.id = 1234L");
		assertQuery("SELECT s FROM Stat s WHERE s.ratio > 3.14");
		assertQuery("SELECT s FROM Stat s WHERE s.ratio > 3.14F");
		assertQuery("SELECT s FROM Stat s WHERE s.ratio > 3.14e32D");
	}

	@Test // GH-3308
	void newWithStrings() {
		assertQuery("select new com.example.demo.SampleObject(se.id, se.sampleValue, \"java\") from SampleEntity se");
	}

	@Test // GH-3136
	void union() {

		assertQuery("""
				SELECT MAX(e.salary) FROM Employee e WHERE e.address.city = :city1
				UNION SELECT MAX(e.salary) FROM Employee e WHERE e.address.city = :city2
				""");
	}

	@Test // GH-3136
	void intersect() {

		assertQuery("""
				SELECT e FROM Employee e JOIN e.phones p WHERE p.areaCode = :areaCode1
				INTERSECT SELECT e FROM Employee e JOIN e.phones p WHERE p.areaCode = :areaCode2
				""");
	}

	@Test // GH-3136
	void except() {

		assertQuery("""
				SELECT e FROM Employee e
				EXCEPT SELECT e FROM Employee e WHERE e.salary > e.manager.salary
				""");
	}

	@ParameterizedTest // GH-3136
	@ValueSource(strings = {"STRING", "INTEGER", "FLOAT", "DOUBLE"})
	void cast(String targetType) {
		assertQuery("SELECT CAST(e.salary AS %s) FROM Employee e".formatted(targetType));
	}

	@ParameterizedTest // GH-3136
	@ValueSource(strings = {"LEFT", "RIGHT"})
	void leftRightStringFunctions(String keyword) {
		assertQuery("SELECT %s(e.name, 3) FROM Employee e".formatted(keyword));
	}

	@Test // GH-3136
	void replaceStringFunctions() {
		assertQuery("SELECT REPLACE(e.name, 'o', 'a') FROM Employee e");
		assertQuery("SELECT REPLACE(e.name, ' ', '_') FROM Employee e");
	}

}
