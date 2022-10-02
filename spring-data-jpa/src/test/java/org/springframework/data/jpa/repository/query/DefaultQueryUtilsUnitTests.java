/*
 * Copyright 2008-2022 the original author or authors.
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
import static org.springframework.data.jpa.repository.query.QueryUtils.*;

import java.util.Collections;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.JpaSort;

/**
 * Unit test for {@link QueryUtils}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Komi Innocent
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Florian Lüdiger
 * @author Grégoire Druant
 * @author Mohammad Hewedy
 * @author Greg Turnquist
 * @author Vladislav Yukharin
 */
class DefaultQueryUtilsUnitTests {

	private static final String QUERY = "select u from User u";
	private static final String FQ_QUERY = "select u from org.acme.domain.User$Foo_Bar u";
	private static final String SIMPLE_QUERY = "from User u";
	private static final String COUNT_QUERY = "select count(u) from User u";

	private static final String QUERY_WITH_AS = "select u from User as u where u.username = ?";

	@Test
	void createsCountQueryCorrectly() {
		assertCountQuery(QUERY, COUNT_QUERY);
	}

	@Test
	void createsCountQueriesCorrectlyForCapitalLetterJPQL() {

		assertCountQuery("FROM User u WHERE u.foo.bar = ?", "select count(u) FROM User u WHERE u.foo.bar = ?");

		assertCountQuery("SELECT u FROM User u where u.foo.bar = ?", "select count(u) FROM User u where u.foo.bar = ?");
	}

	@Test
	void createsCountQueryForDistinctQueries() {

		assertCountQuery("select distinct u from User u where u.foo = ?",
				"select count(distinct u) from User u where u.foo = ?");
	}

	@Test
	void createsCountQueryForConstructorQueries() {

		assertCountQuery("select distinct new User(u.name) from User u where u.foo = ?",
				"select count(distinct u) from User u where u.foo = ?");
	}

	@Test
	void createsCountQueryForJoins() {

		assertCountQuery("select distinct new User(u.name) from User u left outer join u.roles r WHERE r = ?",
				"select count(distinct u) from User u left outer join u.roles r WHERE r = ?");
	}

	@Test // GH-1869
	void createsCountQueryForJoinsWithTwoArgs() {

		assertCountQuery("select distinct new User(u.name, u.age) from User u left outer join u.roles r WHERE r = ?",
				"select count(distinct u) from User u left outer join u.roles r WHERE r = ?");
	}

	@Test // GH-1869
	void createsCountQueryForDtoWithOneArg() {

		assertCountQuery(
				"SELECT new org.springframework.data.jpa.repository.sample.FirstNameDto(u.firstname) from User u where u.firstname = ?",
				"select count(u) from User u where u.firstname = ?");
	}

	@Test // GH-1869
	void createsCountQueryForDtoWithTwoArgs() {

		assertCountQuery(
				"SELECT new org.springframework.data.jpa.repository.sample.NameOnlyDto(u.firstname, u.lastname) from User u where u.firstname = ?",
				"select count(u) from User u where u.firstname = ?");
	}

	@Test
	void createsCountQueryForQueriesWithSubSelects() {

		assertCountQuery("select u from User u left outer join u.roles r where r in (select r from Role)",
				"select count(u) from User u left outer join u.roles r where r in (select r from Role)");
	}

	@Test
	void createsCountQueryForAliasesCorrectly() {

		assertCountQuery("select u from User as u", "select count(u) from User as u");
	}

	@Test
	void allowsShortJpaSyntax() {

		assertCountQuery(SIMPLE_QUERY, COUNT_QUERY);
	}

	@Test
	void detectsAliasCorrectly() {

		assertThat(detectAlias(QUERY)).isEqualTo("u");
		assertThat(detectAlias(SIMPLE_QUERY)).isEqualTo("u");
		assertThat(detectAlias(COUNT_QUERY)).isEqualTo("u");
		assertThat(detectAlias(QUERY_WITH_AS)).isEqualTo("u");
		assertThat(detectAlias("SELECT FROM USER U")).isEqualTo("U");
		assertThat(detectAlias("select u from  User u")).isEqualTo("u");
		assertThat(detectAlias("select u from  com.acme.User u")).isEqualTo("u");
		assertThat(detectAlias("select u from T05User u")).isEqualTo("u");
	}

	@Test
	void allowsFullyQualifiedEntityNamesInQuery() {

		assertThat(detectAlias(FQ_QUERY)).isEqualTo("u");
		assertCountQuery(FQ_QUERY, "select count(u) from org.acme.domain.User$Foo_Bar u");
	}

	@Test // DATAJPA-252
	void detectsJoinAliasesCorrectly() {

		Set<String> aliases = getOuterJoinAliases("select p from Person p left outer join x.foo b2_$ar where …");
		assertThat(aliases).hasSize(1);
		assertThat(aliases).contains("b2_$ar");

		aliases = getOuterJoinAliases("select p from Person p left join x.foo b2_$ar where …");
		assertThat(aliases).hasSize(1);
		assertThat(aliases).contains("b2_$ar");

		aliases = getOuterJoinAliases(
				"select p from Person p left outer join x.foo as b2_$ar, left join x.bar as foo where …");
		assertThat(aliases).hasSize(2);
		assertThat(aliases).contains("b2_$ar", "foo");

		aliases = getOuterJoinAliases(
				"select p from Person p left join x.foo as b2_$ar, left outer join x.bar foo where …");
		assertThat(aliases).hasSize(2);
		assertThat(aliases).contains("b2_$ar", "foo");
	}

	@Test // DATAJPA-252
	void doesNotPrefixOrderReferenceIfOuterJoinAliasDetected() {

		String query = "select p from Person p left join p.address address";
		assertThat(applySorting(query, Sort.by("address.city"))).endsWith("order by address.city asc");
		assertThat(applySorting(query, Sort.by("address.city", "lastname"), "p"))
				.endsWith("order by address.city asc, p.lastname asc");
	}

	@Test // DATAJPA-252
	void extendsExistingOrderByClausesCorrectly() {

		String query = "select p from Person p order by p.lastname asc";
		assertThat(applySorting(query, Sort.by("firstname"), "p")).endsWith("order by p.lastname asc, p.firstname asc");
	}

	@Test // DATAJPA-296
	void appliesIgnoreCaseOrderingCorrectly() {

		Sort sort = Sort.by(Order.by("firstname").ignoreCase());

		String query = "select p from Person p";
		assertThat(applySorting(query, sort, "p")).endsWith("order by lower(p.firstname) asc");
	}

	@Test // DATAJPA-296
	void appendsIgnoreCaseOrderingCorrectly() {

		Sort sort = Sort.by(Order.by("firstname").ignoreCase());

		String query = "select p from Person p order by p.lastname asc";
		assertThat(applySorting(query, sort, "p")).endsWith("order by p.lastname asc, lower(p.firstname) asc");
	}

	@Test // DATAJPA-342
	void usesReturnedVariableInCOuntProjectionIfSet() {

		assertCountQuery("select distinct m.genre from Media m where m.user = ?1 order by m.genre asc",
				"select count(distinct m.genre) from Media m where m.user = ?1");
	}

	@Test // DATAJPA-343
	void projectsCOuntQueriesForQueriesWithSubselects() {

		assertCountQuery("select o from Foo o where cb.id in (select b from Bar b)",
				"select count(o) from Foo o where cb.id in (select b from Bar b)");
	}

	@Test // DATAJPA-148
	void doesNotPrefixSortsIfFunction() {

		Sort sort = Sort.by("sum(foo)");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> applySorting("select p from Person p", sort, "p"));
	}

	@Test // GH-2348
	void removesFetchPartInJoinFetchClauseInGeneratedCountQueryIfPresent() {

		assertCountQuery("select u from User u left outer join fetch u.roles r left outer JOIN   FETCH  u.accounts a",
				"select count(u) from User u left outer join u.roles r left outer JOIN u.accounts a");
	}

	@Test // DATAJPA-377
	void removesOrderByInGeneratedCountQueryFromOriginalQueryIfPresent() {

		assertCountQuery("select distinct m.genre from Media m where m.user = ?1 OrDer  By   m.genre ASC",
				"select count(distinct m.genre) from Media m where m.user = ?1");
	}

	@Test // DATAJPA-375
	void findsExistingOrderByIndependentOfCase() {

		Sort sort = Sort.by("lastname");
		String query = applySorting("select p from Person p ORDER BY p.firstname", sort, "p");
		assertThat(query).endsWith("ORDER BY p.firstname, p.lastname asc");
	}

	@Test // DATAJPA-409
	void createsCountQueryForNestedReferenceCorrectly() {
		assertCountQuery("select a.b from A a", "select count(a.b) from A a");
	}

	@Test // DATAJPA-420
	void createsCountQueryForScalarSelects() {
		assertCountQuery("select p.lastname,p.firstname from Person p", "select count(p) from Person p");
	}

	@Test // DATAJPA-456
	void createCountQueryFromTheGivenCountProjection() {
		assertThat(createCountQueryFor("select p.lastname,p.firstname from Person p", "p.lastname"))
				.isEqualTo("select count(p.lastname) from Person p");
	}

	@Test // DATAJPA-726
	void detectsAliassesInPlainJoins() {

		String query = "select p from Customer c join c.productOrder p where p.delayed = true";
		Sort sort = Sort.by("p.lineItems");

		assertThat(applySorting(query, sort, "c")).endsWith("order by p.lineItems asc");
	}

	@Test // DATAJPA-736
	void supportsNonAsciiCharactersInEntityNames() {
		assertThat(createCountQueryFor("select u from Usèr u")).isEqualTo("select count(u) from Usèr u");
	}

	@Test // DATAJPA-798
	void detectsAliasInQueryContainingLineBreaks() {
		assertThat(detectAlias("select \n u \n from \n User \nu")).isEqualTo("u");
	}

	@Test // DATAJPA-815
	void doesPrefixPropertyWith() {

		String query = "from Cat c join Dog d";
		Sort sort = Sort.by("dPropertyStartingWithJoinAlias");

		assertThat(applySorting(query, sort, "c")).endsWith("order by c.dPropertyStartingWithJoinAlias asc");
	}

	@Test // DATAJPA-938
	void detectsConstructorExpressionInDistinctQuery() {
		assertThat(hasConstructorExpression("select distinct new Foo() from Bar b")).isTrue();
	}

	@Test // DATAJPA-938
	void detectsComplexConstructorExpression() {

		assertThat(hasConstructorExpression("select new foo.bar.Foo(ip.id, ip.name, sum(lp.amount)) " //
				+ "from Bar lp join lp.investmentProduct ip " //
				+ "where (lp.toDate is null and lp.fromDate <= :now and lp.fromDate is not null) and lp.accountId = :accountId " //
				+ "group by ip.id, ip.name, lp.accountId " //
				+ "order by ip.name ASC")).isTrue();
	}

	@Test // DATAJPA-938
	void detectsConstructorExpressionWithLineBreaks() {
		assertThat(hasConstructorExpression("select new foo.bar.FooBar(\na.id) from DtoA a ")).isTrue();
	}

	@Test // DATAJPA-960
	void doesNotQualifySortIfNoAliasDetected() {
		assertThat(applySorting("from mytable where ?1 is null", Sort.by("firstname"))).endsWith("order by firstname asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotAllowWhitespaceInSort() {

		Sort sort = Sort.by("case when foo then bar");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> applySorting("select p from Person p", sort, "p"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixUnsageJpaSortFunctionCalls() {

		JpaSort sort = JpaSort.unsafe("sum(foo)");
		assertThat(applySorting("select p from Person p", sort, "p")).endsWith("order by sum(foo) asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixMultipleAliasedFunctionCalls() {

		String query = "SELECT AVG(m.price) AS avgPrice, SUM(m.stocks) AS sumStocks FROM Magazine m";
		Sort sort = Sort.by("avgPrice", "sumStocks");

		assertThat(applySorting(query, sort, "m")).endsWith("order by avgPrice asc, sumStocks asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixSingleAliasedFunctionCalls() {

		String query = "SELECT AVG(m.price) AS avgPrice FROM Magazine m";
		Sort sort = Sort.by("avgPrice");

		assertThat(applySorting(query, sort, "m")).endsWith("order by avgPrice asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void prefixesSingleNonAliasedFunctionCallRelatedSortProperty() {

		String query = "SELECT AVG(m.price) AS avgPrice FROM Magazine m";
		Sort sort = Sort.by("someOtherProperty");

		assertThat(applySorting(query, sort, "m")).endsWith("order by m.someOtherProperty asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void prefixesNonAliasedFunctionCallRelatedSortPropertyWhenSelectClauseContainesAliasedFunctionForDifferentProperty() {

		String query = "SELECT m.name, AVG(m.price) AS avgPrice FROM Magazine m";
		Sort sort = Sort.by("name", "avgPrice");

		assertThat(applySorting(query, sort, "m")).endsWith("order by m.name asc, avgPrice asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithMultipleNumericParameters() {

		String query = "SELECT SUBSTRING(m.name, 2, 5) AS trimmedName FROM Magazine m";
		Sort sort = Sort.by("trimmedName");

		assertThat(applySorting(query, sort, "m")).endsWith("order by trimmedName asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithMultipleStringParameters() {

		String query = "SELECT CONCAT(m.name, 'foo') AS extendedName FROM Magazine m";
		Sort sort = Sort.by("extendedName");

		assertThat(applySorting(query, sort, "m")).endsWith("order by extendedName asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithUnderscores() {

		String query = "SELECT AVG(m.price) AS avg_price FROM Magazine m";
		Sort sort = Sort.by("avg_price");

		assertThat(applySorting(query, sort, "m")).endsWith("order by avg_price asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWithDots() {

		String query = "SELECT AVG(m.price) AS m.avg FROM Magazine m";
		Sort sort = Sort.by("m.avg");

		assertThat(applySorting(query, sort, "m")).endsWith("order by m.avg asc");
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixAliasedFunctionCallNameWhenQueryStringContainsMultipleWhiteSpaces() {

		String query = "SELECT  AVG(  m.price  )   AS   avgPrice   FROM Magazine   m";
		Sort sort = Sort.by("avgPrice");

		assertThat(applySorting(query, sort, "m")).endsWith("order by avgPrice asc");
	}

	@Test // DATAJPA-1000
	void discoversCorrectAliasForJoinFetch() {

		Set<String> aliases = QueryUtils
				.getOuterJoinAliases("SELECT DISTINCT user FROM User user LEFT JOIN FETCH user.authorities AS authority");

		assertThat(aliases).containsExactly("authority");
	}

	@Test // DATAJPA-1171
	void doesNotContainStaticClauseInExistsQuery() {

		assertThat(QueryUtils.getExistsQueryString("entity", "x", Collections.singleton("id"))) //
				.endsWith("WHERE x.id = :id");
	}

	@Test // DATAJPA-1363
	void discoversAliasWithComplexFunction() {

		assertThat(
				QueryUtils.getFunctionAliases("select new MyDto(sum(case when myEntity.prop3=0 then 1 else 0 end) as myAlias")) //
						.contains("myAlias");
	}

	@Test // DATAJPA-1506
	void detectsAliasWithGroupAndOrderBy() {

		assertThat(detectAlias("select * from User group by name")).isNull();
		assertThat(detectAlias("select * from User order by name")).isNull();
		assertThat(detectAlias("select * from User u group by name")).isEqualTo("u");
		assertThat(detectAlias("select * from User u order by name")).isEqualTo("u");
	}

	@Test // DATAJPA-1500
	void createCountQuerySupportsWhitespaceCharacters() {

		assertThat(createCountQueryFor("select * from User user\n" + //
				"  where user.age = 18\n" + //
				"  order by user.name\n ")).isEqualTo("select count(user) from User user\n" + //
						"  where user.age = 18\n ");
	}

	@Test
	void createCountQuerySupportsLineBreaksInSelectClause() {

		assertThat(createCountQueryFor("select user.age,\n" + //
				"  user.name\n" + //
				"  from User user\n" + //
				"  where user.age = 18\n" + //
				"  order\nby\nuser.name\n ")).isEqualTo("select count(user) from User user\n" + //
						"  where user.age = 18\n ");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForFieldAliases() {

		String query = "SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a";
		Sort sort = Sort.by("authorName");

		String fullQuery = applySorting(query, sort);

		assertThat(fullQuery).endsWith("order by authorName asc");
	}

	@Test // GH-2280
	void appliesOrderingCorrectlyForFieldAliasWithIgnoreCase() {

		String query = "SELECT customer.id as id, customer.name as name FROM CustomerEntity customer";
		Sort sort = Sort.by(Order.by("name").ignoreCase());

		String fullQuery = applySorting(query, sort);

		assertThat(fullQuery).isEqualTo(
				"SELECT customer.id as id, customer.name as name FROM CustomerEntity customer order by lower(name) asc");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForFunctionAliases() {

		String query = "SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a";
		Sort sort = Sort.by("title");

		String fullQuery = applySorting(query, sort);

		assertThat(fullQuery).endsWith("order by title asc");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForSimpleField() {

		String query = "SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a";
		Sort sort = Sort.by("price");

		String fullQuery = applySorting(query, sort);

		assertThat(fullQuery).endsWith("order by m.price asc");
	}

	@Test
	void createCountQuerySupportsLineBreakRightAfterDistinct() {

		assertThat(createCountQueryFor("select\ndistinct\nuser.age,\n" + //
				"user.name\n" + //
				"from\nUser\nuser")).isEqualTo(createCountQueryFor("select\ndistinct user.age,\n" + //
						"user.name\n" + //
						"from\nUser\nuser"));
	}

	@Test
	void detectsAliasWithGroupAndOrderByWithLineBreaks() {

		assertThat(detectAlias("select * from User group\nby name")).isNull();
		assertThat(detectAlias("select * from User order\nby name")).isNull();
		assertThat(detectAlias("select * from User u group\nby name")).isEqualTo("u");
		assertThat(detectAlias("select * from User u order\nby name")).isEqualTo("u");
		assertThat(detectAlias("select * from User\nu\norder \n by name")).isEqualTo("u");
	}

	@Test // DATAJPA-1679
	void findProjectionClauseWithDistinct() {

		SoftAssertions.assertSoftly(sofly -> {
			sofly.assertThat(QueryUtils.getProjection("select * from x")).isEqualTo("*");
			sofly.assertThat(QueryUtils.getProjection("select a, b, c from x")).isEqualTo("a, b, c");
			sofly.assertThat(QueryUtils.getProjection("select distinct a, b, c from x")).isEqualTo("a, b, c");
			sofly.assertThat(QueryUtils.getProjection("select DISTINCT a, b, c from x")).isEqualTo("a, b, c");
		});
	}

	@Test // DATAJPA-1696
	void findProjectionClauseWithSubselect() {

		// This is not a required behavior, in fact the opposite is,
		// but it documents a current limitation.
		// to fix this without breaking findProjectionClauseWithIncludedFrom we need a more sophisticated parser.
		assertThat(QueryUtils.getProjection("select * from (select x from y)")).isNotEqualTo("*");
	}

	@Test // DATAJPA-1696
	void findProjectionClauseWithIncludedFrom() {
		assertThat(QueryUtils.getProjection("select x, frommage, y from t")).isEqualTo("x, frommage, y");
	}

	private static void assertCountQuery(String originalQuery, String countQuery) {
		assertThat(createCountQueryFor(originalQuery)).isEqualTo(countQuery);
	}
}
