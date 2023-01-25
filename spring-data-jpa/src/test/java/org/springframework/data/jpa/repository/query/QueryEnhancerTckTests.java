/*
 * Copyright 2023 the original author or authors.
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
 * TCK Tests for {@link QueryEnhancer}.
 *
 * @author Mark Paluch
 */
abstract class QueryEnhancerTckTests {

	@ParameterizedTest
	@MethodSource("nativeCountQueries") // GH-2773
	void shouldDeriveNativeCountQuery(String query, String expected) {

		DeclaredQuery declaredQuery = DeclaredQuery.of(query, true);
		QueryEnhancer enhancer = createQueryEnhancer(declaredQuery);
		String countQueryFor = enhancer.createCountQueryFor(null);

		assertThat(countQueryFor).isEqualToIgnoringCase(expected);
	}

	static Stream<Arguments> nativeCountQueries() {

		return Stream.of(Arguments.of( //
				"SELECT * FROM table_name some_alias", //
				"select count(1) FROM table_name some_alias"), //

				Arguments.of( //
						"SELECT name FROM table_name some_alias", //
						"select count(name) FROM table_name some_alias"), //

				Arguments.of( //
						"SELECT DISTINCT name FROM table_name some_alias", //
						"select count(DISTINCT name) FROM table_name some_alias"));
	}

	@ParameterizedTest // GH-2773
	@MethodSource("jpqlCountQueries")
	void shouldDeriveJpqlCountQuery(String query, String expected) {

		DeclaredQuery declaredQuery = DeclaredQuery.of(query, false);
		QueryEnhancer enhancer = createQueryEnhancer(declaredQuery);
		String countQueryFor = enhancer.createCountQueryFor(null);

		assertThat(countQueryFor).isEqualToIgnoringCase(expected);
	}

	static Stream<Arguments> jpqlCountQueries() {

		return Stream.of(Arguments.of( //
				"SELECT some_alias FROM table_name some_alias", //
				"select count(some_alias) FROM table_name some_alias"), //

				Arguments.of( //
						"SELECT name FROM table_name some_alias", //
						"select count(name) FROM table_name some_alias"), //

				Arguments.of( //
						"SELECT DISTINCT name FROM table_name some_alias", //
						"select count(DISTINCT name) FROM table_name some_alias"));
	}

	abstract QueryEnhancer createQueryEnhancer(DeclaredQuery declaredQuery);

}
