/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

/**
 * Unit tests for {@link QueryEnhancer}.
 *
 * @author Diego Krupitza
 * @author Geoffrey Deremetz
 * @author Krzysztof Krason
 */
class QueryEnhancerUnitTests {

	private static final String QUERY = "select u from User u";
	private static final String FQ_QUERY = "select u from org.acme.domain.User$Foo_Bar u";
	private static final String SIMPLE_QUERY = "from User u";
	private static final String COUNT_QUERY = "select count(u) from User u";

	private static final String QUERY_WITH_AS = "select u from User as u where u.username = ?";

	@Test
	void createsCountQueryCorrectly() {
		assertCountQuery(QUERY, COUNT_QUERY, true);
	}

	@Test
	void createsCountQueriesCorrectlyForCapitalLetterJPQL() {

		assertCountQuery("FROM User u WHERE u.foo.bar = ?", "select count(u) FROM User u WHERE u.foo.bar = ?", false);

		assertCountQuery("SELECT u FROM User u where u.foo.bar = ?", "select count(u) FROM User u where u.foo.bar = ?",
				true);
	}

	@Test
	void createsCountQueryForDistinctQueries() {

		assertCountQuery("select distinct u from User u where u.foo = ?",
				"select count(distinct u) from User u where u.foo = ?", true);
	}

	@Test
	void createsCountQueryForConstructorQueries() {

		assertCountQuery("select distinct new User(u.name) from User u where u.foo = ?",
				"select count(distinct u) from User u where u.foo = ?", false);
	}

	@Test
	void createsCountQueryForJoinsNoneNative() {

		assertCountQuery("select distinct new User(u.name) from User u left outer join u.roles r WHERE r = ?",
				"select count(distinct u) from User u left outer join u.roles r WHERE r = ?", false);
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

	@Test
	void createsCountQueryForAliasesCorrectly() {

		assertCountQuery("select u from User as u", "select count(u) from User as u", true);
	}

	@Test
	void allowsShortJpaSyntax() {

		assertCountQuery(SIMPLE_QUERY, COUNT_QUERY, false);
	}

	@ParameterizedTest
	@MethodSource("detectsAliasWithUCorrectlySource")
	void detectsAliasWithUCorrectly(DeclaredQuery query, String alias) {
		assertThat(getEnhancer(query).detectAlias()).isEqualTo(alias);
	}

	public static Stream<Arguments> detectsAliasWithUCorrectlySource() {

		return Stream.of( //
				Arguments.of(new StringQuery(QUERY, true), "u"), //
				Arguments.of(new StringQuery(SIMPLE_QUERY, false), "u"), //
				Arguments.of(new StringQuery(COUNT_QUERY, true), "u"), //
				Arguments.of(new StringQuery(QUERY_WITH_AS, true), "u"), //
				Arguments.of(new StringQuery("SELECT FROM USER U", false), "U"), //
				Arguments.of(new StringQuery("select u from  User u", true), "u"), //
				Arguments.of(new StringQuery("select u from  com.acme.User u", true), "u"), //
				Arguments.of(new StringQuery("select u from T05User u", true), "u") //
		);
	}

	@Test
	void allowsFullyQualifiedEntityNamesInQuery() {

		StringQuery query = new StringQuery(FQ_QUERY, true);

		assertThat(getEnhancer(query).detectAlias()).isEqualTo("u");
		assertCountQuery(FQ_QUERY, "select count(u) from org.acme.domain.User$Foo_Bar u", true);
	}

	@Test // DATAJPA-252
	void doesNotPrefixOrderReferenceIfOuterJoinAliasDetected() {

		StringQuery query = new StringQuery("select p from Person p left join p.address address", true);

		endsIgnoringCase(getEnhancer(query).applySorting(Sort.by("address.city")), "order by address.city asc");
		endsIgnoringCase(getEnhancer(query).applySorting(Sort.by("address.city", "lastname"), "p"),
				"order by address.city asc, p.lastname asc");
	}

	@Test // DATAJPA-252
	void extendsExistingOrderByClausesCorrectly() {

		StringQuery query = new StringQuery("select p from Person p order by p.lastname asc", true);

		endsIgnoringCase(getEnhancer(query).applySorting(Sort.by("firstname"), "p"),
				"order by p.lastname asc, p.firstname asc");
	}

	@Test // DATAJPA-296
	void appliesIgnoreCaseOrderingCorrectly() {

		Sort sort = Sort.by(Sort.Order.by("firstname").ignoreCase());

		StringQuery query = new StringQuery("select p from Person p", true);

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "p"), "order by lower(p.firstname) asc");
	}

	@Test // DATAJPA-296
	void appendsIgnoreCaseOrderingCorrectly() {

		Sort sort = Sort.by(Sort.Order.by("firstname").ignoreCase());

		StringQuery query = new StringQuery("select p from Person p order by p.lastname asc", true);

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "p"), "order by p.lastname asc, lower(p.firstname) asc");
	}

	@Test // DATAJPA-342
	void usesReturnedVariableInCountProjectionIfSet() {

		assertCountQuery("select distinct m.genre from Media m where m.user = ?1 order by m.genre asc",
				"select count(distinct m.genre) from Media m where m.user = ?1", true);
	}

	@Test // DATAJPA-343
	void projectsCountQueriesForQueriesWithSubSelects() {

		assertCountQuery("select o from Foo o where cb.id in (select b from Bar b)",
				"select count(o) from Foo o where cb.id in (select b from Bar b)", true);
	}

	@Test // DATAJPA-148
	void doesNotPrefixSortsIfFunction() {

		StringQuery query = new StringQuery("select p from Person p", true);
		Sort sort = Sort.by("sum(foo)");

		QueryEnhancer enhancer = getEnhancer(query);

		assertThatThrownBy(() -> enhancer.applySorting(sort, "p")) //
				.isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test // DATAJPA-377
	void removesOrderByInGeneratedCountQueryFromOriginalQueryIfPresent() {

		assertCountQuery("select distinct m.genre from Media m where m.user = ?1 OrDer  By   m.genre ASC",
				"select count(distinct m.genre) from Media m where m.user = ?1", true);
	}

	@Test // DATAJPA-375
	void findsExistingOrderByIndependentOfCase() {

		Sort sort = Sort.by("lastname");
		StringQuery originalQuery = new StringQuery("select p from Person p ORDER BY p.firstname", true);
		String query = getEnhancer(originalQuery).applySorting(sort, "p");

		endsIgnoringCase(query, "ORDER BY p.firstname, p.lastname asc");
	}

	@Test // DATAJPA-409
	void createsCountQueryForNestedReferenceCorrectly() {
		assertCountQuery("select a.b from A a", "select count(a.b) from A a", true);
	}

	@Test // DATAJPA-420
	void createsCountQueryForScalarSelects() {
		assertCountQuery("select p.lastname,p.firstname from Person p", "select count(p) from Person p", true);
	}

	@Test // DATAJPA-456
	void createCountQueryFromTheGivenCountProjection() {

		StringQuery query = new StringQuery("select p.lastname,p.firstname from Person p", true);

		assertThat(getEnhancer(query).createCountQueryFor("p.lastname"))
				.isEqualToIgnoringCase("select count(p.lastname) from Person p");
	}

	@Test // DATAJPA-726
	void detectsAliasesInPlainJoins() {

		StringQuery query = new StringQuery("select p from Customer c join c.productOrder p where p.delay = true", true);
		Sort sort = Sort.by("p.lineItems");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "c"), "order by p.lineItems asc");
	}

	@Test // DATAJPA-736
	void supportsNonAsciiCharactersInEntityNames() {

		StringQuery query = new StringQuery("select u from Usèr u", true);

		assertThat(getEnhancer(query).createCountQueryFor()).isEqualToIgnoringCase("select count(u) from Usèr u");
	}

	@Test // DATAJPA-798
	void detectsAliasInQueryContainingLineBreaks() {

		StringQuery query = new StringQuery("select \n u \n from \n User \nu", true);

		assertThat(getEnhancer(query).detectAlias()).isEqualTo("u");
	}

	@Test // DATAJPA-815
	void doesPrefixPropertyWithNonNative() {

		StringQuery query = new StringQuery("from Cat c join Dog d", false);
		Sort sort = Sort.by("dPropertyStartingWithJoinAlias");

		assertThat(getEnhancer(query).applySorting(sort, "c")).endsWith("order by c.dPropertyStartingWithJoinAlias asc");
	}

	@Test // DATAJPA-815
	void doesPrefixPropertyWithNative() {

		StringQuery query = new StringQuery("Select * from Cat c join Dog d", true);
		Sort sort = Sort.by("dPropertyStartingWithJoinAlias");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "c"), "order by c.dPropertyStartingWithJoinAlias asc");
	}

	@Test // DATAJPA-938
	void detectsConstructorExpressionInDistinctQuery() {

		StringQuery query = new StringQuery("select distinct new Foo() from Bar b", false);

		assertThat(getEnhancer(query).hasConstructorExpression()).isTrue();
	}

	@Test // DATAJPA-938
	void detectsComplexConstructorExpression() {

		StringQuery query = new StringQuery("select new foo.bar.Foo(ip.id, ip.name, sum(lp.amount)) " //
				+ "from Bar lp join lp.investmentProduct ip " //
				+ "where (lp.toDate is null and lp.fromDate <= :now and lp.fromDate is not null) and lp.accountId = :accountId "
				//
				+ "group by ip.id, ip.name, lp.accountId " //
				+ "order by ip.name ASC", false);

		assertThat(getEnhancer(query).hasConstructorExpression()).isTrue();
	}

	@Test // DATAJPA-938
	void detectsConstructorExpressionWithLineBreaks() {

		StringQuery query = new StringQuery("select new foo.bar.FooBar(\na.id) from DtoA a ", false);

		assertThat(getEnhancer(query).hasConstructorExpression()).isTrue();
	}

	@Test // DATAJPA-960
	void doesNotQualifySortIfNoAliasDetectedNonNative() {

		StringQuery query = new StringQuery("from mytable where ?1 is null", false);

		assertThat(getEnhancer(query).applySorting(Sort.by("firstname"))).endsWith("order by firstname asc");
	}

	@Test // DATAJPA-960
	void doesNotQualifySortIfNoAliasDetectedNative() {

		StringQuery query = new StringQuery("Select * from mytable where ?1 is null", true);

		endsIgnoringCase(getEnhancer(query).applySorting(Sort.by("firstname")), "order by firstname asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotAllowWhitespaceInSort() {

		StringQuery query = new StringQuery("select p from Person p", true);

		Sort sort = Sort.by("case when foo then bar");

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> getEnhancer(query).applySorting(sort, "p"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixUnsafeJpaSortFunctionCalls() {

		JpaSort sort = JpaSort.unsafe("sum(foo)");
		StringQuery query = new StringQuery("select p from Person p", true);

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "p"), "order by sum(foo) asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixMultipleAliasedFunctionCalls() {

		StringQuery query = new StringQuery("SELECT AVG(m.price) AS avgPrice, SUM(m.stocks) AS sumStocks FROM Magazine m",
				true);
		Sort sort = Sort.by("avgPrice", "sumStocks");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "m"), "order by avgPrice asc, sumStocks asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixSingleAliasedFunctionCalls() {

		StringQuery query = new StringQuery("SELECT AVG(m.price) AS avgPrice FROM Magazine m", true);
		Sort sort = Sort.by("avgPrice");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "m"), "order by avgPrice asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void prefixesSingleNonAliasedFunctionCallRelatedSortProperty() {

		StringQuery query = new StringQuery("SELECT AVG(m.price) AS avgPrice FROM Magazine m", true);
		Sort sort = Sort.by("someOtherProperty");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "m"), "order by m.someOtherProperty asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void prefixesNonAliasedFunctionCallRelatedSortPropertyWhenSelectClauseContainsAliasedFunctionForDifferentProperty() {

		StringQuery query = new StringQuery("SELECT m.name, AVG(m.price) AS avgPrice FROM Magazine m", true);
		Sort sort = Sort.by("name", "avgPrice");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "m"), "order by m.name asc, avgPrice asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithMultipleNumericParameters() {

		StringQuery query = new StringQuery("SELECT SUBSTRING(m.name, 2, 5) AS trimmedName FROM Magazine m", true);
		Sort sort = Sort.by("trimmedName");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "m"), "order by trimmedName asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithMultipleStringParameters() {

		StringQuery query = new StringQuery("SELECT CONCAT(m.name, 'foo') AS extendedName FROM Magazine m", true);
		Sort sort = Sort.by("extendedName");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "m"), "order by extendedName asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithUnderscores() {

		StringQuery query = new StringQuery("SELECT AVG(m.price) AS avg_price FROM Magazine m", true);
		Sort sort = Sort.by("avg_price");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "m"), "order by avg_price asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithDots() {

		StringQuery query = new StringQuery("SELECT AVG(m.price) AS m.avg FROM Magazine m", false);
		Sort sort = Sort.by("m.avg");

		assertThat(getEnhancer(query).applySorting(sort, "m")).endsWith("order by m.avg asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithDotsNativeQuery() {

		// this is invalid since the '.' character is not allowed. Not in sql nor in JPQL.
		assertThatThrownBy(() -> new StringQuery("SELECT AVG(m.price) AS m.avg FROM Magazine m", true)) //
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWhenQueryStringContainsMultipleWhiteSpaces() {

		StringQuery query = new StringQuery("SELECT  AVG(  m.price  )   AS   avgPrice   FROM Magazine   m", true);
		Sort sort = Sort.by("avgPrice");

		endsIgnoringCase(getEnhancer(query).applySorting(sort, "m"), "order by avgPrice asc");
	}

	@Test // DATAJPA-1000
	void discoversCorrectAliasForJoinFetch() {

		String queryString = "SELECT DISTINCT user FROM User user LEFT JOIN user.authorities AS authority";
		Set<String> aliases = QueryUtils.getOuterJoinAliases(queryString);

		StringQuery nativeQuery = new StringQuery(queryString, true);
		Set<String> joinAliases = new JSqlParserQueryEnhancer(nativeQuery).getJoinAliases();

		assertThat(aliases).containsExactly("authority");
		assertThat(joinAliases).containsExactly("authority");
	}

	@Test // DATAJPA-1171
	void doesNotContainStaticClauseInExistsQuery() {
		endsIgnoringCase(QueryUtils.getExistsQueryString("entity", "x", Collections.singleton("id")), "WHERE x.id = :id");
	}

	@Test // DATAJPA-1363
	void discoversAliasWithComplexFunction() {

		assertThat(
				QueryUtils.getFunctionAliases("select new MyDto(sum(case when myEntity.prop3=0 then 1 else 0 end) as myAlias")) //
						.contains("myAlias");
	}

	@Test // DATAJPA-1506
	void detectsAliasWithGroupAndOrderBy() {

		StringQuery queryWithGroupNoAlias = new StringQuery("select * from User group by name", true);
		StringQuery queryWithGroupAlias = new StringQuery("select * from User u group by name", true);

		StringQuery queryWithOrderNoAlias = new StringQuery("select * from User order by name", true);
		StringQuery queryWithOrderAlias = new StringQuery("select * from User u order by name", true);

		assertThat(getEnhancer(queryWithGroupNoAlias).detectAlias()).isNull();
		assertThat(getEnhancer(queryWithOrderNoAlias).detectAlias()).isNull();
		assertThat(getEnhancer(queryWithGroupAlias).detectAlias()).isEqualTo("u");
		assertThat(getEnhancer(queryWithOrderAlias).detectAlias()).isEqualTo("u");
	}

	@Test // DATAJPA-1500
	void createCountQuerySupportsWhitespaceCharacters() {

		StringQuery query = new StringQuery("select * from User user\n" + //
				"  where user.age = 18\n" + //
				"  order by user.name\n ", true);

		assertThat(getEnhancer(query).createCountQueryFor())
				.isEqualToIgnoringCase("select count(user) from User user where user.age = 18");
	}

	@Test
	void createCountQuerySupportsLineBreaksInSelectClause() {

		StringQuery query = new StringQuery("select user.age,\n" + //
				"  user.name\n" + //
				"  from User user\n" + //
				"  where user.age = 18\n" + //
				"  order\nby\nuser.name\n ", true);

		assertThat(getEnhancer(query).createCountQueryFor())
				.isEqualToIgnoringCase("select count(user) from User user where user.age = 18");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForFieldAliases() {

		StringQuery query = new StringQuery(
				"SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a",
				true);
		Sort sort = Sort.by("authorName");

		String fullQuery = getEnhancer(query).applySorting(sort);

		endsIgnoringCase(fullQuery, "order by authorName asc");
	}

	@Test // GH-2280
	void appliesOrderingCorrectlyForFieldAliasWithIgnoreCase() {

		StringQuery query = new StringQuery("SELECT customer.id as id, customer.name as name FROM CustomerEntity customer",
				true);
		Sort sort = Sort.by(Sort.Order.by("name").ignoreCase());

		String fullQuery = getEnhancer(query).applySorting(sort);

		assertThat(fullQuery).isEqualToIgnoringCase(
				"SELECT customer.id as id, customer.name as name FROM CustomerEntity customer order by lower(name) asc");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForFunctionAliases() {

		StringQuery query = new StringQuery(
				"SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a",
				true);
		Sort sort = Sort.by("title");

		String fullQuery = getEnhancer(query).applySorting(sort);

		endsIgnoringCase(fullQuery, "order by title asc");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForSimpleField() {

		StringQuery query = new StringQuery(
				"SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a",
				true);
		Sort sort = Sort.by("price");

		String fullQuery = getEnhancer(query).applySorting(sort);

		endsIgnoringCase(fullQuery, "order by m.price asc");
	}

	@Test
	void createCountQuerySupportsLineBreakRightAfterDistinct() {

		StringQuery query1 = new StringQuery("select\ndistinct\nuser.age,\n" + //
				"user.name\n" + //
				"from\nUser\nuser", true);

		StringQuery query2 = new StringQuery("select\ndistinct user.age,\n" + //
				"user.name\n" + //
				"from\nUser\nuser", true);

		assertThat(getEnhancer(query1).createCountQueryFor()).isEqualTo(getEnhancer(query2).createCountQueryFor());
	}

	@Test
	void detectsAliasWithGroupAndOrderByWithLineBreaks() {

		StringQuery queryWithGroupAndLineBreak = new StringQuery("select * from User group\nby name", true);
		StringQuery queryWithGroupAndLineBreakAndAlias = new StringQuery("select * from User u group\nby name", true);

		assertThat(getEnhancer(queryWithGroupAndLineBreak).detectAlias()).isNull();
		assertThat(getEnhancer(queryWithGroupAndLineBreakAndAlias).detectAlias()).isEqualTo("u");

		StringQuery queryWithOrderAndLineBreak = new StringQuery("select * from User order\nby name", true);
		StringQuery queryWithOrderAndLineBreakAndAlias = new StringQuery("select * from User u order\nby name", true);
		StringQuery queryWithOrderAndMultipleLineBreakAndAlias = new StringQuery("select * from User\nu\norder \n by name",
				true);

		assertThat(getEnhancer(queryWithOrderAndLineBreak).detectAlias()).isNull();
		assertThat(getEnhancer(queryWithOrderAndLineBreakAndAlias).detectAlias()).isEqualTo("u");
		assertThat(getEnhancer(queryWithOrderAndMultipleLineBreakAndAlias).detectAlias()).isEqualTo("u");
	}

	@ParameterizedTest // DATAJPA-1679
	@MethodSource("findProjectionClauseWithDistinctSource")
	void findProjectionClauseWithDistinct(DeclaredQuery query, String expected) {

		SoftAssertions.assertSoftly(sofly -> sofly.assertThat(getEnhancer(query).getProjection()).isEqualTo(expected));
	}

	public static Stream<Arguments> findProjectionClauseWithDistinctSource() {

		return Stream.of( //
				Arguments.of(new StringQuery("select * from x", true), "*"), //
				Arguments.of(new StringQuery("select a, b, c from x", true), "a, b, c"), //
				Arguments.of(new StringQuery("select distinct a, b, c from x", true), "a, b, c"), //
				Arguments.of(new StringQuery("select DISTINCT a, b, c from x", true), "a, b, c") //
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
		StringQuery query = new StringQuery(queryString, true);

		assertThat(getEnhancer(query).getProjection()).isEqualTo("*");
	}

	@Test // DATAJPA-1696
	void findProjectionClauseWithIncludedFrom() {

		StringQuery query = new StringQuery("select x, frommage, y from t", true);

		assertThat(getEnhancer(query).getProjection()).isEqualTo("x, frommage, y");
	}

	@Test
	void countProjectionDistinctQueryIncludesNewLineAfterFromAndBeforeJoin() {

		StringQuery originalQuery = new StringQuery(
				"SELECT DISTINCT entity1\nFROM Entity1 entity1\nLEFT JOIN Entity2 entity2 ON entity1.key = entity2.key", true);

		assertCountQuery(originalQuery,
				"select count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key");
	}

	@Test
	void countProjectionDistinctQueryIncludesNewLineAfterEntity() {

		StringQuery originalQuery = new StringQuery(
				"SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key", true);

		assertCountQuery(originalQuery,
				"select count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key");
	}

	@Test
	void countProjectionDistinctQueryIncludesNewLineAfterEntityAndBeforeWhere() {

		StringQuery originalQuery = new StringQuery(
				"SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key\nwhere entity1.id = 1799",
				true);

		assertCountQuery(originalQuery,
				"select count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key where entity1.id = 1799");
	}

	@Test
	void createsCountQueriesCorrectlyForCapitalLetter() {
		assertCountQuery("SELECT u FROM User u where u.foo.bar = ?", "select count(u) FROM User u where u.foo.bar = ?",
				true);
	}

	@ParameterizedTest // DATAJPA-252
	@MethodSource("detectsJoinAliasesCorrectlySource")
	void detectsJoinAliasesCorrectly(String queryString, List<String> aliases) {

		StringQuery nativeQuery = new StringQuery(queryString, true);
		StringQuery nonNativeQuery = new StringQuery(queryString, false);

		Set<String> nativeJoinAliases = getEnhancer(nativeQuery).getJoinAliases();
		Set<String> nonNativeJoinAliases = getEnhancer(nonNativeQuery).getJoinAliases();

		assertThat(nonNativeJoinAliases).containsAll(nativeJoinAliases);
		assertThat(nativeJoinAliases).hasSameSizeAs(aliases) //
				.containsAll(aliases);

	}

	@Test // GH-2441
	void correctFunctionAliasWithComplexNestedFunctions() {

		String queryString = "\nSELECT \nCAST(('{' || string_agg(distinct array_to_string(c.institutes_ids, ','), ',') || '}') AS bigint[]) as institutesIds\nFROM\ncity c";

		StringQuery nativeQuery = new StringQuery(queryString, true);
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

		StringQuery nativeQuery = new StringQuery(queryString, true);
		QueryEnhancer queryEnhancer = getEnhancer(nativeQuery);
		String result = queryEnhancer.applySorting(Sort.by(new Sort.Order(Sort.Direction.ASC, "institutesIds")));

		assertThat(result).containsIgnoringCase("order by dd.institutesIds");
	}

	@Test // GH-2511
	void countQueryUsesCorrectVariable() {

		StringQuery nativeQuery = new StringQuery("SELECT * FROM User WHERE created_at > $1", true);

		QueryEnhancer queryEnhancer = getEnhancer(nativeQuery);
		String countQueryFor = queryEnhancer.createCountQueryFor();
		assertThat(countQueryFor).isEqualTo("SELECT count(*) FROM User WHERE created_at > $1");

		nativeQuery = new StringQuery("SELECT * FROM (select * from test) ", true);
		queryEnhancer = getEnhancer(nativeQuery);
		countQueryFor = queryEnhancer.createCountQueryFor();
		assertThat(countQueryFor).isEqualTo("SELECT count(*) FROM (SELECT * FROM test)");

		nativeQuery = new StringQuery("SELECT * FROM (select * from test) as test", true);
		queryEnhancer = getEnhancer(nativeQuery);
		countQueryFor = queryEnhancer.createCountQueryFor();
		assertThat(countQueryFor).isEqualTo("SELECT count(test) FROM (SELECT * FROM test) AS test");
	}

	@Test // GH-2555
	void modifyingQueriesAreDetectedCorrectly() {

		String modifyingQuery = "update userinfo user set user.is_in_treatment = false where user.id = :userId";

		String aliasNotConsideringQueryType = QueryUtils.detectAlias(modifyingQuery);
		String projectionNotConsideringQueryType = QueryUtils.getProjection(modifyingQuery);
		boolean constructorExpressionNotConsideringQueryType = QueryUtils.hasConstructorExpression(modifyingQuery);
		String countQueryForNotConsiderQueryType = QueryUtils.createCountQueryFor(modifyingQuery);

		StringQuery modiQuery = new StringQuery(modifyingQuery, true);

		assertThat(modiQuery.getAlias()).isEqualToIgnoringCase(aliasNotConsideringQueryType);
		assertThat(modiQuery.getProjection()).isEqualToIgnoringCase(projectionNotConsideringQueryType);
		assertThat(modiQuery.hasConstructorExpression()).isEqualTo(constructorExpressionNotConsideringQueryType);

		assertThat(countQueryForNotConsiderQueryType).isEqualToIgnoringCase(modifyingQuery);
		assertThat(QueryEnhancerFactory.forQuery(modiQuery).createCountQueryFor()).isEqualToIgnoringCase(modifyingQuery);
	}

	@Test // GH-2578
	void setOperationListWorksWithJSQLParser() {

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
	void complexSetOperationListWorksWithJSQLParser() {

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
	void deeplyNestedcomplexSetOperationListWorksWithJSQLParser() {

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
	void valuesStatementsWorksWithJSQLParser() {

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
	void withStatementsWorksWithJSQLParser() {

		String setQuery = "with sample_data(day, value) as (values ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))) \n"
				+ "select day, value from sample_data as a";

		StringQuery stringQuery = new StringQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

		assertThat(stringQuery.getAlias()).isEqualToIgnoringCase("a");
		assertThat(stringQuery.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(stringQuery.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor()).isEqualToIgnoringCase(
				"with sample_data (day, value) AS (VALUES ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16)))\n"
						+ "SELECT count(a) FROM sample_data AS a");
		assertThat(queryEnhancer.applySorting(Sort.by("day").descending())).endsWith("ORDER BY a.day DESC");
		assertThat(queryEnhancer.getJoinAliases()).isEmpty();
		assertThat(queryEnhancer.detectAlias()).isEqualToIgnoringCase("a");
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@Test // GH-2578
	void multipleWithStatementsWorksWithJSQLParser() {

		String setQuery = "with sample_data(day, value) as (values ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))), test2 as (values (1,2,3)) \n"
				+ "select day, value from sample_data as a";

		StringQuery stringQuery = new StringQuery(setQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

		assertThat(stringQuery.getAlias()).isEqualToIgnoringCase("a");
		assertThat(stringQuery.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(stringQuery.hasConstructorExpression()).isFalse();

		assertThat(queryEnhancer.createCountQueryFor()).isEqualToIgnoringCase(
				"with sample_data (day, value) AS (VALUES ((0, 13), (1, 12), (2, 15), (3, 4), (4, 8), (5, 16))),test2 AS (VALUES (1, 2, 3))\n"
						+ "SELECT count(a) FROM sample_data AS a");
		assertThat(queryEnhancer.applySorting(Sort.by("day").descending())).endsWith("ORDER BY a.day DESC");
		assertThat(queryEnhancer.getJoinAliases()).isEmpty();
		assertThat(queryEnhancer.detectAlias()).isEqualToIgnoringCase("a");
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase("day, value");
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@ParameterizedTest // GH-2593
	@MethodSource("insertStatementIsProcessedSameAsDefaultSource")
	void insertStatementIsProcessedSameAsDefault(String insertQuery) {

		StringQuery stringQuery = new StringQuery(insertQuery, true);
		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(stringQuery);

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
		assertThat(queryEnhancer.createCountQueryFor()).isEqualToIgnoringCase(queryUtilsCountQuery);
		assertThat(queryEnhancer.applySorting(sorting)).isEqualTo(insertQuery); // cant check with queryutils result since
																																						// query utils appens order by which is not
																																						// supported by sql standard.
		assertThat(queryEnhancer.getJoinAliases()).isEqualTo(queryUtilsOuterJoinAlias);
		assertThat(queryEnhancer.detectAlias()).isEqualToIgnoringCase(queryUtilsDetectAlias);
		assertThat(queryEnhancer.getProjection()).isEqualToIgnoringCase(queryUtilsProjection);
		assertThat(queryEnhancer.hasConstructorExpression()).isFalse();
	}

	@ParameterizedTest // GH-2641
	@MethodSource("mergeStatementWorksWithJSqlParserSource")
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

	public static Stream<Arguments> insertStatementIsProcessedSameAsDefaultSource() {

		return Stream.of( //
				Arguments.of("INSERT INTO FOO(A) VALUES('A')"), //
				Arguments.of("INSERT INTO randomsecondTable(A,B,C,D) VALUES('A','B','C','D')") //
		);
	}

	public static Stream<Arguments> mergeStatementWorksWithJSqlParserSource() {

		return Stream.of( //
				Arguments.of(
						"merge into a using (select id, value from b) query on (a.id = query.id) when matched then update set a.value = value",
						"query"),
				Arguments.of(
						"merge into a using (select id2, value from b) on (id = id2) when matched then update set a.value = value",
						null));
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
		assertCountQuery(new StringQuery(originalQuery, nativeQuery), countQuery);
	}

	private static void assertCountQuery(StringQuery originalQuery, String countQuery) {
		assertThat(getEnhancer(originalQuery).createCountQueryFor()).isEqualToIgnoringCase(countQuery);
	}

	private static void endsIgnoringCase(String original, String endWithIgnoreCase) {

		// https://github.com/assertj/assertj-core/pull/2451
		// can be removed when upgrading to version 3.23.0 assertJ
		assertThat(original.toUpperCase()).endsWith(endWithIgnoreCase.toUpperCase());
	}

	private static QueryEnhancer getEnhancer(DeclaredQuery query) {
		return QueryEnhancerFactory.forQuery(query);
	}
}
