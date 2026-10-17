/*
 * Copyright 2025-present the original author or authors.
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

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link BadJpqlGrammarException}.
 *
 * @author Mark Paluch
 */
@ParameterizedClass(name = "{0}")
@MethodSource("parsers")
class BadJpqlGrammarExceptionUnitTests {

	private final String grammar;
	private final Consumer<String> parseQuery;

	BadJpqlGrammarExceptionUnitTests(String grammar, Consumer<String> parseQuery) {
		this.grammar = grammar;
		this.parseQuery = parseQuery;
	}

	static Stream<Arguments> parsers() {
		return Stream.of(Arguments.of("HQL", (Consumer<String>) JpaQueryEnhancer.HqlQueryParser::parseQuery),
				Arguments.of("EQL", (Consumer<String>) JpaQueryEnhancer.EqlQueryParser::parseQuery),
				Arguments.of("JPQL", (Consumer<String>) JpaQueryEnhancer.JpqlQueryParser::parseQuery));
	}

	@Test // GH-3757
	void shouldContainOriginalText() {

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> parseQuery.accept("SELECT e FROM Employee e WHERE FUNCTION('foo', x + )"))
				.withMessageContaining("no viable alternative")
				.withMessageContaining("SELECT e FROM Employee e WHERE FUNCTION('foo', x + *)")
				.withMessageContaining("Bad " + grammar + " grammar [SELECT e FROM Employee e WHERE FUNCTION('foo', x + )]");
	}

	@Test // GH-4326
	void shouldReportMismatchedFunctionClause() {

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> parseQuery.accept("SELECT e FROM Employee e WHERE FUNCTION('foo', x) = 1 RESPECTING NULLS"))
				.withMessageContaining("mismatched input 'RESPECTING'").withMessageContaining(
						"Bad " + grammar + " grammar [SELECT e FROM Employee e WHERE FUNCTION('foo', x) = 1 RESPECTING NULLS]");
	}

	@Test // GH-3757
	void shouldReportExtraneousInput() {

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> parseQuery.accept("select * from User group by name"))
				.withMessageContaining("extraneous input '*'")
				.withMessageContaining("Bad " + grammar + " grammar [select * from User group by name]");
	}

	@Test // GH-3757
	void shouldReportMismatchedInput() {

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> parseQuery.accept("SELECT AVG(m.price) AS m.avg FROM Magazine m"))
				.withMessageContaining("mismatched input '.'").withMessageContaining("expecting one of the following tokens:")
				.withMessageContaining("FROM")
				.withMessageContaining("Bad " + grammar + " grammar [SELECT AVG(m.price) AS m.avg FROM Magazine m]");
	}

}
