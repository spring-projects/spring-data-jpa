/*
 * Copyright 2008-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.data.jpa.repository.query.QueryUtils.*;

import java.util.Collections;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Test;
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
 */
public class QueryUtilsUnitTests {

	static final String QUERY = "select u from User u";
	static final String FQ_QUERY = "select u from org.acme.domain.User$Foo_Bar u";
	static final String SIMPLE_QUERY = "from User u";
	static final String COUNT_QUERY = "select count(u) from User u";

	static final String QUERY_WITH_AS = "select u from User as u where u.username = ?";

	static final Matcher<String> IS_U = is("u");

	@Test
	public void createsCountQueryCorrectly() throws Exception {
		assertCountQuery(QUERY, COUNT_QUERY);
	}

	@Test
	public void createsCountQueriesCorrectlyForCapitalLetterJPQL() {

		assertCountQuery("FROM User u WHERE u.foo.bar = ?", "select count(u) FROM User u WHERE u.foo.bar = ?");

		assertCountQuery("SELECT u FROM User u where u.foo.bar = ?", "select count(u) FROM User u where u.foo.bar = ?");
	}

	@Test
	public void createsCountQueryForDistinctQueries() throws Exception {

		assertCountQuery("select distinct u from User u where u.foo = ?",
				"select count(distinct u) from User u where u.foo = ?");
	}

	@Test
	public void createsCountQueryForConstructorQueries() throws Exception {

		assertCountQuery("select distinct new User(u.name) from User u where u.foo = ?",
				"select count(distinct u) from User u where u.foo = ?");
	}

	@Test
	public void createsCountQueryForJoins() throws Exception {

		assertCountQuery("select distinct new User(u.name) from User u left outer join u.roles r WHERE r = ?",
				"select count(distinct u) from User u left outer join u.roles r WHERE r = ?");
	}

	@Test
	public void createsCountQueryForQueriesWithSubSelects() throws Exception {

		assertCountQuery("select u from User u left outer join u.roles r where r in (select r from Role)",
				"select count(u) from User u left outer join u.roles r where r in (select r from Role)");
	}

	@Test
	public void createsCountQueryForAliasesCorrectly() throws Exception {

		assertCountQuery("select u from User as u", "select count(u) from User as u");
	}

	@Test
	public void allowsShortJpaSyntax() throws Exception {

		assertCountQuery(SIMPLE_QUERY, COUNT_QUERY);
	}

	@Test
	public void detectsAliasCorrectly() throws Exception {

		assertThat(detectAlias(QUERY), IS_U);
		assertThat(detectAlias(SIMPLE_QUERY), IS_U);
		assertThat(detectAlias(COUNT_QUERY), IS_U);
		assertThat(detectAlias(QUERY_WITH_AS), IS_U);
		assertThat(detectAlias("SELECT FROM USER U"), is("U"));
		assertThat(detectAlias("select u from  User u"), IS_U);
		assertThat(detectAlias("select u from  com.acme.User u"), IS_U);
		assertThat(detectAlias("select u from T05User u"), IS_U);
	}

	@Test
	public void allowsFullyQualifiedEntityNamesInQuery() {

		assertThat(detectAlias(FQ_QUERY), IS_U);
		assertCountQuery(FQ_QUERY, "select count(u) from org.acme.domain.User$Foo_Bar u");
	}

	@Test // DATAJPA-252
	public void detectsJoinAliasesCorrectly() {

		Set<String> aliases = getOuterJoinAliases("select p from Person p left outer join x.foo b2_$ar where …");
		assertThat(aliases, hasSize(1));
		assertThat(aliases, hasItems("b2_$ar"));

		aliases = getOuterJoinAliases("select p from Person p left join x.foo b2_$ar where …");
		assertThat(aliases, hasSize(1));
		assertThat(aliases, hasItems("b2_$ar"));

		aliases = getOuterJoinAliases(
				"select p from Person p left outer join x.foo as b2_$ar, left join x.bar as foo where …");
		assertThat(aliases, hasSize(2));
		assertThat(aliases, hasItems("b2_$ar", "foo"));

		aliases = getOuterJoinAliases(
				"select p from Person p left join x.foo as b2_$ar, left outer join x.bar foo where …");
		assertThat(aliases, hasSize(2));
		assertThat(aliases, hasItems("b2_$ar", "foo"));
	}

	@Test // DATAJPA-252
	public void doesNotPrefixOrderReferenceIfOuterJoinAliasDetected() {

		String query = "select p from Person p left join p.address address";
		assertThat(applySorting(query, Sort.by("address.city")), endsWith("order by address.city asc"));
		assertThat(applySorting(query, Sort.by("address.city", "lastname"), "p"),
				endsWith("order by address.city asc, p.lastname asc"));
	}

	@Test // DATAJPA-252
	public void extendsExistingOrderByClausesCorrectly() {

		String query = "select p from Person p order by p.lastname asc";
		assertThat(applySorting(query, Sort.by("firstname"), "p"), endsWith("order by p.lastname asc, p.firstname asc"));
	}

	@Test // DATAJPA-296
	public void appliesIgnoreCaseOrderingCorrectly() {

		Sort sort = Sort.by(Order.by("firstname").ignoreCase());

		String query = "select p from Person p";
		assertThat(applySorting(query, sort, "p"), endsWith("order by lower(p.firstname) asc"));
	}

	@Test // DATAJPA-296
	public void appendsIgnoreCaseOrderingCorrectly() {

		Sort sort = Sort.by(Order.by("firstname").ignoreCase());

		String query = "select p from Person p order by p.lastname asc";
		assertThat(applySorting(query, sort, "p"), endsWith("order by p.lastname asc, lower(p.firstname) asc"));
	}

	@Test // DATAJPA-342
	public void usesReturnedVariableInCOuntProjectionIfSet() {

		assertCountQuery("select distinct m.genre from Media m where m.user = ?1 order by m.genre asc",
				"select count(distinct m.genre) from Media m where m.user = ?1");
	}

	@Test // DATAJPA-343
	public void projectsCOuntQueriesForQueriesWithSubselects() {

		assertCountQuery("select o from Foo o where cb.id in (select b from Bar b)",
				"select count(o) from Foo o where cb.id in (select b from Bar b)");
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATAJPA-148
	public void doesNotPrefixSortsIfFunction() {

		Sort sort = Sort.by("sum(foo)");
		assertThat(applySorting("select p from Person p", sort, "p"), endsWith("order by sum(foo) asc"));
	}

	@Test // DATAJPA-377
	public void removesOrderByInGeneratedCountQueryFromOriginalQueryIfPresent() {

		assertCountQuery("select distinct m.genre from Media m where m.user = ?1 OrDer  By   m.genre ASC",
				"select count(distinct m.genre) from Media m where m.user = ?1");
	}

	@Test // DATAJPA-375
	public void findsExistingOrderByIndependentOfCase() {

		Sort sort = Sort.by("lastname");
		String query = applySorting("select p from Person p ORDER BY p.firstname", sort, "p");
		assertThat(query, endsWith("ORDER BY p.firstname, p.lastname asc"));
	}

	@Test // DATAJPA-409
	public void createsCountQueryForNestedReferenceCorrectly() {
		assertCountQuery("select a.b from A a", "select count(a.b) from A a");
	}

	@Test // DATAJPA-420
	public void createsCountQueryForScalarSelects() {
		assertCountQuery("select p.lastname,p.firstname from Person p", "select count(p) from Person p");
	}

	@Test // DATAJPA-456
	public void createCountQueryFromTheGivenCountProjection() {
		assertThat(createCountQueryFor("select p.lastname,p.firstname from Person p", "p.lastname"),
				is("select count(p.lastname) from Person p"));
	}

	@Test // DATAJPA-726
	public void detectsAliassesInPlainJoins() {

		String query = "select p from Customer c join c.productOrder p where p.delayed = true";
		Sort sort = Sort.by("p.lineItems");

		assertThat(applySorting(query, sort, "c"), endsWith("order by p.lineItems asc"));
	}

	@Test // DATAJPA-736
	public void supportsNonAsciiCharactersInEntityNames() {
		assertThat(createCountQueryFor("select u from Usèr u"), is("select count(u) from Usèr u"));
	}

	@Test // DATAJPA-798
	public void detectsAliasInQueryContainingLineBreaks() {
		assertThat(detectAlias("select \n u \n from \n User \nu"), is("u"));
	}

	@Test // DATAJPA-815
	public void doesPrefixPropertyWith() {

		String query = "from Cat c join Dog d";
		Sort sort = Sort.by("dPropertyStartingWithJoinAlias");

		assertThat(applySorting(query, sort, "c"), endsWith("order by c.dPropertyStartingWithJoinAlias asc"));
	}

	@Test // DATAJPA-938
	public void detectsConstructorExpressionInDistinctQuery() {
		assertThat(hasConstructorExpression("select distinct new Foo() from Bar b"), is(true));
	}

	@Test // DATAJPA-938
	public void detectsComplexConstructorExpression() {

		assertThat(hasConstructorExpression("select new foo.bar.Foo(ip.id, ip.name, sum(lp.amount)) " //
				+ "from Bar lp join lp.investmentProduct ip " //
				+ "where (lp.toDate is null and lp.fromDate <= :now and lp.fromDate is not null) and lp.accountId = :accountId " //
				+ "group by ip.id, ip.name, lp.accountId " //
				+ "order by ip.name ASC"), is(true));
	}

	@Test // DATAJPA-938
	public void detectsConstructorExpressionWithLineBreaks() {
		assertThat(hasConstructorExpression("select new foo.bar.FooBar(\na.id) from DtoA a "), is(true));
	}

	@Test // DATAJPA-960
	public void doesNotQualifySortIfNoAliasDetected() {
		assertThat(applySorting("from mytable where ?1 is null", Sort.by("firstname")), endsWith("order by firstname asc"));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATAJPA-965, DATAJPA-970
	public void doesNotAllowWhitespaceInSort() {

		Sort sort = Sort.by("case when foo then bar");
		applySorting("select p from Person p", sort, "p");
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void doesNotPrefixUnsageJpaSortFunctionCalls() {

		JpaSort sort = JpaSort.unsafe("sum(foo)");
		assertThat(applySorting("select p from Person p", sort, "p"), endsWith("order by sum(foo) asc"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void doesNotPrefixMultipleAliasedFunctionCalls() {

		String query = "SELECT AVG(m.price) AS avgPrice, SUM(m.stocks) AS sumStocks FROM Magazine m";
		Sort sort = Sort.by("avgPrice", "sumStocks");

		assertThat(applySorting(query, sort, "m"), endsWith("order by avgPrice asc, sumStocks asc"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void doesNotPrefixSingleAliasedFunctionCalls() {

		String query = "SELECT AVG(m.price) AS avgPrice FROM Magazine m";
		Sort sort = Sort.by("avgPrice");

		assertThat(applySorting(query, sort, "m"), endsWith("order by avgPrice asc"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void prefixesSingleNonAliasedFunctionCallRelatedSortProperty() {

		String query = "SELECT AVG(m.price) AS avgPrice FROM Magazine m";
		Sort sort = Sort.by("someOtherProperty");

		assertThat(applySorting(query, sort, "m"), endsWith("order by m.someOtherProperty asc"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void prefixesNonAliasedFunctionCallRelatedSortPropertyWhenSelectClauseContainesAliasedFunctionForDifferentProperty() {

		String query = "SELECT m.name, AVG(m.price) AS avgPrice FROM Magazine m";
		Sort sort = Sort.by("name", "avgPrice");

		assertThat(applySorting(query, sort, "m"), endsWith("order by m.name asc, avgPrice asc"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void doesNotPrefixAliasedFunctionCallNameWithMultipleNumericParameters() {

		String query = "SELECT SUBSTRING(m.name, 2, 5) AS trimmedName FROM Magazine m";
		Sort sort = Sort.by("trimmedName");

		assertThat(applySorting(query, sort, "m"), endsWith("order by trimmedName asc"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void doesNotPrefixAliasedFunctionCallNameWithMultipleStringParameters() {

		String query = "SELECT CONCAT(m.name, 'foo') AS extendedName FROM Magazine m";
		Sort sort = Sort.by("extendedName");

		assertThat(applySorting(query, sort, "m"), endsWith("order by extendedName asc"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void doesNotPrefixAliasedFunctionCallNameWithUnderscores() {

		String query = "SELECT AVG(m.price) AS avg_price FROM Magazine m";
		Sort sort = Sort.by("avg_price");

		assertThat(applySorting(query, sort, "m"), endsWith("order by avg_price asc"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void doesNotPrefixAliasedFunctionCallNameWithDots() {

		String query = "SELECT AVG(m.price) AS m.avg FROM Magazine m";
		Sort sort = Sort.by("m.avg");

		assertThat(applySorting(query, sort, "m"), endsWith("order by m.avg asc"));
	}

	@Test // DATAJPA-965, DATAJPA-970
	public void doesNotPrefixAliasedFunctionCallNameWhenQueryStringContainsMultipleWhiteSpaces() {

		String query = "SELECT  AVG(  m.price  )   AS   avgPrice   FROM Magazine   m";
		Sort sort = Sort.by("avgPrice");

		assertThat(applySorting(query, sort, "m"), endsWith("order by avgPrice asc"));
	}

	@Test // DATAJPA-1000
	public void discoversCorrectAliasForJoinFetch() {

		Set<String> aliases = QueryUtils
				.getOuterJoinAliases("SELECT DISTINCT user FROM User user LEFT JOIN FETCH user.authorities AS authority");

		assertThat(aliases, contains("authority"));
	}

	@Test // DATAJPA-1171
	public void doesNotContainStaticClauseInExistsQuery() {

		assertThat(QueryUtils.getExistsQueryString("entity", "x", Collections.singleton("id"))) //
				.endsWith("WHERE x.id = :id");
	}

	@Test // DATAJPA-1363
	public void discoversAliasWithComplexFunction() {

		assertThat(
				QueryUtils.getFunctionAliases("select new MyDto(sum(case when myEntity.prop3=0 then 1 else 0 end) as myAlias")) //
						.contains("myAlias");
	}

	private static void assertCountQuery(String originalQuery, String countQuery) {
		assertThat(createCountQueryFor(originalQuery), is(countQuery));
	}
}
