/*
 * Copyright 2008-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.data.jpa.repository.query.QueryUtils.applySorting;
import static org.springframework.data.jpa.repository.query.QueryUtils.createCountQueryFor;
import static org.springframework.data.jpa.repository.query.QueryUtils.detectAlias;
import static org.springframework.data.jpa.repository.query.QueryUtils.getOuterJoinAliases;
import static org.springframework.data.jpa.repository.query.QueryUtils.hasConstructorExpression;
import static org.springframework.data.jpa.repository.query.QueryUtils.removeSubqueries;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @author Jędrzej Biedrzycki
 * @author Darin Manica
 * @author Chris Fraser
 * @author Michał Pachucki
 * @author Erik Pellizzon
 */
class QueryUtilsUnitTests {

	private static final String QUERY = "select u from User u";
	private static final String FQ_QUERY = "select u from org.acme.domain.User$Foo_Bar u";
	private static final String SIMPLE_QUERY = "from User u";
	private static final String COUNT_QUERY = "select count(u) from User u";

	private static final String QUERY_WITH_AS = "select u from User as u where u.username = ?";
	private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");

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

	@Test // GH-2260
	void detectsAliasCorrectly() {

		assertThat(detectAlias(QUERY)).isEqualTo("u");
		assertThat(detectAlias(SIMPLE_QUERY)).isEqualTo("u");
		assertThat(detectAlias(COUNT_QUERY)).isEqualTo("u");
		assertThat(detectAlias(QUERY_WITH_AS)).isEqualTo("u");
		assertThat(detectAlias("SELECT FROM USER U")).isEqualTo("U");
		assertThat(detectAlias("select u from  User u")).isEqualTo("u");
		assertThat(detectAlias("select u from  com.acme.User u")).isEqualTo("u");
		assertThat(detectAlias("select u from T05User u")).isEqualTo("u");
		assertThat(detectAlias("select u from User u where not exists (from User u2)")).isEqualTo("u");
		assertThat(detectAlias("(select u from User u where not exists (from User u2))")).isEqualTo("u");
		assertThat(detectAlias("(select u from User u where not exists ((from User u2 where not exists (from User u3))))"))
				.isEqualTo("u");
		assertThat(detectAlias(
				"from Foo f left join f.bar b with type(b) = BarChild where (f.id = (select max(f.id) from Foo f2 where type(f2) = FooChild) or 1 <> 1) and 1=1"))
						.isEqualTo("f");
		assertThat(detectAlias(
				"(from Foo f max(f) ((((select * from Foo f2 (from Foo f3) max(*)) (from Foo f4)) max(f5)) (f6)) (from Foo f7))"))
						.isEqualTo("f");
		assertThat(detectAlias(
				"SELECT e FROM DbEvent e WHERE (CAST(:modifiedFrom AS date) IS NULL OR e.modificationDate >= :modifiedFrom)"))
						.isEqualTo("e");
		assertThat(detectAlias("from User u where (cast(:effective as date) is null) OR :effective >= u.createdAt"))
				.isEqualTo("u");
		assertThat(detectAlias("from User u where (cast(:effectiveDate as date) is null) OR :effectiveDate >= u.createdAt"))
				.isEqualTo("u");
		assertThat(detectAlias("from User u where (cast(:effectiveFrom as date) is null) OR :effectiveFrom >= u.createdAt"))
				.isEqualTo("u");
		assertThat(
				detectAlias("from User u where (cast(:e1f2f3ectiveFrom as date) is null) OR :effectiveFrom >= u.createdAt"))
						.isEqualTo("u");
	}

	@Test // GH-2260
	void testRemoveSubqueries() throws Exception {

		// boundary conditions
		assertThat(removeSubqueries(null)).isNull();
		assertThat(removeSubqueries("")).isEmpty();
		assertThat(removeSubqueries(" ")).isEqualTo(" ");
		assertThat(removeSubqueries("(")).isEqualTo("(");
		assertThat(removeSubqueries(")")).isEqualTo(")");
		assertThat(removeSubqueries("(()")).isEqualTo("(()");
		assertThat(removeSubqueries("())")).isEqualTo("())");

		// realistic conditions
		assertThat(removeSubqueries(QUERY)).isEqualTo(QUERY);
		assertThat(removeSubqueries(SIMPLE_QUERY)).isEqualTo(SIMPLE_QUERY);
		assertThat(removeSubqueries(COUNT_QUERY)).isEqualTo(COUNT_QUERY);
		assertThat(removeSubqueries(QUERY_WITH_AS)).isEqualTo(QUERY_WITH_AS);
		assertThat(removeSubqueries("SELECT FROM USER U")).isEqualTo("SELECT FROM USER U");
		assertThat(removeSubqueries("select u from  User u")).isEqualTo("select u from  User u");
		assertThat(removeSubqueries("select u from  com.acme.User u")).isEqualTo("select u from  com.acme.User u");
		assertThat(removeSubqueries("select u from T05User u")).isEqualTo("select u from T05User u");
		assertThat(normalizeWhitespace(removeSubqueries("select u from User u where not exists (from User u2)")))
				.isEqualTo("select u from User u where not exists");
		assertThat(normalizeWhitespace(removeSubqueries("(select u from User u where not exists (from User u2))")))
				.isEqualTo("(select u from User u where not exists )");
		assertThat(normalizeWhitespace(
				removeSubqueries("select u from User u where not exists (from User u2 where not exists (from User u3))")))
						.isEqualTo("select u from User u where not exists");
		assertThat(normalizeWhitespace(
				removeSubqueries("select u from User u where not exists ((from User u2 where not exists (from User u3)))")))
						.isEqualTo("select u from User u where not exists ( )");
		assertThat(normalizeWhitespace(
				removeSubqueries("(select u from User u where not exists ((from User u2 where not exists (from User u3))))")))
						.isEqualTo("(select u from User u where not exists ( ))");
	}

	@Test // GH-2581
	void testRemoveMultilineSubqueries() {

		assertThat(normalizeWhitespace(removeSubqueries("select u from User u\n" //
				+ "    where not exists (\n" //
				+ "        from User u2\n" //
				+ "    )"))).isEqualTo("select u from User u where not exists");

		assertThat(normalizeWhitespace(removeSubqueries("(\n" //
				+ "    select u from User u \n" //
				+ "        where not exists (\n" //
				+ "            from User u2\n" //
				+ "        )\n" //
				+ ")"))).isEqualTo("( select u from User u where not exists )");

		assertThat(normalizeWhitespace(removeSubqueries("select u from User u \n" //
				+ "    where not exists (\n" //
				+ "        from User u2 \n" //
				+ "            where not exists (\n" //
				+ "                from User u3\n" //
				+ "            )\n" //
				+ "    )"))).isEqualTo("select u from User u where not exists");

		assertThat(normalizeWhitespace(removeSubqueries("select u from User u \n" //
				+ "    where not exists (\n" //
				+ "        (\n" //
				+ "            from User u2 \n" //
				+ "                where not exists (\n" //
				+ "                    from User u3\n" //
				+ "                )\n" //
				+ "        )\n" //
				+ "    )"))).isEqualTo("select u from User u where not exists ( )");

		assertThat(normalizeWhitespace(removeSubqueries("(\n" //
				+ "    select u from User u \n" //
				+ "        where not exists (\n" //
				+ "            (\n" //
				+ "                from User u2 \n" //
				+ "                    where not exists (\n" //
				+ "                        from User u3\n" //
				+ "                    )\n" //
				+ "            )\n" //
				+ "        )\n" //
				+ ")"))).isEqualTo("( select u from User u where not exists ( ) )");
	}

	@Test // GH-2557
	void applySortingAccountsForNewlinesInSubselect() {

		Sort sort = Sort.by(Order.desc("age"));

		assertThat(QueryUtils.applySorting("select u\n" + //
				"from user u\n" + //
				"where exists (select u2\n" + //
				"from user u2\n" + //
				")\n" + //
				"", sort)).isEqualTo("select u\n" + //
						"from user u\n" + //
						"where exists (select u2\n" + //
						"from user u2\n" + //
						")\n" + //
						" order by u.age desc");
	}

	@Test // GH-2563
	void aliasDetectionProperlyHandlesNewlinesInSubselects() {

		assertThat(detectAlias("SELECT o\n" + //
				"FROM Order o\n" + //
				"AND EXISTS(SELECT 1\n" + //
				"FROM Vehicle vehicle\n" + //
				"WHERE vehicle.vehicleOrderId = o.id\n" + //
				"AND LOWER(COALESCE(vehicle.make, '')) LIKE :query)")).isEqualTo("o");
	}

	private String normalizeWhitespace(String s) {

		Matcher matcher = MULTI_WHITESPACE.matcher(s);

		if (matcher.find()) {
			return matcher.replaceAll(" ").trim();
		}

		return s.strip();
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
	void usesReturnedVariableInCountProjectionIfSet() {

		assertCountQuery("select distinct m.genre from Media m where m.user = ?1 order by m.genre asc",
				"select count(distinct m.genre) from Media m where m.user = ?1");
	}

	@Test // DATAJPA-343
	void projectsCountQueriesForQueriesWithSubselects() {

		assertCountQuery("select o from Foo o where cb.id in (select b from Bar b)",
				"select count(o) from Foo o where cb.id in (select b from Bar b)");
	}

	@Test // DATAJPA-148
	void doesNotPrefixSortsIfFunction() {

		Sort sort = Sort.by("sum(foo)");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> applySorting("select p from Person p", sort, "p"));
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
	void detectsAliasesInPlainJoins() {

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
	void doesNotPrefixUnsafeJpaSortFunctionCalls() {

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
	void prefixesNonAliasedFunctionCallRelatedSortPropertyWhenSelectClauseContainsAliasedFunctionForDifferentProperty() {

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

	@Test // GH-2341
	void createCountQueryStarCharacterConverted() {
		assertThat(createCountQueryFor("select * from User user")).isEqualTo("select count(user) from User user");
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

	@Test // GH-2341
	void countProjectionDistrinctQueryIncludesNewLineAfterFromAndBeforeJoin() {
		String originalQuery = "SELECT DISTINCT entity1\nFROM Entity1 entity1\nLEFT JOIN Entity2 entity2 ON entity1.key = entity2.key";

		assertCountQuery(originalQuery,
				"select count(DISTINCT entity1) FROM Entity1 entity1\nLEFT JOIN Entity2 entity2 ON entity1.key = entity2.key");
	}

	@Test // GH-2341
	void countProjectionDistinctQueryIncludesNewLineAfterEntity() {
		String originalQuery = "SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key";
		assertCountQuery(originalQuery,
				"select count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key");
	}

	@Test // GH-2341
	void countProjectionDistinctQueryIncludesNewLineAfterEntityAndBeforeWhere() {
		String originalQuery = "SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key\nwhere entity1.id = 1799";
		assertCountQuery(originalQuery,
				"select count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN Entity2 entity2 ON entity1.key = entity2.key\nwhere entity1.id = 1799");
	}

	@Test // GH-2393
	void createCountQueryStartsWithWhitespace() {

		assertThat(createCountQueryFor(" \nselect * from User u where u.age > :age"))
				.isEqualTo("select count(u) from User u where u.age > :age");

		assertThat(createCountQueryFor("  \nselect u from User u where u.age > :age"))
				.isEqualTo("select count(u) from User u where u.age > :age");
	}

	private static void assertCountQuery(String originalQuery, String countQuery) {
		assertThat(createCountQueryFor(originalQuery)).isEqualTo(countQuery);
	}

	@Test // GH-2260
	void applySortingAccountsForNativeWindowFunction() {

		Sort sort = Sort.by(Order.desc("age"));

		// order by absent
		assertThat(QueryUtils.applySorting("select * from user u", sort))
				.isEqualTo("select * from user u order by u.age desc");

		// order by present
		assertThat(QueryUtils.applySorting("select * from user u order by u.lastname", sort))
				.isEqualTo("select * from user u order by u.lastname, u.age desc");

		// partition by
		assertThat(QueryUtils.applySorting("select dense_rank() over (partition by age) from user u", sort))
				.isEqualTo("select dense_rank() over (partition by age) from user u order by u.age desc");

		// order by in over clause
		assertThat(QueryUtils.applySorting("select dense_rank() over (order by lastname) from user u", sort))
				.isEqualTo("select dense_rank() over (order by lastname) from user u order by u.age desc");

		// order by in over clause (additional spaces)
		assertThat(QueryUtils.applySorting("select dense_rank() over ( order by lastname ) from user u", sort))
				.isEqualTo("select dense_rank() over ( order by lastname ) from user u order by u.age desc");

		// order by in over clause + at the end
		assertThat(
				QueryUtils.applySorting("select dense_rank() over (order by lastname) from user u order by u.lastname", sort))
						.isEqualTo("select dense_rank() over (order by lastname) from user u order by u.lastname, u.age desc");

		// partition by + order by in over clause
		assertThat(QueryUtils.applySorting(
				"select dense_rank() over (partition by active, age order by lastname) from user u", sort)).isEqualTo(
						"select dense_rank() over (partition by active, age order by lastname) from user u order by u.age desc");

		// partition by + order by in over clause + order by at the end
		assertThat(QueryUtils.applySorting(
				"select dense_rank() over (partition by active, age order by lastname) from user u order by active", sort))
						.isEqualTo(
								"select dense_rank() over (partition by active, age order by lastname) from user u order by active, u.age desc");

		// partition by + order by in over clause + frame clause
		assertThat(QueryUtils.applySorting(
				"select dense_rank() over ( partition by active, age order by username rows between current row and unbounded following ) from user u",
				sort)).isEqualTo(
						"select dense_rank() over ( partition by active, age order by username rows between current row and unbounded following ) from user u order by u.age desc");

		// partition by + order by in over clause + frame clause + order by at the end
		assertThat(QueryUtils.applySorting(
				"select dense_rank() over ( partition by active, age order by username rows between current row and unbounded following ) from user u order by active",
				sort)).isEqualTo(
						"select dense_rank() over ( partition by active, age order by username rows between current row and unbounded following ) from user u order by active, u.age desc");

		// order by in subselect (select expression)
		assertThat(
				QueryUtils.applySorting("select lastname, (select i.id from item i order by i.id limit 1) from user u", sort))
						.isEqualTo(
								"select lastname, (select i.id from item i order by i.id limit 1) from user u order by u.age desc");

		// order by in subselect (select expression) + at the end
		assertThat(QueryUtils.applySorting(
				"select lastname, (select i.id from item i order by 1 limit 1) from user u order by active", sort)).isEqualTo(
						"select lastname, (select i.id from item i order by 1 limit 1) from user u order by active, u.age desc");

		// order by in subselect (from expression)
		assertThat(QueryUtils.applySorting("select * from (select * from user order by age desc limit 10) u", sort))
				.isEqualTo("select * from (select * from user order by age desc limit 10) u order by age desc");

		// order by in subselect (from expression) + at the end
		assertThat(QueryUtils.applySorting(
				"select * from (select * from user order by 1, 2, 3 desc limit 10) u order by u.active asc", sort)).isEqualTo(
						"select * from (select * from user order by 1, 2, 3 desc limit 10) u order by u.active asc, age desc");
	}

	@Test // GH-2511
	void countQueryUsesCorrectVariable() {

		String countQueryFor = createCountQueryFor("SELECT * FROM User WHERE created_at > $1");
		assertThat(countQueryFor).isEqualTo("select count(*) FROM User WHERE created_at > $1");

		countQueryFor = createCountQueryFor(
				"SELECT * FROM mytable WHERE nr = :number AND kon = :kon AND datum >= '2019-01-01'");
		assertThat(countQueryFor)
				.isEqualTo("select count(*) FROM mytable WHERE nr = :number AND kon = :kon AND datum >= '2019-01-01'");

		countQueryFor = createCountQueryFor("SELECT * FROM context ORDER BY time");
		assertThat(countQueryFor).isEqualTo("select count(*) FROM context");

		countQueryFor = createCountQueryFor("select * FROM users_statuses WHERE (user_created_at BETWEEN $1 AND $2)");
		assertThat(countQueryFor)
				.isEqualTo("select count(*) FROM users_statuses WHERE (user_created_at BETWEEN $1 AND $2)");

		countQueryFor = createCountQueryFor(
				"SELECT * FROM users_statuses us WHERE (user_created_at BETWEEN :fromDate AND :toDate)");
		assertThat(countQueryFor)
				.isEqualTo("select count(us) FROM users_statuses us WHERE (user_created_at BETWEEN :fromDate AND :toDate)");
	}

	@Test // GH-2496, GH-2522, GH-2537, GH-2045
	void orderByShouldWorkWithSubSelectStatements() {

		Sort sort = Sort.by(Order.desc("age"));

		assertThat(QueryUtils.applySorting("SELECT\n" //
				+ "   foo_bar.*\n" //
				+ "FROM\n" //
				+ "    foo foo\n" //
				+ "INNER JOIN\n" //
				+ "   foo_bar_dnrmv foo_bar ON\n" //
				+ "   foo_bar.foo_id = foo.foo_id\n" //
				+ "INNER JOIN\n" //
				+ " (\n" //
				+ "  SELECT\n" //
				+ "       foo_bar_action.*,\n" //
				+ "       RANK() OVER (PARTITION BY \"foo_bar_action\".attributes->>'baz' ORDER BY \"foo_bar_action\".attributes->>'qux' DESC) AS ranking\n" //
				+ "  FROM\n" //
				+ "      foo_bar_action\n" //
				+ "  WHERE\n" //
				+ "       foo_bar_action.deleted_ts IS NULL)\n" //
				+ "    foo_bar_action ON\n" //
				+ "  foo_bar.foo_bar_id = foo_bar_action.foo_bar_id\n" //
				+ "  AND ranking = 1\n" //
				+ "INNER JOIN\n" //
				+ "  bar bar ON\n" //
				+ "  foo_bar.bar_id = bar.bar_id\n" //
				+ "INNER JOIN\n" //
				+ "  bar_metadata bar_metadata ON\n" //
				+ "  bar.bar_metadata_key = bar_metadata.bar_metadata_key\n" //
				+ "WHERE\n" //
				+ "  foo.tenant_id =:tenantId\n" //
				+ "AND (foo.attributes ->> :serialNum IN (:serialNumValue))", sort)).endsWith("order by foo.age desc");

		assertThat(QueryUtils.applySorting("select r " //
				+ "From DataRecord r " //
				+ "where " //
				+ " ( " //
				+ "       r.adusrId = :userId " //
				+ "       or EXISTS( select 1 FROM DataRecordDvsRight dr WHERE dr.adusrId = :userId AND dr.dataRecord = r ) " //
				+ ")", sort)).endsWith("order by r.age desc");

		assertThat(QueryUtils.applySorting("select distinct u " //
				+ "from FooBar u " //
				+ "where [REDACTED] " //
				+ "and (" //
				+ "		not exists (" //
				+ "				from FooBarGroup group " //
				+ "				where group in :excludedGroups " //
				+ "				and group in elements(u.groups)" //
				+ "		)" //
				+ ")", sort)).endsWith("order by u.age desc");

		assertThat(QueryUtils.applySorting("SELECT i " //
				+ "FROM Item i " //
				+ "FETCH ALL PROPERTIES \" " //
				+ "+ \"WHERE i.id IN (\" " //
				+ "+ \"SELECT max(i2.id) FROM Item i2 \" " //
				+ "+ \"WHERE i2.field.id = :fieldId \" " //
				+ "+ \"GROUP BY i2.field.id, i2.version)", sort)).endsWith("order by i.age desc");

		assertThat(QueryUtils.applySorting("select \n" //
				+ " f.id,\n" //
				+ " (\n" //
				+ "  select timestamp from bar\n" //
				+ "  where date(bar.timestamp) > '2022-05-21'\n" //
				+ "  and bar.foo_id = f.id \n" //
				+ "  order by date(bar.timestamp) desc\n" //
				+ "  limit 1\n" //
				+ ") as timestamp\n" //
				+ "from foo f", sort)).endsWith("order by f.age desc");
	}
}
