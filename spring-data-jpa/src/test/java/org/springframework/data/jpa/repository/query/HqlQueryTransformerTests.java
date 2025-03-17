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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.util.StringUtils;

/**
 * Verify that HQL queries are properly transformed through the {@link JpaQueryEnhancer} and the
 * {@link JpaQueryEnhancer.HqlQueryParser}.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class HqlQueryTransformerTests {

	private static final String QUERY = "select u from User u";
	private static final String SIMPLE_QUERY = "from User u";
	private static final String COUNT_QUERY = "select count(u) from User u";
	private static final String QUERY_WITH_AS = "select u from User as u where u.username = ?1";

	@Test
	void applyingSortShouldIntroduceOrderByCriteriaWhereNoneExists() {

		// given
		var original = "SELECT e FROM Employee e where e.name = :name";
		var sort = Sort.by("first_name", "last_name");

		// when
		var results = createQueryFor(original, sort);

		// then
		assertThat(original).doesNotContainIgnoringCase("order by");
		assertThat(results).contains("order by e.first_name asc, e.last_name asc");
	}

	@Test
	void applyingSortShouldCreateAdditionalOrderByCriteria() {

		// given
		var original = "SELECT e FROM Employee e where e.name = :name ORDER BY e.role, e.hire_date";
		var sort = Sort.by("first_name", "last_name");

		// when
		var results = createQueryFor(original, sort);

		// then
		assertThat(results).contains("ORDER BY e.role, e.hire_date, e.first_name asc, e.last_name asc");
	}

	@Test // GH-1280
	void nullFirstLastSorting() {

		// given
		var original = "SELECT e FROM Employee e where e.name = :name ORDER BY e.first_name asc NULLS FIRST";

		assertThat(createQueryFor(original, Sort.unsorted())).isEqualTo(original);

		assertThat(createQueryFor(original, Sort.by(Order.desc("lastName").nullsLast()))).startsWith(original)
				.endsWithIgnoringCase("e.lastName DESC NULLS LAST");

		assertThat(createQueryFor(original, Sort.by(Order.desc("lastName").nullsFirst()))).startsWith(original)
				.endsWithIgnoringCase("e.lastName DESC NULLS FIRST");
	}

	@Test
	void applyCountToSimpleQuery() {

		// given
		var original = "FROM Employee e where e.name = :name";

		// when
		var results = createCountQueryFor(original);

		// then
		assertThat(results).isEqualTo("select count(e) FROM Employee e where e.name = :name");
	}

	@Test // GH-3536
	void shouldCreateCountQueryForDistinctCount() {

		// given
		var original = """
				select distinct cast(e.timestampField as date) as foo
				from ExampleEntity e
				order by cast(e.timestampField as date) desc
				""";

		// when
		var results = createCountQueryFor(original);

		// then
		assertThat(results).isEqualTo("select count(distinct cast(e.timestampField as date)) from ExampleEntity e");
	}

	@Test
	void applyCountToMoreComplexQuery() {

		// given
		var original = "SELECT e FROM Employee e where e.name = :name ORDER BY e.modified_date";

		// when
		var results = createCountQueryFor(original);

		// then
		assertThat(results).isEqualTo("SELECT count(e) FROM Employee e where e.name = :name");
	}

	@Test
	void applyCountToAlreadySortedQuery() {

		// given
		var original = "SELECT e FROM Employee e where e.name = :name ORDER BY e.modified_date";

		// when
		var results = createCountQueryFor(original);

		// then
		assertThat(results).isEqualTo("SELECT count(e) FROM Employee e where e.name = :name");
	}

	@Test // GH-3726
	void shouldCreateCountQueryForCTE() {

		// given
		var original = """
				WITH cte_select AS (select u.firstname as firstname, u.lastname as lastname from User u)
						SELECT new org.springframework.data.jpa.repository.sample.UserExcerptDto(c.firstname, c.lastname)
						FROM cte_select c
				""";

		// when
		var results = createCountQueryFor(original);

		// then
		assertThat(results).isEqualToIgnoringWhitespace(
				"WITH cte_select AS (select u.firstname as firstname, u.lastname as lastname from User u) SELECT count(*) FROM cte_select c");
	}

	@Test
	void multipleAliasesShouldBeGathered() {

		// given
		var original = "select e from Employee e join e.manager m";

		// when
		var results = createQueryFor(original, Sort.unsorted());

		// then
		assertThat(results).isEqualTo("select e from Employee e join e.manager m");
	}

	@Test
	void createsCountQueryCorrectly() {

		assertCountQuery("SELECT id FROM Person", "SELECT count(id) FROM Person");
		assertCountQuery("SELECT p.id FROM Person p", "SELECT count(p) FROM Person p");
		assertCountQuery("SELECT id FROM Person p", "SELECT count(id) FROM Person p");
		assertCountQuery("SELECT id, name FROM Person", "SELECT count(*) FROM Person");
		assertCountQuery("SELECT id, name FROM Person p", "SELECT count(p) FROM Person p");
		assertCountQuery(QUERY, COUNT_QUERY);
	}

	@Test
	void createsCountQueriesCorrectlyForCapitalLetterHQL() {

		assertCountQuery("select u FROM User u WHERE u.foo.bar = ?1", "select count(u) FROM User u WHERE u.foo.bar = ?1");
		assertCountQuery("SELECT u FROM User u where u.foo.bar = ?1", "SELECT count(u) FROM User u where u.foo.bar = ?1");
	}

	@Test
	void createsCountQueryForDistinctQueries() {

		assertCountQuery("select distinct u from User u where u.foo = ?1",
				"select count(distinct u) from User u where u.foo = ?1");
	}

	@Test
	void createsCountQueryForConstructorQueries() {

		assertCountQuery("select distinct new com.example.User(u.name) from User u where u.foo = ?1",
				"select count(distinct u) from User u where u.foo = ?1");

		assertCountQuery("select distinct new com.example.User(name, lastname) from User where foo = ?1",
				"select count(distinct name, lastname) from User where foo = ?1");
	}

	@Test
	void createsCountQueryForJoins() {

		assertCountQuery("select distinct new com.User(u.name) from User u left outer join u.roles r WHERE r = ?1",
				"select count(distinct u) from User u left outer join u.roles r WHERE r = ?1");
	}

	@Test
	void createsCountQueryForQueriesWithSubSelectsSelectQuery() {

		assertCountQuery("select u from User u left outer join u.roles r where r in (select r from Role r)",
				"select count(u) from User u left outer join u.roles r where r in (select r from Role r)");
	}

	@Test
	void createsCountQueryForQueriesWithSubSelects() {

		assertCountQuery("from User u left outer join u.roles r where r in (select r from Role r) select u ",
				"from User u left outer join u.roles r where r in (select r from Role r) select count(u)");
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

		assertThat(alias(QUERY)).isEqualTo("u");
		assertThat(alias(SIMPLE_QUERY)).isEqualTo("u");
		assertThat(alias(COUNT_QUERY)).isEqualTo("u");
		assertThat(alias(QUERY_WITH_AS)).isEqualTo("u");
		assertThat(alias("SELECT u FROM USER U")).isEqualTo("U");
		assertThat(alias("select u from  User u")).isEqualTo("u");
		assertThat(alias("select new com.acme.UserDetails(u.id, u.name) from User u")).isEqualTo("u");
		assertThat(alias("select u from T05User u")).isEqualTo("u");
		assertThat(alias("select u from User u where not exists (select m from User m where m = u.manager) "))
				.isEqualTo("u");
		assertThat(alias("select u from User u where not exists (select u2 from User u2)")).isEqualTo("u");
		assertThat(alias(
				"select u from User u where not exists (select u2 from User u2 where not exists (select u3 from User u3))"))
				.isEqualTo("u");
		assertThat(alias(
				"SELECT e FROM DbEvent e WHERE TREAT(modifiedFrom AS date) IS NULL OR e.modificationDate >= :modifiedFrom"))
				.isEqualTo("e");
	}

	@Test // GH-2557
	void applySortingAccountsForNewlinesInSubselect() {

		Sort sort = Sort.by(Sort.Order.desc("age"));

		assertThat(newParser("""
				select u
				from user u
				where exists (select u2
				from user u2
				)
				""").rewrite(new DefaultQueryRewriteInformation(sort,
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory()))))
				.isEqualToIgnoringWhitespace("""
				select u
				from user u
				where exists (select u2
				from user u2
				)
				 order by u.age desc""");
	}

	@Test // GH-2563
	void aliasDetectionProperlyHandlesNewlinesInSubselects() {

		assertThat(alias("""
				SELECT o
				FROM Order o
				WHERE EXISTS( SELECT 1
				    FROM Vehicle vehicle
				    WHERE vehicle.vehicleOrderId = o.id
				    AND LOWER(COALESCE(vehicle.make, '')) LIKE :query)
				""")).isEqualTo("o");
	}

	@Test // DATAJPA-252, GH-664, GH-1066, GH-2960
	void doesNotPrefixOrderReferenceIfOuterJoinAliasDetected() {

		String query = "select p from Person p left join p.address address";
		Sort sort = Sort.by("address.city");
		assertThat(createQueryFor(query, sort)).endsWith("order by address.city asc");
	}

	@Test // DATAJPA-252
	void extendsExistingOrderByClausesCorrectly() {

		String query = "select p from Person p order by p.lastname asc";
		Sort sort = Sort.by("firstname");
		assertThat(createQueryFor(query, sort))
				.isEqualTo("select p from Person p order by p.lastname asc, p.firstname asc");
	}

	@Test // DATAJPA-296
	void appliesIgnoreCaseOrderingCorrectly() {

		String query = "select p from Person p";
		Sort sort = Sort.by(Sort.Order.by("firstname").ignoreCase());

		assertThat(createQueryFor(query, sort)).endsWith("order by lower(p.firstname) asc");
	}

	@Test // DATAJPA-296
	void appendsIgnoreCaseOrderingCorrectly() {

		String query = "select p from Person p order by p.lastname asc";
		Sort sort = Sort.by(Sort.Order.by("firstname").ignoreCase());

		assertThat(createQueryFor(query, sort))
				.isEqualTo("select p from Person p order by p.lastname asc, lower(p.firstname) asc");
	}

	@Test // DATAJPA-342
	void usesReturnedVariableInCountProjectionIfSet() {

		assertCountQuery("select distinct m.genre from Media m where m.user = ?1 order by m.genre asc",
				"select count(distinct m.genre) from Media m where m.user = ?1");
	}

	@Test // DATAJPA-343
	void projectsCountQueriesForQueriesWithSubselects() {

		// given
		var original = "select o from Foo o where cb.id in (select b from Bar b)";

		// when
		var results = createQueryFor(original, Sort.by("first_name", "last_name"));

		// then
		assertThat(results).isEqualTo(
				"select o from Foo o where cb.id in (select b from Bar b) order by o.first_name asc, o.last_name asc");

		assertCountQuery("select o from Foo o where cb.id in (select b from Bar b)",
				"select count(o) from Foo o where cb.id in (select b from Bar b)");
	}

	@Test // DATAJPA-148
	void doesNotPrefixSortsIfFunction() {

		Sort sort = Sort.by("sum(foo)");

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> createQueryFor("select p from Person p", sort));
	}

	@Test // DATAJPA-377
	void removesOrderByInGeneratedCountQueryFromOriginalQueryIfPresent() {

		assertCountQuery("select distinct m.genre from Media m where m.user = ?1 OrDer  By   m.genre ASC",
				"select count(distinct m.genre) from Media m where m.user = ?1");
	}

	@Test // DATAJPA-375
	void findsExistingOrderByIndependentOfCase() {

		Sort sort = Sort.by("lastname");
		String query = createQueryFor("select p from Person p ORDER BY p.firstname", sort);
		assertThat(query).endsWith("ORDER BY p.firstname, p.lastname asc");
	}

	@Test // DATAJPA-409
	void createsCountQueryForNestedReferenceCorrectly() {
		assertCountQuery("select a.b from A a", "select count(a) from A a");
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

	@Test // DATAJPA-736
	void supportsNonAsciiCharactersInEntityNames() {
		assertThat(createCountQueryFor("select u from Usèr u")).isEqualTo("select count(u) from Usèr u");
	}

	@Test // DATAJPA-798
	void detectsAliasInQueryContainingLineBreaks() {
		assertThat(alias("select \n u \n from \n User \nu")).isEqualTo("u");
	}

	@Test // DATAJPA-938
	void detectsConstructorExpressionInDistinctQuery() {
		assertThat(hasConstructorExpression("select distinct new com.example.Foo(b.name) from Bar b")).isTrue();
	}

	@Test // DATAJPA-938
	void detectsComplexConstructorExpression() {

		assertThat(hasConstructorExpression(
				"""
						select new foo.bar.Foo(ip.id, ip.name, sum(lp.amount))
						from Bar lp join lp.investmentProduct ip
						where (lp.toDate is null and lp.fromDate <= :now and lp.fromDate is not null) and lp.accountId = :accountId group by ip.id, ip.name, lp.accountId
						order by ip.name ASC"""))
				.isTrue();
	}

	@Test // DATAJPA-938
	void detectsConstructorExpressionWithLineBreaks() {
		assertThat(hasConstructorExpression("select new foo.bar.FooBar(\na.id) from DtoA a ")).isTrue();
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotAllowWhitespaceInSort() {

		Sort sort = Sort.by("case when foo then bar");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> createQueryFor("select p from Person p", sort));
	}

	@Test // DATAJPA-965, DATAJPA-970
	void doesNotPrefixUnsafeJpaSortFunctionCalls() {

		JpaSort sort = JpaSort.unsafe("sum(foo)");
		assertThat(createQueryFor("select p from Person p", sort)).endsWith("order by sum(foo) asc");
	}

	@Test // DATAJPA-965, DATAJPA-970, GH-2863
	void doesNotPrefixMultipleAliasedFunctionCalls() {

		String query = "SELECT AVG(m.price) AS avgPrice, SUM(m.stocks) AS sumStocks FROM Magazine m";
		Sort sort = Sort.by("avgPrice", "sumStocks");

		assertThat(createQueryFor(query, sort)).endsWith("order by avgPrice asc, sumStocks asc");
	}

	@Test // DATAJPA-965, DATAJPA-970, GH-2863
	void doesNotPrefixSingleAliasedFunctionCalls() {

		String query = "SELECT AVG(m.price) AS avgPrice FROM Magazine m";
		Sort sort = Sort.by("avgPrice");

		assertThat(createQueryFor(query, sort)).endsWith("order by avgPrice asc");
	}

	@Test // DATAJPA-965, DATAJPA-970, GH-2863
	void prefixesSingleNonAliasedFunctionCallRelatedSortProperty() {

		String query = "SELECT AVG(m.price) AS avgPrice FROM Magazine m";
		Sort sort = Sort.by("someOtherProperty");

		assertThat(createQueryFor(query, sort)).endsWith("order by m.someOtherProperty asc");
	}

	@Test // DATAJPA-965, DATAJPA-970, GH-2863
	void prefixesNonAliasedFunctionCallRelatedSortPropertyWhenSelectClauseContainsAliasedFunctionForDifferentProperty() {

		String query = "SELECT m.name, AVG(m.price) AS avgPrice FROM Magazine m";
		Sort sort = Sort.by("name", "avgPrice");

		assertThat(createQueryFor(query, sort)).endsWith("order by m.name asc, avgPrice asc");
	}

	@Test // DATAJPA-965, DATAJPA-970, GH-2863
	void doesNotPrefixAliasedFunctionCallNameWithMultipleNumericParameters() {

		String query = "SELECT SUBSTRING(m.name, 2, 5) AS trimmedName FROM Magazine m";
		Sort sort = Sort.by("trimmedName");

		assertThat(createQueryFor(query, sort)).endsWith("order by trimmedName asc");
	}

	@Test // DATAJPA-965, DATAJPA-970, GH-2863
	void doesNotPrefixAliasedFunctionCallNameWithMultipleStringParameters() {

		String query = "SELECT CONCAT(m.name, 'foo') AS extendedName FROM Magazine m";
		Sort sort = Sort.by("extendedName");

		assertThat(createQueryFor(query, sort)).endsWith("order by extendedName asc");
	}

	@Test // DATAJPA-965, DATAJPA-970, GH-2863
	void doesNotPrefixAliasedFunctionCallNameWithUnderscores() {

		String query = "SELECT AVG(m.price) AS avg_price FROM Magazine m";
		Sort sort = Sort.by("avg_price");

		assertThat(createQueryFor(query, sort)).endsWith("order by avg_price asc");
	}

	@Test // DATAJPA-965, DATAJPA-970, GH-2863
	void doesNotPrefixAliasedFunctionCallNameWithDots() {

		String query = "SELECT AVG(m.price) AS m.avg FROM Magazine m";
		Sort sort = Sort.by("m.avg");

		assertThatExceptionOfType(BadJpqlGrammarException.class).isThrownBy(() -> createQueryFor(query, sort));
	}

	@Test // DATAJPA-965, DATAJPA-970, GH-2863
	void doesNotPrefixAliasedFunctionCallNameWhenQueryStringContainsMultipleWhiteSpaces() {

		String query = "SELECT  AVG(  m.price  )   AS   avgPrice   FROM Magazine   m";
		Sort sort = Sort.by("avgPrice");

		assertThat(createQueryFor(query, sort)).endsWith("order by avgPrice asc");
	}

	@Test // DATAJPA-1506
	void detectsAliasWithGroupAndOrderBy() {

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> alias("select * from User group by name"));
		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> alias("select * from User order by name"));
		assertThat(alias("select u from User u group by name")).isEqualTo("u");
		assertThat(alias("select u from User u order by name")).isEqualTo("u");
	}

	@Test // DATAJPA-1500
	void createCountQuerySupportsWhitespaceCharacters() {

		assertThat(createCountQueryFor("""
				select user from User user
				 where user.age = 18
				 order by user.name
				""")).isEqualToIgnoringWhitespace("""
				select count(user) from User user
				 where user.age = 18
				""");
	}

	@Test // GH-3504
	void createCountWithCteShouldWork() {

		String countQuery = createCountQueryFor("""
				WITH maxId AS(select max(sr.snapshot.id) snapshotId from SnapshotReference sr
					where sr.id.selectionId = ?1 and sr.enabled
					group by sr.userId)
				select sr from maxId m join SnapshotReference sr on sr.snapshot.id = m.snapshotId
				""");

		assertThat(countQuery).startsWith("WITH maxId AS (select max(sr.snapshot.id) snapshotId from SnapshotReference sr")
				.endsWith("select count(*) from maxId m join SnapshotReference sr on sr.snapshot.id = m.snapshotId");
	}

	@Test // GH-3504
	void createSortedQueryWithCteShouldWork() {

		String sortedQuery = createQueryFor("""
				WITH maxId AS(select max(sr.snapshot.id) snapshotId from SnapshotReference sr
					where sr.id.selectionId = ?1 and sr.enabled
					group by sr.userId)
				select sr from maxId m join SnapshotReference sr on sr.snapshot.id = m.snapshotId
				""", Sort.by("sr.snapshot"));

		assertThat(sortedQuery).startsWith(
				"WITH maxId AS (select max(sr.snapshot.id) snapshotId from SnapshotReference sr where sr.id.selectionId = ?1 and sr.enabled group by sr.userId)")
				.endsWith(
						"select sr from maxId m join SnapshotReference sr on sr.snapshot.id = m.snapshotId order by sr.snapshot asc");
	}

	@Test
	void createCountQuerySupportsLineBreaksInSelectClause() {

		assertThat(createCountQueryFor("""
				select user.age,
				 user.name
				 from User user
				 where user.age = 18
				 order
				by
				user.name
				""")).isEqualToIgnoringWhitespace("""
				select count(user) from User user
				 where user.age = 18
				""");
	}

	@Test // DATAJPA-1061, GH-2863
	void appliesSortCorrectlyForFieldAliases() {

		String query = "SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a";
		Sort sort = Sort.by("authorName");

		String fullQuery = createQueryFor(query, sort);

		assertThat(fullQuery).endsWith("order by authorName asc");
	}

	@Test // GH-2280, GH-2863
	void appliesOrderingCorrectlyForFieldAliasWithIgnoreCase() {

		String query = "SELECT customer.id as id, customer.name as name FROM CustomerEntity customer";
		Sort sort = Sort.by(Sort.Order.by("name").ignoreCase());

		String fullQuery = createQueryFor(query, sort);

		assertThat(fullQuery).isEqualTo(
				"SELECT customer.id as id, customer.name as name FROM CustomerEntity customer order by lower(name) asc");
	}

	@Test // DATAJPA-1061, GH-2863
	void appliesSortCorrectlyForFunctionAliases() {

		String query = "SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a";
		Sort sort = Sort.by("title");

		String fullQuery = createQueryFor(query, sort);

		assertThat(fullQuery).endsWith("order by title asc");
	}

	@Test // DATAJPA-1061
	void appliesSortCorrectlyForSimpleField() {

		String query = "SELECT  m.price, lower(m.title) AS title, a.name as authorName   FROM Magazine   m INNER JOIN m.author a";
		Sort sort = Sort.by("price");

		String fullQuery = createQueryFor(query, sort);

		assertThat(fullQuery).endsWith("order by m.price asc");
	}

	@Test
	void createCountQuerySupportsLineBreakRightAfterDistinct() {

		assertThat(createCountQueryFor("""
				select
				distinct
				user.age,
				user.name
				from
				User
				user""")).isEqualTo(createCountQueryFor("""
				select
				distinct user.age,
				user.name
				from
				User
				user"""));
	}

	@Test
	void detectsAliasWithGroupAndOrderByWithLineBreaks() {

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> alias("select * from User group\nby name"));
		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> alias("select * from User order\nby name"));
		assertThat(alias("select u from User u group\nby name")).isEqualTo("u");
		assertThat(alias("select u from User u order\nby name")).isEqualTo("u");
		assertThat(alias("select u from User\nu\norder \n by name")).isEqualTo("u");
	}

	@Test // DATAJPA-1679
	void findProjectionClauseWithDistinct() {

		SoftAssertions.assertSoftly(softly -> {
			softly.assertThat(projection("select a,b,c from Entity x")).isEqualTo("a, b, c");
			softly.assertThat(projection("select a, b, c from Entity x")).isEqualTo("a, b, c");
			softly.assertThat(projection("select distinct a, b, c from Entity x")).isEqualTo("a, b, c");
			softly.assertThat(projection("select DISTINCT a, b, c from Entity x")).isEqualTo("a, b, c");
		});
	}

	@Test // DATAJPA-1696
	void findProjectionClauseWithSubselect() {

		// This is not a required behavior, in fact the opposite is,
		// but it documents a current limitation.
		// to fix this without breaking findProjectionClauseWithIncludedFrom we need a more sophisticated parser.
		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> projection("select * from (select x from y)"));
	}

	@Test // DATAJPA-1696
	void findProjectionClauseWithIncludedFrom() {
		assertThat(projection("select x, frommage, y from t")).isEqualTo("x, frommage, y");
	}

	@Test // GH-2341
	void countProjectionDistrinctQueryIncludesNewLineAfterFromAndBeforeJoin() {

		String originalQuery = "SELECT DISTINCT entity1\nFROM Entity1 entity1\nLEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key";
		assertCountQuery(originalQuery,
				"SELECT count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key");
	}

	@Test // GH-2341
	void countProjectionDistinctQueryIncludesNewLineAfterEntity() {

		String originalQuery = "SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key";
		assertCountQuery(originalQuery,
				"SELECT count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key");
	}

	@Test // GH-2341
	void countProjectionDistinctQueryIncludesNewLineAfterEntityAndBeforeWhere() {

		String originalQuery = "SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key\nwhere entity1.id = 1799";
		assertCountQuery(originalQuery,
				"SELECT count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key where entity1.id = 1799");
	}

	@Test // GH-2393
	void createCountQueryStartsWithWhitespace() {

		assertThat(createCountQueryFor(" \nselect u from User u where u.age > :age"))
				.isEqualTo("select count(u) from User u where u.age > :age");

		assertThat(createCountQueryFor(" \nselect u from User u where u.age > :age"))
				.isEqualTo("select count(u) from User u where u.age > :age");
	}

	@Test // GH-2260
	void applySortingAccountsForNativeWindowFunction() {

		Sort sort = Sort.by(Sort.Order.desc("age"));

		// order by absent
		assertThat(createQueryFor("select u from user u", sort)).isEqualTo("select u from user u order by u.age desc");

		// order by present
		assertThat(createQueryFor("select u from user u order by u.lastname", sort))
				.isEqualTo("select u from user u order by u.lastname, u.age desc");

		// partition by
		assertThat(createQueryFor("select dense_rank() over (partition by age) from user u", sort))
				.isEqualTo("select dense_rank() over (partition by age) from user u order by u.age desc");

		// order by in over clause
		assertThat(createQueryFor("select dense_rank() over (order by lastname) from user u", sort))
				.isEqualTo("select dense_rank() over (order by lastname) from user u order by u.age desc");

		// order by in over clause (additional spaces)
		assertThat(createQueryFor("select dense_rank() over ( order by lastname ) from user u", sort))
				.isEqualTo("select dense_rank() over (order by lastname) from user u order by u.age desc");

		// order by in over clause + at the end
		assertThat(createQueryFor("select dense_rank() over (order by lastname) from user u order by u.lastname", sort))
				.isEqualTo("select dense_rank() over (order by lastname) from user u order by u.lastname, u.age desc");

		// partition by + order by in over clause
		assertThat(createQueryFor(
				"select dense_rank() over (partition by active, age order by lastname range between 1.0 preceding and 1.0 following) from user u",
				sort)).isEqualTo(
						"select dense_rank() over (partition by active, age order by lastname range between 1.0 preceding and 1.0 following) from user u order by u.age desc");

		// partition by + order by in over clause + order by at the end
		assertThat(createQueryFor(
				"select dense_rank() over (partition by active, age order by lastname) from user u order by active", sort))
				.isEqualTo(
						"select dense_rank() over (partition by active, age order by lastname) from user u order by active, u.age desc");

		// partition by + order by in over clause + frame clause
		assertThat(createQueryFor(
				"select dense_rank() over ( partition by active, age order by username rows between current row and unbounded following ) from user u",
				sort)).isEqualTo(
						"select dense_rank() over (partition by active, age order by username rows between current row and unbounded following) from user u order by u.age desc");

		// partition by + order by in over clause + frame clause + order by at the end
		assertThat(createQueryFor(
				"select dense_rank() over ( partition by active, age order by username rows between current row and unbounded following ) from user u order by active",
				sort)).isEqualTo(
						"select dense_rank() over (partition by active, age order by username rows between current row and unbounded following) from user u order by active, u.age desc");

		// order by in subselect (select expression)
		assertThat(createQueryFor("select lastname, (select i.id from item i order by i.id limit 1) from user u", sort))
				.isEqualTo("select lastname, (select i.id from item i order by i.id limit 1) from user u order by u.age desc");

		// order by in subselect (select expression) + at the end
		assertThat(createQueryFor(
				"select lastname, (select i.id from item i order by 1 limit 1) from user u order by active", sort)).isEqualTo(
						"select lastname, (select i.id from item i order by 1 limit 1) from user u order by active, u.age desc");

		// order by in subselect (from expression)
		assertThat(createQueryFor("select u from (select u2 from user u2 order by age desc limit 10) u", sort))
				.isEqualTo("select u from (select u2 from user u2 order by age desc limit 10) u order by u.age desc");

		// order by in subselect (from expression) + at the end
		assertThat(createQueryFor(
				"select u from (select u2 from user u2 order by 1, 2, 3 desc limit 10) u order by u.active asc", sort))
				.isEqualTo(
						"select u from (select u2 from user u2 order by 1, 2, 3 desc limit 10) u order by u.active asc, u.age desc");
	}

	@Test // GH-2511
	void countQueryUsesCorrectVariable() {

		assertThat(createCountQueryFor("SELECT e FROM User e WHERE created_at > $1"))
				.isEqualTo("SELECT count(e) FROM User e WHERE created_at > $1");

		assertThat(
				createCountQueryFor("SELECT e FROM mytable e WHERE nr = :number AND kon = :kon AND datum >= '2019-01-01'"))
				.isEqualTo("SELECT count(e) FROM mytable e WHERE nr = :number AND kon = :kon AND datum >= '2019-01-01'");

		assertThat(createCountQueryFor("SELECT e FROM context e ORDER BY time"))
				.isEqualTo("SELECT count(e) FROM context e");

		assertThat(createCountQueryFor("select e FROM users_statuses e WHERE (user_created_at BETWEEN $1 AND $2)"))
				.isEqualTo("select count(e) FROM users_statuses e WHERE (user_created_at BETWEEN $1 AND $2)");

		assertThat(
				createCountQueryFor("SELECT us FROM users_statuses us WHERE (user_created_at BETWEEN :fromDate AND :toDate)"))
				.isEqualTo("SELECT count(us) FROM users_statuses us WHERE (user_created_at BETWEEN :fromDate AND :toDate)");
	}

	@Test // GH-2496, GH-2522, GH-2537, GH-2045
	void orderByShouldWorkWithSubSelectStatements() {

		Sort sort = Sort.by(Sort.Order.desc("age"));

		assertThat(createQueryFor("""
				SELECT
				   foo_bar
				FROM
				    foo foo
				INNER JOIN
				   foo_bar_dnrmv foo_bar ON
				   foo_bar.foo_id = foo.foo_id
				INNER JOIN
				 (
				  SELECT
				       foo_bar_action
				  FROM
				      foo_bar_action
				  WHERE
				       foo_bar_action.deleted_ts IS NULL)
				    foo_bar_action ON
				  foo_bar.foo_bar_id = foo_bar_action.foo_bar_id
				  AND ranking = 1
				INNER JOIN
				  bar bar ON
				  foo_bar.bar_id = bar.bar_id
				INNER JOIN
				  bar_metadata bar_metadata ON
				  bar.bar_metadata_key = bar_metadata.bar_metadata_key
				WHERE
				  foo.tenant_id =:tenantId""", sort)).endsWith("order by foo.age desc");

		assertThat(createQueryFor("""
				select r
				From DataRecord r
				where
				 (
				       r.adusrId = :userId
				       or EXISTS( select 1 FROM DataRecordDvsRight dr WHERE dr.adusrId = :userId AND dr.dataRecord = r )
				)""", sort)).endsWith("order by r.age desc");

		assertThat(createQueryFor("""
				select distinct u
				from FooBar u
				where u.role = 'redacted'
				and (
						not exists (
								from FooBarGroup group
								where group in :excludedGroups
								and group in elements(u.groups)
						)
				)""", sort)).endsWith("order by u.age desc");

		assertThat(createQueryFor("""
				SELECT i
				 FROM Item i
				 WHERE i.id IN (
				 SELECT max(i2.id) FROM Item i2
				 WHERE i2.field.id = :fieldId
				 GROUP BY i2.field.id, i2.version)""", sort)).endsWith("order by i.age desc");

		assertThat(createQueryFor("""
				select
				 f.id,
				 (
				  select timestamp from bar
				  where date(bar.timestamp) > '2022-05-21'
				  and bar.foo_id = f.id
				  order by date(bar.timestamp) desc
				  limit 1
				) as timestamp
				from foo f""", sort)).endsWith("order by f.age desc");
	}

	@Test // GH-2862
	void sortProperlyAppendsToExistingOrderByWithFunction() {

		assertThat(createQueryFor(
				"select e from SampleEntity e  where function('nativeFunc', ?1) > 'testVal'  order by function('nativeFunc', ?1)",
				Sort.by(Sort.Order.desc("age")))).isEqualTo(
						"select e from SampleEntity e where function('nativeFunc', ?1) > 'testVal' order by function('nativeFunc', ?1), e.age desc");
	}

	@Test // GH-2074
	void queryParserPicksCorrectAliasAmidstMultipleAlises() {
		assertThat(alias("select u from User as u left join  u.roles as r")).isEqualTo("u");
	}

	@Test // GH-2032, GH-3792
	void countQueryShouldWorkEvenWithoutExplicitAlias() {

		assertCountQuery("FROM BookError WHERE portal = :portal",
				"select count(__) FROM BookError AS __ WHERE portal = :portal");

		assertCountQuery("FROM BookError b WHERE portal = :portal",
				"select count(b) FROM BookError b WHERE portal = :portal");
	}

	@ParameterizedTest
	@MethodSource("queriesWithReservedWordsAsIdentifiers") // GH-2864
	void usingReservedWordAsRelationshipNameShouldWork(String relationshipName, String joinAlias) {

		JpaQueryEnhancer.HqlQueryParser.parseQuery(String.format("""
				select u
				from UserAccountEntity u
				join fetch u.lossInspectorLimitConfiguration lil
				join fetch u.companyTeam ct
				where exists (
					select iu
					from UserAccountEntity  iu
					join iu.roles u2r
					join u2r.role r
					join r.rights r2r
					join r2r.%s %s
					where
						%s.code = :rightCode
						and iu = u
				)
				and ct.id = :teamId
				""", relationshipName, joinAlias, joinAlias));
	}

	static Stream<Arguments> queriesWithReservedWordsAsIdentifiers() {

		return Stream.of( //
				Arguments.of("right", "rt"), //
				Arguments.of("left", "lt"), //
				Arguments.of("outer", "ou"), //
				Arguments.of("inner", "inr"));
	}

	@Test // GH-2508
	void detectAliasWithCastCorrectly() {

		assertThat(alias("from User u where (cast(:effective as date) is null) OR :effective >= u.createdAt"))
				.isEqualTo("u");
		assertThat(alias("from User u where (cast(:effectiveDate as date) is null) OR :effectiveDate >= u.createdAt"))
				.isEqualTo("u");
		assertThat(alias("from User u where (cast(:effectiveFrom as date) is null) OR :effectiveFrom >= u.createdAt"))
				.isEqualTo("u");
	}

	@Test // GH-2348
	void removeFetchFromJoinsDuringCountQueryCreation() {

		assertCountQuery("select u from User u left outer join fetch u.roles r left outer JOIN   FETCH  u.accounts a",
				"select count(u) from User u left outer join u.roles r left outer JOIN u.accounts a");

		assertCountQuery("SELECT DISTINCT b FROM Board b LEFT JOIN FETCH b.comments ORDER BY b.id",
				"SELECT count(DISTINCT b) FROM Board b LEFT JOIN b.comments");
	}

	@Test // GH-2626, GH-2863
	void orderByAliasedColumn() {

		assertThat(createQueryFor("""
				select
					max(resource.name)             as resourceName,
					max(resource.id) as id,
					max(resource.description) as description,
					max(resource.uuid) as uuid,
					max(resource.type)        as type,
					max(resource.createdOn)  as createdOn,
					max(users.firstName)     as authorFirstName,
					max(users.lastName)      as authorLastName,
					max(file.version)         as version,
					max(file.comment)         as comment,
					file.deployed             as deployed,
					max(log.date)             as modifiedOn
				from Resource resource
				where (
					cast(:startDate as date) is null
					or resource.latestLogRecord.date between cast(:startDate as date) and cast(:endDate as date)
				)
				group by resource.id, file.deployed, log.author.firstName, file.comment
				""", Sort.by(Sort.Direction.DESC, "uuid"))).endsWith("order by uuid desc");
	}

	@Test // GH-2863, GH-2322
	void sortShouldWorkWhenAliasingFunctions() {

		assertThat(createQueryFor("""
				SELECT
					DISTINCT(event.id) as id,
					event.name as name,
					MIN(bundle.base_price_amount) as cheapestBundlePrice,
					MIN(DATE(bundle.start)) as earliestBundleStart
				FROM event event
				LEFT JOIN bundle bundle ON event.id = bundle.event_id
				GROUP BY event.id
				""", Sort.by(Sort.Direction.ASC, "cheapestBundlePrice") //
				.and(Sort.by(Sort.Direction.ASC, "earliestBundleStart")) //
				.and(Sort.by(Sort.Direction.ASC, "name"))))
				.endsWith(" order by cheapestBundlePrice asc, earliestBundleStart asc, name asc");
	}

	@Test // GH-2863, GH-1655
	void shouldHandleAliasInsideCaseStatement() {

		Sort sort = PageRequest.of(0, 20, Sort.Direction.DESC, "newDateDue").getSort();

		assertThat(createQueryFor("Select DISTINCT new " + //
				"com.api.dto.FilterDTO(c.id, p.id, CASE WHEN item.dateDue IS NOT NULL THEN item.dateDue ELSE p.dateDue END AS newDateDue) "
				+ "FROM Customer c " + //
				"join c.productOrder p " + //
				"JOIN p.items item", //
				sort)).isEqualTo("Select DISTINCT new " + //
						"com.api.dto.FilterDTO(c.id, p.id, CASE WHEN item.dateDue IS NOT NULL THEN item.dateDue ELSE p.dateDue END AS newDateDue) "
						+ "FROM Customer c " + //
						"join c.productOrder p " + //
						"JOIN p.items item " + //
						"order by newDateDue desc");
	}

	@Test // GH-2969
	void fromWithoutAPrimaryAliasShouldWork() {

		assertThat(createQueryFor("FROM Story WHERE enabled = true", Sort.by(Sort.Direction.DESC, "created")))
				.isEqualTo("FROM Story WHERE enabled = true order by created desc");
	}

	@ParameterizedTest
	@ValueSource(strings = { """
			insert into MyEntity (id, col)
			select max(id), col
			from MyEntityStaging
			group by col
			""", """
			update MyEntity AS mes
			set mes.col = 'test'
			where mes.id = 1
			""", """
			delete MyEntity AS mes
			where mes.col = 'test'
			""" }) // GH-2977, GH-3649
	void isSubqueryThrowsException(String query) {
		assertThat(createQueryFor(query, Sort.unsorted())).isEqualToIgnoringWhitespace(query);
	}

	@Test // GH-664, GH-1066, GH-2960
	void sortingRecognizesJoinAliases() {

		String query = "select p from Customer c join c.productOrder p where p.delayed = true";

		assertThat(createQueryFor(query, Sort.by(Sort.Order.desc("lastName")))).isEqualToIgnoringWhitespace("""
				select p from Customer c
				join c.productOrder p
				where p.delayed = true
				order by c.lastName desc
				""");

		assertThat(createQueryFor(query, Sort.by(Sort.Order.desc("p.lineItems")))).isEqualToIgnoringWhitespace("""
				select p from Customer c
				join c.productOrder p
				where p.delayed = true
				order by p.lineItems desc
				""");
	}

	@Test // GH-3054
	void aliasesShouldNotOverlapWithSortProperties() {

		assertThat(
				createQueryFor("select e from Employee e where e.name = :name", Sort.by(Sort.Order.desc("evaluationDate"))))
				.isEqualToIgnoringWhitespace("select e from Employee e where e.name = :name order by e.evaluationDate desc");

		assertThat(createQueryFor("select e from Employee e join training t where e.name = :name",
				Sort.by(Sort.Order.desc("trainingDueDate")))).isEqualToIgnoringWhitespace(
						"select e from Employee e join training t where e.name = :name order by e.trainingDueDate desc");

		assertThat(createQueryFor("select e from Employee e join training t where e.name = :name",
				Sort.by(Sort.Order.desc("t.trainingDueDate")))).isEqualToIgnoringWhitespace(
						"select e from Employee e join training t where e.name = :name order by t.trainingDueDate desc");

		assertThat(createQueryFor("SELECT t3 FROM Test3 t3 JOIN t3.test2 t2 JOIN t2.test1 test WHERE test.id = :test1Id",
				Sort.by(Sort.Order.desc("testDuplicateColumnName")))).isEqualToIgnoringWhitespace(
						"SELECT t3 FROM Test3 t3 JOIN t3.test2 t2 JOIN t2.test1 test WHERE test.id = :test1Id order by t3.testDuplicateColumnName desc");

		assertThat(createQueryFor("SELECT t3 FROM Test3 t3 JOIN t3.test2 x WHERE x.id = :test2Id",
				Sort.by(Sort.Order.desc("t3.testDuplicateColumnName")))).isEqualToIgnoringWhitespace(
						"SELECT t3 FROM Test3 t3 JOIN t3.test2 x WHERE x.id = :test2Id order by t3.testDuplicateColumnName desc");
	}

	@Test // GH-3269, GH-3689
	void createsCountQueryUsingAliasCorrectly() {

		assertCountQuery("select distinct 1 as x from Employee", "select count(distinct 1) from Employee");
		assertCountQuery("SELECT DISTINCT abc AS x FROM T", "SELECT count(DISTINCT abc) FROM T");
		assertCountQuery("select distinct a as x, b as y from Employee", "select count(distinct a, b) from Employee");
		assertCountQuery("select distinct sum(amount) as x from Employee GROUP BY n",
				"select count(distinct sum(amount)) from Employee GROUP BY n");
		assertCountQuery("select distinct a, b, sum(amount) as c, d from Employee GROUP BY n",
				"select count(distinct a, b, sum(amount), d) from Employee GROUP BY n");
		assertCountQuery("select distinct a, count(b) as c from Employee GROUP BY n",
				"select count(distinct a, count(b)) from Employee GROUP BY n");
		assertCountQuery("select distinct substring(e.firstname, 1, position('a' in e.lastname)) as x from from Employee",
				"select count(distinct substring(e.firstname, 1, position('a' in e.lastname))) from from Employee");
	}

	@Test // GH-3427
	void sortShouldBeAppendedWithSpacingInCaseOfSetOperator() {

		String source = "SELECT tb FROM Test tb WHERE (tb.type='A') UNION SELECT tb FROM Test tb WHERE (tb.type='B') UNION SELECT tb FROM Test tb WHERE (tb.type='C')";
		String target = createQueryFor(source, Sort.by("Type").ascending());

		assertThat(target).isEqualTo("SELECT tb FROM Test tb WHERE (tb.type = 'A') " //
				+ "UNION SELECT tb FROM Test tb WHERE (tb.type = 'B') " //
				+ "UNION SELECT tb FROM Test tb WHERE (tb.type = 'C') order by tb.Type asc");
	}

	@ParameterizedTest // GH-3427
	@ValueSource(strings = { "", "res" })
	void sortShouldBeAppendedToSubSelectWithSetOperatorInSubselect(String alias) {

		String prefix = StringUtils.hasText(alias) ? (alias + ".") : "";
		String source = "SELECT %sname FROM (SELECT c.name as name FROM Category c UNION SELECT t.name as name FROM Tag t)"
				.formatted(prefix);

		if (StringUtils.hasText(alias)) {
			source = source + " %s".formatted(alias);
		}

		String target = createQueryFor(source, Sort.by("name").ascending());

		assertThat(target).contains(" UNION SELECT ").doesNotContainPattern(Pattern.compile(".*\\SUNION"));
		assertThat(target).endsWith("order by %sname asc".formatted(prefix)).satisfies(it -> {
			Pattern pattern = Pattern.compile("order by");
			Matcher matcher = pattern.matcher(target);
			int count = 0;
			while (matcher.find()) {
				count++;
			}
			assertThat(count).describedAs("Found order by clause more than once in: \n%s", it).isOne();
		});
	}

	private void assertCountQuery(String originalQuery, String countQuery) {
		assertThat(createCountQueryFor(originalQuery)).isEqualTo(countQuery);
	}

	private String createQueryFor(String query, Sort sort) {
		return newParser(query).rewrite(new DefaultQueryRewriteInformation(sort,
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory())));
	}

	private String createCountQueryFor(String query) {
		return createCountQueryFor(query, null);
	}

	private String createCountQueryFor(String query, @Nullable String countProjection) {
		return newParser(query).createCountQueryFor(countProjection);
	}

	private @Nullable String alias(String query) {
		return newParser(query).detectAlias();
	}

	private boolean hasConstructorExpression(String query) {
		return newParser(query).hasConstructorExpression();
	}

	private String projection(String query) {
		return newParser(query).getProjection();
	}

	private QueryEnhancer newParser(String query) {
		return JpaQueryEnhancer.forHql(query);
	}
}
