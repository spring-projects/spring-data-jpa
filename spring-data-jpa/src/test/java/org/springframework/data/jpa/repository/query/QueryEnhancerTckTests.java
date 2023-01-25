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

import org.junit.jupiter.api.Test;
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
		String countQueryFor = enhancer.createCountQueryFor();

		// lenient cleanup to allow for rendering variance
		String sanitized = countQueryFor.replaceAll("\r", " ").replaceAll("\n", " ").replaceAll(" {2}", " ")
				.replaceAll(" {2}", " ").trim();

		assertThat(sanitized).isEqualToIgnoringCase(expected);
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
						"select count(DISTINCT name) FROM table_name some_alias"), //

				Arguments.of( //
						"select distinct u from User u where u.foo = ?", //
						"select count(distinct u) from User u where u.foo = ?"),

				Arguments.of( //
						"select u from User as u", //
						"select count(u) from User as u"),

				Arguments.of( //
						"SELECT u FROM User u where u.foo.bar = ?", //
						"select count(u) FROM User u where u.foo.bar = ?"),

				Arguments.of( //
						"select p.lastname,p.firstname from Person p", //
						"select count(1) from Person p"),

				// whitespace quirks
				Arguments.of( //
						"""
								select user.age,
								  user.name
								  from User user
								  where user.age = 18
								  order
								by
								user.name
								\s""", //
						"select count(1) from User user where user.age = 18"),

				Arguments.of( //
						"select * from User user\n" + //
								"  where user.age = 18\n" + //
								"  order by user.name\n ", //
						"select count(1) from User user where user.age = 18"),

				Arguments.of( //
						"SELECT DISTINCT entity1\nFROM Entity1 entity1\nLEFT JOIN Entity2 entity2 ON entity1.key = entity2.key", //
						"select count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key"),

				Arguments.of( //
						"SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key", //
						"select count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key"),

				Arguments.of( //
						"SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key\nwhere entity1.id = 1799", //
						"select count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key where entity1.id = 1799"),

				Arguments.of( //
						"select distinct m.genre from Media m where m.user = ?1 OrDer  By   m.genre ASC", //
						"select count(distinct m.genre) from Media m where m.user = ?1"));
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
						"select count(DISTINCT name) FROM table_name some_alias"),

				Arguments.of( //
						"select distinct new User(u.name) from User u where u.foo = ?", //
						"select count(distinct u) from User u where u.foo = ?"),

				Arguments.of( //
						"FROM User u WHERE u.foo.bar = ?", //
						"select count(u) FROM User u WHERE u.foo.bar = ?"),

				Arguments.of( //
						"from User u", //
						"select count(u) FROM User u"),

				Arguments.of( //
						"select u from User as u", //
						"select count(u) from User as u"),

				Arguments.of( //
						"select p.lastname,p.firstname from Person p", //
						"select count(p) from Person p"),

				Arguments.of( //
						"select a.b from A a", //
						"select count(a.b) from A a"),

				Arguments.of( //
						"select distinct m.genre from Media m where m.user = ?1 order by m.genre asc", //
						"select count(distinct m.genre) from Media m where m.user = ?1"));
	}

	@ParameterizedTest // GH-2511, GH-2773
	@MethodSource("nativeQueriesWithVariables")
	void shouldDeriveNativeCountQueryWithVariable(String query, String expected) {

		DeclaredQuery declaredQuery = DeclaredQuery.of(query, true);
		QueryEnhancer enhancer = createQueryEnhancer(declaredQuery);
		String countQueryFor = enhancer.createCountQueryFor();

		assertThat(countQueryFor).isEqualToIgnoringCase(expected);
	}

	static Stream<Arguments> nativeQueriesWithVariables() {

		return Stream.of(Arguments.of( //
				"SELECT * FROM User WHERE created_at > $1", //
				"SELECT count(1) FROM User WHERE created_at > $1"), //

				Arguments.of( //
						"SELECT * FROM (select * from test) ", //
						"SELECT count(1) FROM (SELECT * FROM test)"), //

				Arguments.of( //
						"SELECT * FROM (select * from test) as test", //
						"SELECT count(1) FROM (SELECT * FROM test) AS test"));
	}

	@Test
	// DATAJPA-1696
	void findProjectionClauseWithIncludedFrom() {

		StringQuery query = new StringQuery("select x, frommage, y from t", true);

		assertThat(createQueryEnhancer(query).getProjection()).isEqualTo("x, frommage, y");
	}

	abstract QueryEnhancer createQueryEnhancer(DeclaredQuery declaredQuery);

}
