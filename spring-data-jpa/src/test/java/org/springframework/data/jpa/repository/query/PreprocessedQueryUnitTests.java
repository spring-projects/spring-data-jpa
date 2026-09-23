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

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link PreprocessedQuery}.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @author Mark Paluch
 * @author Nabil Fawwaz Elqayyim
 */
class PreprocessedQueryUnitTests {

	@ParameterizedTest // DATAJPA-1200
	@MethodSource("parameters")
	void identificationOfParameters(String jpql, boolean containsParameter) {

		DefaultEntityQuery stringQuery = new TestEntityQuery(jpql, false);

		if (containsParameter) {
			assertThat(stringQuery.getParameterBindings()).isNotEmpty();
		} else {
			assertThat(stringQuery.getParameterBindings()).isEmpty();
		}
	}

	static Stream<Arguments.ArgumentSet> parameters() {

		return Stream.of(Arguments.argumentSet("named parameter", "select u from User u where u.name = :name", true),
				Arguments.argumentSet("indexed parameter", "select u from User u where u.age > ?1", true),
				Arguments.argumentSet("no parameter", "select u from User u where u.email = u.email", false));
	}

	@ParameterizedTest // GH-4090
	@MethodSource("commentParameters")
	void shouldIgnoreMarkersInComments(String queryText, int expectedBindingCount, String description) {

		// Use the official static factory method from the DeclaredQuery interface
		DeclaredQuery declaredQuery = DeclaredQuery.jpqlQuery(queryText);
		PreprocessedQuery preprocessed = PreprocessedQuery.parse(declaredQuery);

		assertThat(preprocessed.getBindings())
				.as(description)
				.hasSize(expectedBindingCount);
	}

	static Stream<Arguments> commentParameters() {
		return Stream.of(
				// 1. Block Comment Scenarios
				Arguments.of("SELECT e FROM Entity e /* block ? */ WHERE e.id = :id", 1, "Standard block comment"),
				Arguments.of("SELECT e FROM Entity e /* asterisk * inside */ WHERE e.id = :id", 1, "Asterisk inside block comment"),

				// 2. SQL-style Line Comment Scenarios
				Arguments.of("SELECT e FROM Entity e -- line comment \n WHERE e.id = :id", 1, "SQL-style line comment with newline"),

				// 3. Java-style Line Comment Scenarios
				// This specific case turns the remaining yellow branches green.
				Arguments.of("SELECT e FROM Entity e // java comment \r WHERE e.id = :id", 1, "Java-style line comment with carriage return"),

				// 4. Mathematical Operators
				// Ensures '/' and '-' are not always treated as comment starts.
				Arguments.of("SELECT e FROM Entity e WHERE e.id / 2 = 1 AND e.id * 2 = :id", 1, "Slash and asterisk as operators"),
				Arguments.of("SELECT e FROM Entity e WHERE e.id - 1 = :id", 1, "Hyphen as subtraction operator"),

				// 5. String Literal and Escaping Scenarios
				// Confirms that markers inside quotes are ignored and escaped quotes are handled.
				Arguments.of("SELECT e FROM Entity e WHERE e.name = 'It''s a -- marker' AND e.id = :id", 1, "Markers inside string literals")
		);
	}
}
