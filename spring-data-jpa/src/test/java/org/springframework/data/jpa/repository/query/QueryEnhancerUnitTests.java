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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.query.ReturnedType;

/**
 * Unit tests for {@link QueryEnhancer}.
 *
 * @author Diego Krupitza
 * @author Geoffrey Deremetz
 * @author Krzysztof Krason
 * @author Mark Paluch
 */
class QueryEnhancerUnitTests {

	private static final String QUERY = "select u from User u";
	private static final String FQ_QUERY = "select u from org.acme.domain.User$Foo_Bar u";
	private static final String SIMPLE_QUERY = "from User u";
	private static final String COUNT_QUERY = "select count(u) from User u";
	private static final String QUERY_WITH_AS = "select u from User as u where u.username = ?";

	@Test
	void createsCountQueryForJoinsNoneNative() {

		assertCountQuery("select distinct new com.example.User(u.name) from User u left outer join u.roles r WHERE r = ?1",
				"select count(distinct u) from User u left outer join u.roles r WHERE r = ?1", false);
	}

	@Test
	void createsCountQueryForJoinsNative() {

		assertCountQuery("select distinct u.name from User u left outer join u.roles r WHERE r = ?",
				"select count(distinct u.name) from User u left outer join u.roles r WHERE r = ?", true);
	}

	@Test
	void createsCountQueryForQueriesWithSubSelects() {

		assertCountQuery("select u from User u left outer join u.roles r where r in (select r from Role)",
				"select count(u) from User u left outer join u.roles r where r in (select r from Role)", true);
	}

	@Disabled("JPQL doesn't support short JPA syntax.")
	@Test
	void allowsShortJpaSyntax() {
		assertCountQuery(SIMPLE_QUERY, COUNT_QUERY, false);
	}

	@ParameterizedTest
	@MethodSource("detectsAliasWithUCorrectlySource")
	void detectsAliasWithUCorrectly(DefaultEntityQuery query, String alias) {

		assumeThat(query.getQueryString()).as("JsqlParser does not support simple JPA syntax")
				.doesNotStartWithIgnoringCase("from");

		assertThat(getEnhancer(query).detectAlias()).isEqualTo(alias);
	}

	public static Stream<Arguments> detectsAliasWithUCorrectlySource() {

		return Stream.of( //
				Arguments.of(new TestEntityQuery(QUERY, true), "u"), //
				Arguments.of(new TestEntityQuery(SIMPLE_QUERY, false), "u"), //
				Arguments.of(new TestEntityQuery(COUNT_QUERY, true), "u"), //
				Arguments.of(new TestEntityQuery(QUERY_WITH_AS, true), "u"), //
				Arguments.of(new TestEntityQuery("SELECT u FROM USER U", false), "U"), //
				Arguments.of(new TestEntityQuery("select u from  User u", true), "u"), //
				Arguments.of(new TestEntityQuery("select u from  com.acme.User u", true), "u"), //
				Arguments.of(new TestEntityQuery("select u from T05User u", true), "u") //
		);
	}

	@Test
	void allowsFullyQualifiedEntityNamesInQuery() {

		DefaultEntityQuery query = new TestEntityQuery(FQ_QUERY, true);

		assertThat(getEnhancer(query).detectAlias()).isEqualTo("u");
		assertCountQuery(FQ_QUERY, "select count(u) from org.acme.domain.User$Foo_Bar u", true);
	}

	@Test // DATAJPA-252
	void doesNotPrefixOrderReferenceIfOuterJoinAliasDetected() {

		DefaultEntityQuery query = new TestEntityQuery("select p from Person p left join p.address address", true);

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(Sort.by("address.city"))))
				.endsWithIgnoringCase("order by address.city asc");
	}

	@Test // DATAJPA-252
	void extendsExistingOrderByClausesCorrectly() {

		DefaultEntityQuery query = new TestEntityQuery("select p from Person p order by p.lastname asc", true);

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(Sort.by("firstname"))))
				.endsWithIgnoringCase("order by p.lastname asc, p.firstname asc");
	}

	@Test // DATAJPA-296
	void appliesIgnoreCaseOrderingCorrectly() {

		Sort sort = Sort.by(Sort.Order.by("firstname").ignoreCase());

		DefaultEntityQuery query = new TestEntityQuery("select p from Person p", true);

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWithIgnoringCase("order by lower(p.firstname) asc");
	}

	@Test // DATAJPA-296
	void appendsIgnoreCaseOrderingCorrectly() {

		Sort sort = Sort.by(Sort.Order.by("firstname").ignoreCase());

		DefaultEntityQuery query = new TestEntityQuery("select p from Person p order by p.lastname asc", true);

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWithIgnoringCase("order by p.lastname asc, lower(p.firstname) asc");
	}

	@Test // DATAJPA-343
	void projectsCountQueriesForQueriesWithSubSelects() {

		assertCountQuery("select o from Foo o where cb.id in (select b from Bar b)",
				"select count(o) from Foo o where cb.id in (select b from Bar b)", true);
	}

	@Test // DATAJPA-148
	void doesNotPrefixSortsIfFunction() {

		DefaultEntityQuery query = new TestEntityQuery("select p from Person p", true);
		Sort sort = Sort.by("sum(foo)");

		QueryEnhancer enhancer = getEnhancer(query);

		assertThatThrownBy(() -> enhancer.rewrite(getRewriteInformation(sort))) //
				.isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test // DATAJPA-375
	void findsExistingOrderByIndependentOfCase() {

		Sort sort = Sort.by("lastname");
		DefaultEntityQuery originalQuery = new TestEntityQuery("select p from Person p ORDER BY p.firstname", true);
		String query = getEnhancer(originalQuery).rewrite(getRewriteInformation(sort));

		assertThat(query).endsWithIgnoringCase("ORDER BY p.firstname, p.lastname asc");
	}

	@Test // GH-3263
	void preserveSourceQueryWhenAddingSort() {

		DefaultEntityQuery query = new TestEntityQuery(
				"WITH all_projects AS (SELECT * FROM projects) SELECT * FROM all_projects p", true);

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(Sort.by("name")))) //
				.startsWithIgnoringCase(query.getQueryString()).endsWithIgnoringCase("ORDER BY p.name ASC");
	}

	@Test // GH-2812
	void createCountQueryFromDeleteQuery() {

		DefaultEntityQuery query = new TestEntityQuery("delete from some_table where id in :ids", true);

		assertThat(getEnhancer(query).createCountQueryFor("p.lastname"))
				.isEqualToIgnoringCase("delete from some_table where id in :ids");
	}

	@Test // DATAJPA-456
	void createCountQueryFromTheGivenCountProjection() {

		DefaultEntityQuery query = new TestEntityQuery("select p.lastname,p.firstname from Person p", true);

		assertThat(getEnhancer(query).createCountQueryFor("p.lastname"))
				.isEqualToIgnoringCase("select count(p.lastname) from Person p");
	}

	@Test // DATAJPA-726
	void detectsAliasesInPlainJoins() {

		DefaultEntityQuery query = new TestEntityQuery(
				"select p from Customer c join c.productOrder p where p.delay = true", true);
		Sort sort = Sort.by("p.lineItems");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWithIgnoringCase("order by p.lineItems asc");
	}

	@Test // DATAJPA-736
	void supportsNonAsciiCharactersInEntityNames() {

		DefaultEntityQuery query = new TestEntityQuery("select u from Usèr u", true);

		assertThat(getEnhancer(query).createCountQueryFor(null)).isEqualToIgnoringCase("select count(u) from Usèr u");
	}

	@Test // DATAJPA-798
	void detectsAliasInQueryContainingLineBreaks() {

		DefaultEntityQuery query = new TestEntityQuery("select \n u \n from \n User \nu", true);

		assertThat(getEnhancer(query).detectAlias()).isEqualTo("u");
	}

	@Disabled("JPQL doesn't support short JPA syntax.")
	@Test // DATAJPA-815
	void doesPrefixPropertyWithNonNative() {

		DefaultEntityQuery query = new TestEntityQuery("from Cat c join Dog d", false);
		Sort sort = Sort.by("dPropertyStartingWithJoinAlias");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWith("order by c.dPropertyStartingWithJoinAlias asc");
	}

	@Test // DATAJPA-815
	void doesPrefixPropertyWithNative() {

		DefaultEntityQuery query = new TestEntityQuery("Select * from Cat c join Dog d", true);
		Sort sort = Sort.by("dPropertyStartingWithJoinAlias");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWithIgnoringCase("order by c.dPropertyStartingWithJoinAlias asc");
	}

	@Test // DATAJPA-938
	void detectsConstructorExpressionInDistinctQuery() {

		DefaultEntityQuery query = new TestEntityQuery("select distinct new com.example.Foo(b.name) from Bar b",
				false);

		assertThat(getEnhancer(query).hasConstructorExpression()).isTrue();
	}

	@Test // DATAJPA-938
	void detectsComplexConstructorExpression() {

		DefaultEntityQuery query = new TestEntityQuery("select new foo.bar.Foo(ip.id, ip.name, sum(lp.amount)) " //
				+ "from Bar lp join lp.investmentProduct ip " //
				+ "where (lp.toDate is null and lp.fromDate <= :now and lp.fromDate is not null) and lp.accountId = :accountId "
				//
				+ "group by ip.id, ip.name, lp.accountId " //
				+ "order by ip.name ASC", false);

		assertThat(getEnhancer(query).hasConstructorExpression()).isTrue();
	}

	@Test // DATAJPA-938
	void detectsConstructorExpressionWithLineBreaks() {

		DefaultEntityQuery query = new TestEntityQuery("select new foo.bar.FooBar(\na.id) from DtoA a ", false);

		assertThat(getEnhancer(query).hasConstructorExpression()).isTrue();
	}

	@Disabled("JPQL doesn't support short JPA syntax.")
	@Test // DATAJPA-960
	void doesNotQualifySortIfNoAliasDetectedNonNative() {

		DefaultEntityQuery query = new TestEntityQuery("from mytable where ?1 is null", false);

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(Sort.by("firstname"))))
				.endsWith("order by firstname asc");
	}

	@Test // DATAJPA-960
	void doesNotQualifySortIfNoAliasDetectedNative() {

		DefaultEntityQuery query = new TestEntityQuery("Select * from mytable where ?1 is null", true);

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(Sort.by("firstname"))))
				.endsWithIgnoringCase("order by firstname asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotAllowWhitespaceInSort() {

		DefaultEntityQuery query = new TestEntityQuery("select p from Person p", true);

		Sort sort = Sort.by("case when foo then bar");

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> getEnhancer(query).rewrite(getRewriteInformation(sort)));
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixUnsafeJpaSortFunctionCalls() {

		JpaSort sort = JpaSort.unsafe("sum(foo)");
		DefaultEntityQuery query = new TestEntityQuery("select p from Person p", true);

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort))).endsWithIgnoringCase("order by sum(foo) asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixMultipleAliasedFunctionCalls() {

		DefaultEntityQuery query = new TestEntityQuery(
				"SELECT AVG(m.price) AS avgPrice, SUM(m.stocks) AS sumStocks FROM Magazine m", true);
		Sort sort = Sort.by("avgPrice", "sumStocks");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWithIgnoringCase("order by avgPrice asc, sumStocks asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixSingleAliasedFunctionCalls() {

		DefaultEntityQuery query = new TestEntityQuery("SELECT AVG(m.price) AS avgPrice FROM Magazine m", true);
		Sort sort = Sort.by("avgPrice");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort))).endsWithIgnoringCase("order by avgPrice asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void prefixesSingleNonAliasedFunctionCallRelatedSortProperty() {

		DefaultEntityQuery query = new TestEntityQuery("SELECT AVG(m.price) AS avgPrice FROM Magazine m", true);
		Sort sort = Sort.by("someOtherProperty");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWithIgnoringCase("order by m.someOtherProperty asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void prefixesNonAliasedFunctionCallRelatedSortPropertyWhenSelectClauseContainsAliasedFunctionForDifferentProperty() {

		DefaultEntityQuery query = new TestEntityQuery("SELECT m.name, AVG(m.price) AS avgPrice FROM Magazine m",
				true);
		Sort sort = Sort.by("name", "avgPrice");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWithIgnoringCase("order by m.name asc, avgPrice asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithMultipleNumericParameters() {

		DefaultEntityQuery query = new TestEntityQuery(
				"SELECT SUBSTRING(m.name, 2, 5) AS trimmedName FROM Magazine m", true);
		Sort sort = Sort.by("trimmedName");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWithIgnoringCase("order by trimmedName asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithMultipleStringParameters() {

		DefaultEntityQuery query = new TestEntityQuery(
				"SELECT CONCAT(m.name, 'foo') AS extendedName FROM Magazine m", true);
		Sort sort = Sort.by("extendedName");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort)))
				.endsWithIgnoringCase("order by extendedName asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithUnderscores() {

		DefaultEntityQuery query = new TestEntityQuery("SELECT AVG(m.price) AS avg_price FROM Magazine m", true);
		Sort sort = Sort.by("avg_price");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort))).endsWithIgnoringCase("order by avg_price asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithDots() {

		DefaultEntityQuery query = new TestEntityQuery("SELECT AVG(m.price) AS average FROM Magazine m", false);
		Sort sort = Sort.by("avg");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort))).endsWith("order by m.avg asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithDotsNativeQuery() {

		// this is invalid since the '.' character is not allowed. Not in sql nor in JPQL.
		assertThatThrownBy(() -> new TestEntityQuery("SELECT AVG(m.price) AS m.avg FROM Magazine m", true)) //
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWhenQueryStringContainsMultipleWhiteSpaces() {

		DefaultEntityQuery query = new TestEntityQuery(
				"SELECT  AVG(  m.price  )   AS   avgPrice   FROM Magazine   m", true);
		Sort sort = Sort.by("avgPrice");

		assertThat(getEnhancer(query).rewrite(getRewriteInformation(sort))).endsWithIgnoringCase("order by avgPrice asc");
	}

	@Test // DATAJPA-1171
	void doesNotContainStaticClauseInExistsQuery() {
		assertThat(QueryUtils.getExistsQueryString("entity", "x", Collections.singleton("id")))
				.endsWithIgnoringCase("WHERE x.id = :id");
	}

	@Test // DATAJPA-1363
	void discoversAliasWithComplexFunction() {

		assertThat(
				QueryUtils.getFunctionAliases("select new MyDto(sum(case when myEntity.prop3=0 then 1 else 0 end) as myAlias")) //
				.contains("myAlias");
	}

	@Test // DATAJPA-1506
	void detectsAliasWithGroupAndOrderBy() {

		DefaultEntityQuery queryWithGroupNoAlias = new TestEntityQuery("select * from User group by name", true);
		DefaultEntityQuery queryWithGroupAlias = new TestEntityQuery("select * from User u group by name", true);

		DefaultEntityQuery queryWithOrderNoAlias = new TestEntityQuery("select * from User order by name", true);
		DefaultEntityQuery queryWithOrderAlias = new TestEntityQuery("select * from User u order by name", true);

		assertThat(getEnhancer(queryWithGroupNoAlias).detectAlias()).isNull();
		assertThat(getEnhancer(queryWithOrderNoAlias).detectAlias()).isNull();
		assertThat(getEnhancer(queryWithGroupAlias).detectAlias()).isEqualTo("u");
		assertThat(getEnhancer(queryWithOrderAlias).detectAlias()).isEqualTo("u");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForFieldAliases() {

		DefaultEntityQuery query = new TestEntityQuery(
				"SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a",
				true);
		Sort sort = Sort.by("authorName");

		String fullQuery = getEnhancer(query).rewrite(getRewriteInformation(sort));

		assertThat(fullQuery).endsWithIgnoringCase("order by authorName asc");
	}

	@Test // GH-2280
	void appliesOrderingCorrectlyForFieldAliasWithIgnoreCase() {

		DefaultEntityQuery query = new TestEntityQuery(
				"SELECT customer.id as id, customer.name as name FROM CustomerEntity customer", true);
		Sort sort = Sort.by(Sort.Order.by("name").ignoreCase());

		String fullQuery = getEnhancer(query).rewrite(getRewriteInformation(sort));

		assertThat(fullQuery).isEqualToIgnoringCase(
				"SELECT customer.id as id, customer.name as name FROM CustomerEntity customer order by lower(name) asc");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForFunctionAliases() {

		DefaultEntityQuery query = new TestEntityQuery(
				"SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a",
				true);
		Sort sort = Sort.by("title");

		String fullQuery = getEnhancer(query).rewrite(getRewriteInformation(sort));

		assertThat(fullQuery).endsWithIgnoringCase("order by title asc");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForSimpleField() {

		DefaultEntityQuery query = new TestEntityQuery(
				"SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a",
				true);
		Sort sort = Sort.by("price");

		String fullQuery = getEnhancer(query).rewrite(getRewriteInformation(sort));

		assertThat(fullQuery).endsWithIgnoringCase("order by m.price asc");
	}

	@Test
	void createCountQuerySupportsLineBreakRightAfterDistinct() {

		DefaultEntityQuery query1 = new TestEntityQuery("select\ndistinct\nuser.age,\n" + //
				"user.name\n" + //
				"from\nUser\nuser", true);

		DefaultEntityQuery query2 = new TestEntityQuery("select\ndistinct user.age,\n" + //
				"user.name\n" + //
				"from\nUser\nuser", true);

		assertThat(getEnhancer(query1).createCountQueryFor(null)).isEqualTo(getEnhancer(query2).createCountQueryFor(null));
	}

	@Test
	void detectsAliasWithGroupAndOrderByWithLineBreaks() {

		DefaultEntityQuery queryWithGroupAndLineBreak = new TestEntityQuery("select * from User group\nby name",
				true);
		DefaultEntityQuery queryWithGroupAndLineBreakAndAlias = new TestEntityQuery(
				"select * from User u group\nby name", true);

		assertThat(getEnhancer(queryWithGroupAndLineBreak).detectAlias()).isNull();
		assertThat(getEnhancer(queryWithGroupAndLineBreakAndAlias).detectAlias()).isEqualTo("u");

		DefaultEntityQuery queryWithOrderAndLineBreak = new TestEntityQuery("select * from User order\nby name",
				true);
		DefaultEntityQuery queryWithOrderAndLineBreakAndAlias = new TestEntityQuery(
				"select * from User u order\nby name", true);
		DefaultEntityQuery queryWithOrderAndMultipleLineBreakAndAlias = new TestEntityQuery(
				"select * from User\nu\norder \n by name", true);

		assertThat(getEnhancer(queryWithOrderAndLineBreak).detectAlias()).isNull();
		assertThat(getEnhancer(queryWithOrderAndLineBreakAndAlias).detectAlias()).isEqualTo("u");
		assertThat(getEnhancer(queryWithOrderAndMultipleLineBreakAndAlias).detectAlias()).isEqualTo("u");
	}

	@ParameterizedTest // DATAJPA-1679
	@MethodSource("findProjectionClauseWithDistinctSource")
	void findProjectionClauseWithDistinct(DefaultEntityQuery query, String expected) {

		SoftAssertions.assertSoftly(sofly -> sofly.assertThat(getEnhancer(query).getProjection()).isEqualTo(expected));
	}

	public static Stream<Arguments> findProjectionClauseWithDistinctSource() {

		return Stream.of( //
				Arguments.of(new TestEntityQuery("select * from x", true), "*"), //
				Arguments.of(new TestEntityQuery("select a, b, c from x", true), "a, b, c"), //
				Arguments.of(new TestEntityQuery("select distinct a, b, c from x", true), "a, b, c"), //
				Arguments.of(new TestEntityQuery("select DISTINCT a, b, c from x", true), "a, b, c") //
		);
	}

	@Test // DATAJPA-1696
	void findProjectionClauseWithSubselect() {

		// This is not a required behavior, in fact the opposite is,
		// but it documents a current limitation.
		// to fix this without breaking findProjectionClauseWithIncludedFrom we need a more sophisticated parser.
		assertThat(QueryUtils.getProjection("select * from (select x from y)")).isNotEqualTo("*");
	}

	@Test // DATAJPA-1696
	void findProjectionClauseWithSubselectNative() {

		// This is a required behavior the testcase in #findProjectionClauseWithSubselect tells why
		String queryString = "select * from (select x from y)";
		DefaultEntityQuery query = new TestEntityQuery(queryString, true);

		assertThat(getEnhancer(query).getProjection()).isEqualTo("*");
	}

	@Test // GH-2441
	void correctFunctionAliasWithComplexNestedFunctions() {

		String queryString = "\nSELECT \nCAST(('{' || string_agg(distinct array_to_string(c.institutes_ids, ','), ',') || '}') AS bigint[]) as institutesIds\nFROM\ncity c";

		DefaultEntityQuery nativeQuery = new TestEntityQuery(queryString, true);
		JSqlParserQueryEnhancer queryEnhancer = (JSqlParserQueryEnhancer) getEnhancer(nativeQuery);

		assertThat(queryEnhancer.getSelectionAliases()).contains("institutesIds");
	}

	@Test // GH-2441
	void correctApplySortOnComplexNestedFunctionQuery() {

		String queryString = "SELECT dd.institutesIds FROM (\n" //
				+ "                                SELECT\n" //
				+ "                                    CAST(('{' || string_agg(distinct array_to_string(c.institutes_ids, ','), ',') || '}') AS bigint[]) as institutesIds\n"
				+ "                                FROM\n" //
				+ "                                    city c\n" //
				+ "                            ) dd";

		DefaultEntityQuery nativeQuery = new TestEntityQuery(queryString, true);
		QueryEnhancer queryEnhancer = getEnhancer(nativeQuery);
		String result = queryEnhancer
				.rewrite(getRewriteInformation(Sort.by(new Sort.Order(Sort.Direction.ASC, "institutesIds"))));

		assertThat(result).containsIgnoringCase("order by dd.institutesIds");
	}

	@Test // GH-2555
	void modifyingQueriesAreDetectedCorrectly() {

		String modifyingQuery = "update userinfo user set user.is_in_treatment = false where user.id = :userId";

		String aliasNotConsideringQueryType = QueryUtils.detectAlias(modifyingQuery);
		String projectionNotConsideringQueryType = QueryUtils.getProjection(modifyingQuery);
		boolean constructorExpressionNotConsideringQueryType = QueryUtils.hasConstructorExpression(modifyingQuery);
		String countQueryForNotConsiderQueryType = QueryUtils.createCountQueryFor(modifyingQuery);

		DefaultEntityQuery modiQuery = new TestEntityQuery(modifyingQuery, true);

		assertThat(modiQuery.getAlias()).isEqualToIgnoringCase(aliasNotConsideringQueryType);
		assertThat(modiQuery.getProjection()).isEqualToIgnoringCase(projectionNotConsideringQueryType);
		assertThat(modiQuery.hasConstructorExpression()).isEqualTo(constructorExpressionNotConsideringQueryType);

		assertThat(countQueryForNotConsiderQueryType).isEqualToIgnoringCase(modifyingQuery);
		assertThat(QueryEnhancer.create(modiQuery).createCountQueryFor(null)).isEqualToIgnoringCase(modifyingQuery);
	}

	@ParameterizedTest // GH-2593
	@MethodSource("insertStatementIsProcessedSameAsDefaultSource")
	void insertStatementIsProcessedSameAsDefault(String insertQuery) {

		DefaultEntityQuery stringQuery = new TestEntityQuery(insertQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancer.create(stringQuery);

		Sort sorting = Sort.by("day").descending();

		// queryutils results
		String queryUtilsDetectAlias = QueryUtils.detectAlias(insertQuery);
		String queryUtilsProjection = QueryUtils.getProjection(insertQuery);
		String queryUtilsCountQuery = QueryUtils.createCountQueryFor(insertQuery);
		Set<String> queryUtilsOuterJoinAlias = QueryUtils.getOuterJoinAliases(insertQuery);

		// direct access
		assertThat(stringQuery.getAlias()).isEqualToIgnoringCase(queryUtilsDetectAlias);
		assertThat(stringQuery.getProjection()).isEqualToIgnoringCase(queryUtilsProjection);
		assertThat(stringQuery.hasConstructorExpression()).isFalse();

		// access over enhancer
		assertThat(queryEnhancer.createCountQueryFor(null)).isEqualToIgnoringCase(queryUtilsCountQuery);
		assertThat(queryEnhancer.rewrite(getRewriteInformation(sorting))).isEqualTo(insertQuery); // cant check with
																																															// queryutils result since
		// query utils appens order by which is not
		// supported by sql standard.
		assertThat(queryEnhancer.detectAlias()).isEqualToIgnoringCase(queryUtilsDetectAlias);
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase(queryUtilsProjection);
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	public static Stream<Arguments> insertStatementIsProcessedSameAsDefaultSource() {

		return Stream.of( //
				Arguments.of("INSERT INTO FOO(A) VALUES('A')"), //
				Arguments.of("INSERT INTO randomsecondTable(A,B,C,D) VALUES('A','B','C','D')") //
		);
	}

	public static Stream<Arguments> detectsJoinAliasesCorrectlySource() {

		return Stream.of( //
				Arguments.of("select p from Person p left outer join x.foo b2_$ar", Collections.singletonList("b2_$ar")), //
				Arguments.of("select p from Person p left join x.foo b2_$ar", Collections.singletonList("b2_$ar")), //
				Arguments.of("select p from Person p left outer join x.foo as b2_$ar, left join x.bar as foo",
						Arrays.asList("b2_$ar", "foo")), //
				Arguments.of("select p from Person p left join x.foo as b2_$ar, left outer join x.bar foo",
						Arrays.asList("b2_$ar", "foo")) //

		);
	}

	private static void assertCountQuery(String originalQuery, String countQuery, boolean nativeQuery) {
		assertCountQuery(new TestEntityQuery(originalQuery, nativeQuery), countQuery);
	}

	private static void assertCountQuery(DefaultEntityQuery originalQuery, String countQuery) {
		assertThat(getEnhancer(originalQuery).createCountQueryFor(null)).isEqualToIgnoringCase(countQuery);
	}

	private static DefaultQueryRewriteInformation getRewriteInformation(Sort sort) {
		return new DefaultQueryRewriteInformation(sort,
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory()));
	}

	private static QueryEnhancer getEnhancer(DeclaredQuery query) {
		return QueryEnhancer.create(query);
	}

}
