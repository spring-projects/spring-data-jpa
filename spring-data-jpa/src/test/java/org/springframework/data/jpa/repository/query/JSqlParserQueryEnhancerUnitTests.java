/*
 * Copyright 2023-2025 the original author or authors.
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
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.query.ReturnedType;

/**
 * TCK Tests for {@link JSqlParserQueryEnhancer}.
 *
 * @author Mark Paluch
 * @author Diego Krupitza
 * @author Geoffrey Deremetz
 * @author Christoph Strobl
 */
public class JSqlParserQueryEnhancerUnitTests extends QueryEnhancerTckTests {

	@Override
	QueryEnhancer createQueryEnhancer(DeclaredQuery query) {
		return new JSqlParserQueryEnhancer(query);
	}

	@Test // GH-3546
	void shouldApplySorting() {

		QueryEnhancer enhancer = createQueryEnhancer(DeclaredQuery.jpqlQuery("SELECT e FROM Employee e"));

		String sql = enhancer.rewrite(new DefaultQueryRewriteInformation(Sort.by("foo", "bar"),
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory())));

		assertThat(sql).isEqualTo("SELECT e FROM Employee e ORDER BY e.foo ASC, e.bar ASC");
	}

	@Test // GH-3707
	void countQueriesShouldConsiderPrimaryTableAlias() {

		QueryEnhancer enhancer = createQueryEnhancer(DeclaredQuery.nativeQuery("""
				SELECT DISTINCT a.*, b.b1
				FROM TableA a
				  JOIN TableB b ON a.b = b.b
				  LEFT JOIN TableC c ON b.c = c.c
				ORDER BY b.b1, a.a1, a.a2
				"""));

		String sql = enhancer.createCountQueryFor(null);

		assertThat(sql).startsWith("SELECT count(DISTINCT a.*) FROM TableA a");
	}

	@Override
	@ParameterizedTest // GH-2773
	@MethodSource("jpqlCountQueries")
	void shouldDeriveJpqlCountQuery(String query, String expected) {
		assumeThat(query).as("JSQLParser does not support JPQL").isNull();
	}

	@Test
	// GH-2578
	void setOperationListWorks() {

		String setQuery = "select SOME_COLUMN from SOME_TABLE where REPORTING_DATE = :REPORTING_DATE  \n" //
				+ "except \n" //
				+ "select SOME_COLUMN from SOME_OTHER_TABLE where REPORTING_DATE = :REPORTING_DATE";

		DefaultEntityQuery query = new TestEntityQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancer.create(query);

		assertThat(query.getAlias()).isNullOrEmpty();
		assertThat(query.getProjection()).isEqualToIgnoringCase("SOME_COLUMN");
		assertThat(query.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor(null)).isEqualToIgnoringCase(setQuery);
		assertThat(queryEnhancer.rewrite(getRewriteInformation(Sort.by("SOME_COLUMN"))))
				.endsWith("ORDER BY SOME_COLUMN ASC");
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

		DefaultEntityQuery query = new TestEntityQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query).create(query);

		assertThat(query.getAlias()).isNullOrEmpty();
		assertThat(query.getProjection()).isEqualToIgnoringCase("SOME_COLUMN");
		assertThat(query.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor(null)).isEqualToIgnoringCase(setQuery);
		assertThat(queryEnhancer.rewrite(getRewriteInformation(Sort.by("SOME_COLUMN").ascending())))
				.endsWith("ORDER BY SOME_COLUMN ASC");
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

		DefaultEntityQuery query = new TestEntityQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query).create(query);

		assertThat(query.getAlias()).isNullOrEmpty();
		assertThat(query.getProjection()).isEqualToIgnoringCase("CustomerID");
		assertThat(query.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor(null)).isEqualToIgnoringCase(setQuery);
		assertThat(queryEnhancer.rewrite(getRewriteInformation(Sort.by("CustomerID").descending())))
				.endsWith("ORDER BY CustomerID DESC");
		assertThat(queryEnhancer.detectAlias()).isNullOrEmpty();
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("CustomerID");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-2578
	void valuesStatementsWorks() {

		String setQuery = "VALUES (1, 2, 'test')";

		DefaultEntityQuery query = new TestEntityQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query).create(query);

		assertThat(query.getAlias()).isNullOrEmpty();
		assertThat(query.getProjection()).isNullOrEmpty();
		assertThat(query.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor(null)).isEqualToIgnoringCase(setQuery);
		assertThat(queryEnhancer.rewrite(getRewriteInformation(Sort.by("CustomerID").descending()))).isEqualTo(setQuery);
		assertThat(queryEnhancer.detectAlias()).isNullOrEmpty();
		assertThat(queryEnhancer.getProjection()).isNullOrEmpty();
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-2578
	void withStatementsWorks() {

		String setQuery = "with sample_data(day, value) as (values ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))) \n"
				+ "select day, value from sample_data as a";

		DefaultEntityQuery query = new TestEntityQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query).create(query);

		assertThat(query.getAlias()).isEqualToIgnoringCase("a");
		assertThat(query.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(query.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor(null)).isEqualToIgnoringCase(
				"with sample_data (day, value) AS (VALUES ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))) "
						+ "SELECT count(1) FROM sample_data AS a");
		assertThat(queryEnhancer.rewrite(getRewriteInformation(Sort.by("day").descending())))
				.endsWith("ORDER BY a.day DESC");
		assertThat(queryEnhancer.detectAlias()).isEqualToIgnoringCase("a");
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-2578
	void multipleWithStatementsWorks() {

		String setQuery = "with sample_data(day, value) as (values ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))), test2 as (values (1,2,3)) \n"
				+ "select day, value from sample_data as a";

		DefaultEntityQuery query = new TestEntityQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query).create(query);

		assertThat(query.getAlias()).isEqualToIgnoringCase("a");
		assertThat(query.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(query.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor(null)).isEqualToIgnoringCase(
				"with sample_data (day, value) AS (VALUES ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))), test2 AS (VALUES (1, 2, 3)) "
						+ "SELECT count(1) FROM sample_data AS a");
		assertThat(queryEnhancer.rewrite(getRewriteInformation(Sort.by("day").descending())))
				.endsWith("ORDER BY a.day DESC");
		assertThat(queryEnhancer.detectAlias()).isEqualToIgnoringCase("a");
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-3038
	void truncateStatementShouldWork() {

		DefaultEntityQuery query = new TestEntityQuery("TRUNCATE TABLE foo", true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query).create(query);

		assertThat(query.getAlias()).isNull();
		assertThat(query.getProjection()).isEmpty();
		assertThat(query.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.rewrite(getRewriteInformation(Sort.by("day").descending())))
				.isEqualTo("TRUNCATE TABLE foo");
		assertThat(queryEnhancer.detectAlias()).isNull();
		assertThat(queryEnhancer.getProjection()).isEmpty();
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@ParameterizedTest // GH-2641
	@MethodSource("mergeStatementWorksSource")
	void mergeStatementWorksWithJSqlParser(String queryString, String alias) {

		DefaultEntityQuery query = new TestEntityQuery(queryString, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query).create(query);

		assertThat(queryEnhancer.detectAlias()).isEqualTo(alias);
		assertThat(QueryUtils.detectAlias(queryString)).isNull();

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

	private static DefaultQueryRewriteInformation getRewriteInformation(Sort sort) {
		return new DefaultQueryRewriteInformation(sort,
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory()));
	}

}
