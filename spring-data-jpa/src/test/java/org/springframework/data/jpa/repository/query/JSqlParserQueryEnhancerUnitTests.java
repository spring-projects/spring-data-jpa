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
import static org.assertj.core.api.Assumptions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Sort;

/**
 * TCK Tests for {@link JSqlParserQueryEnhancer}.
 *
 * @author Mark Paluch
 * @author Diego Krupitza
 * @author Geoffrey Deremetz
 */
public class JSqlParserQueryEnhancerUnitTests extends QueryEnhancerTckTests {

	@Override
	QueryEnhancer createQueryEnhancer(DeclaredQuery declaredQuery) {
		return new JSqlParserQueryEnhancer(declaredQuery);
	}

	@Override
	@ParameterizedTest // GH-2773
	@MethodSource("jpqlCountQueries")
	void shouldDeriveJpqlCountQuery(String query, String expected) {

		assumeThat(query).as("JSQLParser does not support simple JPQL syntax").doesNotStartWithIgnoringCase("FROM");

		assumeThat(query).as("JSQLParser does not support constructor JPQL syntax").doesNotContain(" new ");

		super.shouldDeriveJpqlCountQuery(query, expected);
	}

	@Test
	// GH-2578
	void setOperationListWorks() {

		String setQuery = "select SOME_COLUMN from SOME_TABLE where REPORTING_DATE = :REPORTING_DATE  \n" //
				+ "except \n" //
				+ "select SOME_COLUMN from SOME_OTHER_TABLE where REPORTING_DATE = :REPORTING_DATE";

		StringQuery stringQuery = new StringQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

		assertThat(stringQuery.getAlias()).isNullOrEmpty();
		assertThat(stringQuery.getProjection()).isEqualToIgnoringCase("SOME_COLUMN");
		assertThat(stringQuery.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor()).isEqualToIgnoringCase(setQuery);
		assertThat(queryEnhancer.applySorting(Sort.by("SOME_COLUMN"))).endsWith("ORDER BY SOME_COLUMN ASC");
		assertThat(queryEnhancer.getJoinAliases()).isEmpty();
		assertThat(queryEnhancer.detectAlias()).isNullOrEmpty();
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("SOME_COLUMN");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-2578
	void complexSetOperationListWorks() {

		String setQuery = "select SOME_COLUMN from SOME_TABLE where REPORTING_DATE = :REPORTING_DATE  \n" //
				+ "except \n" //
				+ "select SOME_COLUMN from SOME_OTHER_TABLE where REPORTING_DATE = :REPORTING_DATE \n" //
				+ "union select SOME_COLUMN from SOME_OTHER_OTHER_TABLE";

		StringQuery stringQuery = new StringQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

		assertThat(stringQuery.getAlias()).isNullOrEmpty();
		assertThat(stringQuery.getProjection()).isEqualToIgnoringCase("SOME_COLUMN");
		assertThat(stringQuery.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor()).isEqualToIgnoringCase(setQuery);
		assertThat(queryEnhancer.applySorting(Sort.by("SOME_COLUMN").ascending())).endsWith("ORDER BY SOME_COLUMN ASC");
		assertThat(queryEnhancer.getJoinAliases()).isEmpty();
		assertThat(queryEnhancer.detectAlias()).isNullOrEmpty();
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("SOME_COLUMN");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-2578
	void deeplyNestedcomplexSetOperationListWorks() {

		String setQuery = "SELECT CustomerID FROM (\n" //
				+ "\t\t\tselect * from Customers\n" //
				+ "\t\t\texcept\n"//
				+ "\t\t\tselect * from Customers where country = 'Austria'\n"//
				+ "\t)\n" //
				+ "\texcept\n"//
				+ "\tselect CustomerID  from customers where country = 'Germany'\n"//
				+ "\t;";

		StringQuery stringQuery = new StringQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

		assertThat(stringQuery.getAlias()).isNullOrEmpty();
		assertThat(stringQuery.getProjection()).isEqualToIgnoringCase("CustomerID");
		assertThat(stringQuery.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor()).isEqualToIgnoringCase(setQuery);
		assertThat(queryEnhancer.applySorting(Sort.by("CustomerID").descending())).endsWith("ORDER BY CustomerID DESC");
		assertThat(queryEnhancer.getJoinAliases()).isEmpty();
		assertThat(queryEnhancer.detectAlias()).isNullOrEmpty();
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("CustomerID");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-2578
	void valuesStatementsWorks() {

		String setQuery = "VALUES (1, 2, 'test')";

		StringQuery stringQuery = new StringQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

		assertThat(stringQuery.getAlias()).isNullOrEmpty();
		assertThat(stringQuery.getProjection()).isNullOrEmpty();
		assertThat(stringQuery.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor()).isEqualToIgnoringCase(setQuery);
		assertThat(queryEnhancer.applySorting(Sort.by("CustomerID").descending())).isEqualTo(setQuery);
		assertThat(queryEnhancer.getJoinAliases()).isEmpty();
		assertThat(queryEnhancer.detectAlias()).isNullOrEmpty();
		assertThat(queryEnhancer.getProjection()).isNullOrEmpty();
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-2578
	void withStatementsWorks() {

		String setQuery = "with sample_data(day, value) as (values ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))) \n"
				+ "select day, value from sample_data as a";

		StringQuery stringQuery = new StringQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

		assertThat(stringQuery.getAlias()).isEqualToIgnoringCase("a");
		assertThat(stringQuery.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(stringQuery.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor()).isEqualToIgnoringCase(
				"with sample_data (day, value) AS (VALUES ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16)))\n"
						+ "SELECT count(1) FROM sample_data AS a");
		assertThat(queryEnhancer.applySorting(Sort.by("day").descending())).endsWith("ORDER BY a.day DESC");
		assertThat(queryEnhancer.getJoinAliases()).isEmpty();
		assertThat(queryEnhancer.detectAlias()).isEqualToIgnoringCase("a");
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-2578
	void multipleWithStatementsWorks() {

		String setQuery = "with sample_data(day, value) as (values ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))), test2 as (values (1,2,3)) \n"
				+ "select day, value from sample_data as a";

		StringQuery stringQuery = new StringQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

		assertThat(stringQuery.getAlias()).isEqualToIgnoringCase("a");
		assertThat(stringQuery.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(stringQuery.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor()).isEqualToIgnoringCase(
				"with sample_data (day, value) AS (VALUES ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))),test2 AS (VALUES (1, 2, 3))\n"
						+ "SELECT count(1) FROM sample_data AS a");
		assertThat(queryEnhancer.applySorting(Sort.by("day").descending())).endsWith("ORDER BY a.day DESC");
		assertThat(queryEnhancer.getJoinAliases()).isEmpty();
		assertThat(queryEnhancer.detectAlias()).isEqualToIgnoringCase("a");
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@ParameterizedTest // GH-2641
	@MethodSource("mergeStatementWorksSource")
	void mergeStatementWorksWithJSqlParser(String query, String alias) {

		StringQuery stringQuery = new StringQuery(query, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

		assertThat(queryEnhancer.detectAlias()).isEqualTo(alias);
		assertThat(QueryUtils.detectAlias(query)).isNull();

		assertThat(queryEnhancer.getJoinAliases()).isEmpty();
		assertThat(queryEnhancer.detectAlias()).isEqualTo(alias);
		assertThat(queryEnhancer.getProjection()).isEmpty();
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	static Stream<Arguments> mergeStatementWorksSource() {

		return Stream.of( //
				Arguments.of(
						"merge into a using (select id, value from b) query on (a.id = query.id) when matched then update set a.value = value",
						"query"),
				Arguments.of(
						"merge into a using (select id2, value from b) on (id = id2) when matched then update set a.value = value",
						null));
	}
}
