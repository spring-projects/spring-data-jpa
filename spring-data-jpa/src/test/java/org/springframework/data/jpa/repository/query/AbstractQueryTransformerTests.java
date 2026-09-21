/*
 * Copyright 2022-present the original author or authors.
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

import org.assertj.core.api.SoftAssertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.query.ReturnedType;

/**
 * Base class for tests verifying query transformation through a {@link JpaQueryEnhancer}: applying {@link Sort},
 * deriving count queries, detecting the primary alias and extracting projections. Subclasses provide the
 * dialect-specific {@link QueryEnhancer} and add the cases that only apply to their dialect.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 */
abstract class AbstractQueryTransformerTests {

	static final String QUERY = "select u from User u";
	static final String SIMPLE_QUERY = "from User u";
	static final String COUNT_QUERY = "select count(u) from User u";
	static final String QUERY_WITH_AS = "select u from User as u where u.username = ?1";

	/**
	 * Create the {@link QueryEnhancer} under test for the given query.
	 */
	abstract QueryEnhancer createQueryEnhancer(String query);

	/**
	 * Count projection to expect for a query without a primary alias. HQL counts rows using {@code count(*)}, JPQL and
	 * EQL count the first selection item instead.
	 */
	abstract String countProjectionWithoutAlias(String firstSelection);

	String createQueryFor(String query, Sort sort) {
		return createQueryEnhancer(query).rewrite(new DefaultQueryRewriteInformation(sort,
				ReturnedType.of(Object.class, Object.class, new SpelAwareProxyProjectionFactory())));
	}

	String createCountQueryFor(String query) {
		return createCountQueryFor(query, null);
	}

	String createCountQueryFor(String query, @Nullable String countProjection) {
		return createQueryEnhancer(query).createCountQueryFor(countProjection);
	}

	@Nullable
	String alias(String query) {
		return createQueryEnhancer(query).detectAlias();
	}

	@Nested
	class Sorting {

		@Test // GH-1280
		void unsortedLeavesQueryUntouched() {

			assertThat(createQueryFor("select e from Employee e join e.manager m", Sort.unsorted()))
					.isEqualTo("select e from Employee e join e.manager m");

			String query = "SELECT e FROM Employee e where e.name = :name ORDER BY e.first_name asc NULLS FIRST";
			assertThat(createQueryFor(query, Sort.unsorted())).isEqualTo(query);
		}

		@Test // GH-2260
		void appliesSortWhereNoOrderByExists() {

			assertThat(createQueryFor("SELECT e FROM Employee e where e.name = :name", Sort.by("first_name", "last_name")))
					.isEqualTo("SELECT e FROM Employee e where e.name = :name order by e.first_name asc, e.last_name asc");

			assertThat(createQueryFor("select u from user u", Sort.by(Order.desc("age"))))
					.isEqualTo("select u from user u order by u.age desc");
		}

		@Test // DATAJPA-252, DATAJPA-375, GH-2260
		void appendsSortToExistingOrderBy() {

			assertThat(createQueryFor("SELECT e FROM Employee e where e.name = :name ORDER BY e.role, e.hire_date",
					Sort.by("first_name", "last_name")))
					.endsWith("ORDER BY e.role, e.hire_date, e.first_name asc, e.last_name asc");

			assertThat(createQueryFor("select p from Person p order by p.lastname asc", Sort.by("firstname")))
					.isEqualTo("select p from Person p order by p.lastname asc, p.firstname asc");

			assertThat(createQueryFor("select p from Person p ORDER BY p.firstname", Sort.by("lastname")))
					.endsWith("ORDER BY p.firstname, p.lastname asc");

			assertThat(createQueryFor("select u from user u order by u.lastname", Sort.by(Order.desc("age"))))
					.isEqualTo("select u from user u order by u.lastname, u.age desc");
		}

		@Test // GH-1280
		void appliesNullHandling() {

			String query = "SELECT e FROM Employee e where e.name = :name ORDER BY e.first_name asc NULLS FIRST";

			assertThat(createQueryFor(query, Sort.by(Order.desc("lastName").nullsLast()))).startsWith(query)
					.endsWithIgnoringCase("e.lastName DESC NULLS LAST");

			assertThat(createQueryFor(query, Sort.by(Order.desc("lastName").nullsFirst()))).startsWith(query)
					.endsWithIgnoringCase("e.lastName DESC NULLS FIRST");
		}

		@Test // DATAJPA-296
		void appliesIgnoreCaseOrdering() {

			Sort sort = Sort.by(Order.by("firstname").ignoreCase());

			assertThat(createQueryFor("select p from Person p", sort)).endsWith("order by lower(p.firstname) asc");
			assertThat(createQueryFor("select p from Person p order by p.lastname asc", sort))
					.isEqualTo("select p from Person p order by p.lastname asc, lower(p.firstname) asc");
		}

		@Test // GH-2280, GH-2863
		void appliesIgnoreCaseOrderingToSelectAlias() {

			String query = "SELECT customer.id as id, customer.name as name FROM CustomerEntity customer";
			Sort sort = Sort.by(Order.by("name").ignoreCase());

			assertThat(createQueryFor(query, sort)).isEqualTo(
					"SELECT customer.id as id, customer.name as name FROM CustomerEntity customer order by lower(name) asc");
		}

		@ParameterizedTest(name = "sort by {1}") // DATAJPA-965, DATAJPA-970, DATAJPA-1061, GH-2863
		@CsvSource(delimiter = '|', quoteCharacter = '"',
				textBlock = """
						SELECT AVG(m.price) AS avgPrice, SUM(m.stocks) AS sumStocks FROM Magazine m | avgPrice, sumStocks | order by avgPrice asc, sumStocks asc
						SELECT AVG(m.price) AS avgPrice FROM Magazine m                              | avgPrice            | order by avgPrice asc
						SELECT AVG(m.price) AS avgPrice FROM Magazine m                              | someOtherProperty   | order by m.someOtherProperty asc
						SELECT m.name, AVG(m.price) AS avgPrice FROM Magazine m                      | name, avgPrice      | order by m.name asc, avgPrice asc
						SELECT SUBSTRING(m.name, 2, 5) AS trimmedName FROM Magazine m                | trimmedName         | order by trimmedName asc
						SELECT CONCAT(m.name, 'foo') AS extendedName FROM Magazine m                 | extendedName        | order by extendedName asc
						SELECT AVG(m.price) AS avg_price FROM Magazine m                             | avg_price           | order by avg_price asc
						SELECT  AVG(  m.price  )   AS   avgPrice   FROM Magazine   m                 | avgPrice            | order by avgPrice asc
						SELECT m.price, lower(m.title) AS title, a.name as authorName FROM Magazine m INNER JOIN m.author a | authorName | order by authorName asc
						SELECT m.price, lower(m.title) AS title, a.name as authorName FROM Magazine m INNER JOIN m.author a | title      | order by title asc
						SELECT m.price, lower(m.title) AS title, a.name as authorName FROM Magazine m INNER JOIN m.author a | price      | order by m.price asc
						""")
		void sortsBySelectAlias(String query, String properties, String orderBy) {
			assertThat(createQueryFor(query, Sort.by(properties.split(",\\s*")))).endsWith(orderBy);
		}

		@Test // DATAJPA-965, DATAJPA-970, GH-2863
		void rejectsSelectAliasContainingDot() {

			assertThatExceptionOfType(BadJpqlGrammarException.class)
					.isThrownBy(() -> createQueryFor("SELECT AVG(m.price) AS m.avg FROM Magazine m", Sort.by("m.avg")));
		}

		@Test // DATAJPA-148, DATAJPA-965, DATAJPA-970
		void rejectsFunctionCallsAndWhitespaceInSort() {

			assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
					.isThrownBy(() -> createQueryFor("select p from Person p", Sort.by("sum(foo)")));

			assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
					.isThrownBy(() -> createQueryFor("select p from Person p", Sort.by("case when foo then bar")));
		}

		@Test // DATAJPA-965, DATAJPA-970
		void doesNotPrefixUnsafeJpaSort() {
			assertThat(createQueryFor("select p from Person p", JpaSort.unsafe("sum(foo)")))
					.endsWith("order by sum(foo) asc");
		}

		@Test // DATAJPA-252, GH-664, GH-1066, GH-2960
		void doesNotPrefixJoinAlias() {

			assertThat(createQueryFor("select p from Person p left join p.address address", Sort.by("address.city")))
					.endsWith("order by address.city asc");
		}

		@Test // GH-664, GH-1066, GH-2960
		void recognizesJoinAliases() {

			String query = "select p from Customer c join c.productOrder p where p.delayed = true";

			assertThat(createQueryFor(query, Sort.by(Order.desc("lastName")))).isEqualToIgnoringWhitespace("""
					select p from Customer c
					join c.productOrder p
					where p.delayed = true
					order by c.lastName desc
					""");

			assertThat(createQueryFor(query, Sort.by(Order.desc("p.lineItems")))).isEqualToIgnoringWhitespace("""
					select p from Customer c
					join c.productOrder p
					where p.delayed = true
					order by p.lineItems desc
					""");
		}

		@Test // DATAJPA-343, GH-2045, GH-2496, GH-2522, GH-2537, GH-2557
		void appliesSortToQueriesWithSubselects() {

			Sort sort = Sort.by(Order.desc("age"));

			assertThat(createQueryFor("select o from Foo o where cb.id in (select b from Bar b)",
					Sort.by("first_name", "last_name"))).isEqualTo(
							"select o from Foo o where cb.id in (select b from Bar b) order by o.first_name asc, o.last_name asc");

			assertThat(createQueryFor("""
					select u
					from user u
					where exists (select u2
					from user u2
					)
					""", sort)).isEqualToIgnoringWhitespace("""
					select u
					from user u
					where exists (select u2
					from user u2
					)
					 order by u.age desc""");

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
									select g from FooBarGroup g
									where g in :excludedGroups
							)
					)""", sort)).endsWith("order by u.age desc");

			assertThat(createQueryFor("""
					SELECT i
					FROM Item i
					WHERE i.id IN (
					SELECT max(i2.id) FROM Item i2
					WHERE i2.field.id = :fieldId
					GROUP BY i2.field.id, i2.version)""", sort)).endsWith("order by i.age desc");
		}

		@Test // GH-3427, GH-4342
		void appliesSortToSetQuery() {

			String source = "SELECT tb FROM Test tb WHERE (tb.type='A') UNION SELECT tb FROM Test tb WHERE (tb.type='B') UNION SELECT tb FROM Test tb WHERE (tb.type='C')";

			assertThat(createQueryFor(source, Sort.by("Type").ascending()))
					.isEqualTo("SELECT tb FROM Test tb WHERE (tb.type = 'A') " //
							+ "UNION SELECT tb FROM Test tb WHERE (tb.type = 'B') " //
							+ "UNION SELECT tb FROM Test tb WHERE (tb.type = 'C') order by tb.Type asc");
		}

	}

	@Nested
	class CountQueries {

		@Test
		void createsCountQuery() {

			assertThat(createCountQueryFor(QUERY)).isEqualTo(COUNT_QUERY);
			assertThat(createCountQueryFor("select u from User as u")).isEqualTo("select count(u) from User as u");
			assertThat(createCountQueryFor("SELECT e FROM Employee e where e.name = :name"))
					.isEqualTo("SELECT count(e) FROM Employee e where e.name = :name");
			assertThat(createCountQueryFor("select u FROM User u WHERE u.foo.bar = ?1"))
					.isEqualTo("select count(u) FROM User u WHERE u.foo.bar = ?1");
			assertThat(createCountQueryFor("SELECT u FROM User u where u.foo.bar = ?1"))
					.isEqualTo("SELECT count(u) FROM User u where u.foo.bar = ?1");
		}

		@Test // GH-4341
		void createsCountQueryForDistinctFunctionSelection() {

			assertThat(createCountQueryFor("select distinct coalesce(u.name, u.lastname) from User u where u.foo = ?1"))
				.isEqualTo("select count(distinct coalesce(u.name, u.lastname)) from User u where u.foo = ?1");
		}

		@Test // GH-4341
		void createsCountQueryForDistinctExpressionSelection() {

			assertThat(createCountQueryFor("select distinct case when u.age > 18 then 'adult' else 'minor' end from User u"))
				.isEqualTo("select count(distinct case when u.age > 18 then 'adult' else 'minor' end) from User u");
		}

		@Test // GH-2032, GH-3792, GH-3902
		void createsCountQueryForFromQuery() {

			assertThat(createCountQueryFor(SIMPLE_QUERY)).isEqualTo(COUNT_QUERY);
			assertThat(createCountQueryFor("FROM Employee e where e.name = :name"))
					.isEqualTo("select count(e) FROM Employee e where e.name = :name");
			assertThat(createCountQueryFor("FROM BookError b WHERE portal = :portal"))
					.isEqualTo("select count(b) FROM BookError b WHERE portal = :portal");
			assertThat(createCountQueryFor("FROM Employee where name = :name"))
					.isEqualTo("select count(__) FROM Employee AS __ where name = :name");
			assertThat(createCountQueryFor("FROM BookError WHERE portal = :portal"))
					.isEqualTo("select count(__) FROM BookError AS __ WHERE portal = :portal");
		}

		@Test // DATAJPA-409, DATAJPA-420, GH-3902
		void createsCountQueryForScalarSelects() {

			assertThat(createCountQueryFor("select a.b from A a")).isEqualTo("select count(a) from A a");
			assertThat(createCountQueryFor("SELECT p.id FROM Person p")).isEqualTo("SELECT count(p) FROM Person p");
			assertThat(createCountQueryFor("SELECT id FROM Person p")).isEqualTo("SELECT count(p) FROM Person p");
			assertThat(createCountQueryFor("SELECT id, name FROM Person p")).isEqualTo("SELECT count(p) FROM Person p");
			assertThat(createCountQueryFor("select p.lastname,p.firstname from Person p"))
					.isEqualTo("select count(p) from Person p");
			assertThat(
					createCountQueryFor("SELECT e.foo, e.bar FROM Employee e where e.name = :name ORDER BY e.modified_date"))
					.isEqualTo("SELECT count(e) FROM Employee e where e.name = :name");
		}

		@Test // GH-3902, GH-4341
		void createsCountQueryWithoutPrimaryAlias() {

			String countQuery4 = "SELECT %s FROM Person".formatted(countProjectionWithoutAlias("id"));
			assertThat(createCountQueryFor("SELECT id FROM Person")).isEqualTo(countQuery4);
			String countQuery3 = "SELECT %s FROM Person".formatted(countProjectionWithoutAlias("id"));
			assertThat(createCountQueryFor("SELECT id, name FROM Person")).isEqualTo(countQuery3);

			assertThat(createCountQueryFor("SELECT id AS x FROM Person"))
					.isEqualTo("SELECT %s FROM Person".formatted(countProjectionWithoutAlias("id")));

			String countQuery2 = "SELECT %s FROM Order WHERE this.customer.firstname = 'John' AND this.customer.lastname = 'Wick'"
					.formatted(countProjectionWithoutAlias("this.quantity"));
			assertThat(createCountQueryFor(
					"SELECT this.quantity FROM Order WHERE this.customer.firstname = 'John' AND this.customer.lastname = 'Wick'"))
					.isEqualTo(countQuery2);

			String countQuery1 = "SELECT %s FROM Order WHERE this.customer.firstname = 'John' AND this.customer.lastname = 'Wick'"
					.formatted(countProjectionWithoutAlias("this.quantity"));
			assertThat(createCountQueryFor(
					"SELECT this.quantity, that.quantity FROM Order WHERE this.customer.firstname = 'John' AND this.customer.lastname = 'Wick'"))
					.isEqualTo(countQuery1);

			String countQuery = "select %s from User left outer join u.roles r where r in (select r from Role r)"
					.formatted(countProjectionWithoutAlias("name"));
			assertThat(createCountQueryFor(
					"select name, (select foo from bar b) from User left outer join u.roles r where r in (select r from Role r)"))
					.isEqualTo(countQuery);
		}

		@Test // DATAJPA-377, GH-2511
		void removesOrderByFromCountQuery() {

			assertThat(createCountQueryFor("SELECT e FROM Employee e where e.name = :name ORDER BY e.modified_date"))
					.isEqualTo("SELECT count(e) FROM Employee e where e.name = :name");
			assertThat(createCountQueryFor("select distinct m.genre from Media m where m.user = ?1 OrDer  By   m.genre ASC"))
					.isEqualTo("select count(distinct m.genre) from Media m where m.user = ?1");
			assertThat(createCountQueryFor("SELECT e FROM context e ORDER BY time"))
					.isEqualTo("SELECT count(e) FROM context e");
		}

		@Test // DATAJPA-342
		void createsCountQueryForDistinctQueries() {

			assertThat(createCountQueryFor("select distinct u from User u where u.foo = ?1"))
					.isEqualTo("select count(distinct u) from User u where u.foo = ?1");
			assertThat(createCountQueryFor("select distinct m.genre from Media m where m.user = ?1 order by m.genre asc"))
					.isEqualTo("select count(distinct m.genre) from Media m where m.user = ?1");
		}

		@Test // GH-3269
		void createsCountQueryForDistinctSelectAliases() {

			assertThat(createCountQueryFor("select distinct 1 as x from Employee e"))
					.isEqualTo("select count(distinct 1) from Employee e");
			assertThat(createCountQueryFor("SELECT DISTINCT abc AS x FROM T t"))
					.isEqualTo("SELECT count(DISTINCT abc) FROM T t");
			assertThat(createCountQueryFor("select distinct a as x, b as y from Employee e"))
					.isEqualTo("select count(distinct a, b) from Employee e");
			assertThat(createCountQueryFor("select distinct sum(amount) as x from Employee e GROUP BY n"))
					.isEqualTo("select count(distinct sum(amount)) from Employee e GROUP BY n");
			assertThat(createCountQueryFor("select distinct a, b, sum(amount) as c, d from Employee e GROUP BY n"))
					.isEqualTo("select count(distinct a, b, sum(amount), d) from Employee e GROUP BY n");
			assertThat(createCountQueryFor("select distinct a, count(b) as c from Employee e GROUP BY n"))
					.isEqualTo("select count(distinct a, count(b)) from Employee e GROUP BY n");
		}

		@Test // GH-4341
		void createsCountQueryForConstructorExpressions() {

			assertThat(createCountQueryFor("select distinct new com.example.User(u.name) from User u where u.foo = ?1"))
					.isEqualTo("select count(distinct u) from User u where u.foo = ?1");

			assertThat(createCountQueryFor(
					"select distinct new com.User(u.name) from User u left outer join u.roles r WHERE r = ?1"))
					.isEqualTo("select count(distinct u) from User u left outer join u.roles r WHERE r = ?1");

			assertThat(createCountQueryFor("select distinct new com.example.User(name, lastname) from User where foo = ?1"))
					.isEqualTo("select count(distinct name, lastname) from User where foo = ?1");

			assertThat(createCountQueryFor("select distinct new com.example.User(coalesce(u.name, u.lastname)) from User u where u.foo = ?1"))
				.isEqualTo("select count(distinct coalesce(u.name, u.lastname)) from User u where u.foo = ?1");

			assertThat(createCountQueryFor("select distinct new com.example.User(coalesce(u.name, u.lastname), 10) from User u where u.foo = ?1"))
				.isEqualTo("select count(distinct coalesce(u.name, u.lastname), 10) from User u where u.foo = ?1");

			assertThat(createCountQueryFor("select distinct new com.example.User(cast(u.age as string)) from User u"))
				.isEqualTo("select count(distinct cast(u.age as string)) from User u");

			assertThat(createCountQueryFor("select distinct new com.example.User(coalesce(name, lastname)) from User where foo = ?1"))
				.isEqualTo("select count(distinct coalesce(name, lastname)) from User where foo = ?1");
		}

		@Test // DATAJPA-343
		void createsCountQueryForQueriesWithSubselects() {

			assertThat(
					createCountQueryFor("select u from User u left outer join u.roles r where r in (select r from Role r)"))
					.isEqualTo("select count(u) from User u left outer join u.roles r where r in (select r from Role r)");
			assertThat(createCountQueryFor("select o from Foo o where cb.id in (select b from Bar b)"))
					.isEqualTo("select count(o) from Foo o where cb.id in (select b from Bar b)");
		}

		@Test // DATAJPA-456, GH-4341
		void usesGivenCountProjection() {

			assertThat(createCountQueryFor("select p.lastname,p.firstname from Person p", "p.lastname"))
					.isEqualTo("select count(p.lastname) from Person p");
			assertThat(createCountQueryFor("select distinct p.lastname, p.firstname from Person p", "p.lastname"))
					.isEqualTo("select count(distinct p.lastname) from Person p");
		}

		@Test // GH-2511
		void usesQueryAliasInCountProjection() {

			assertThat(createCountQueryFor("SELECT e FROM User e WHERE created_at > $1"))
					.isEqualTo("SELECT count(e) FROM User e WHERE created_at > $1");
			assertThat(
					createCountQueryFor("SELECT t FROM mytable t WHERE nr = :number AND kon = :kon AND datum >= '2019-01-01'"))
					.isEqualTo("SELECT count(t) FROM mytable t WHERE nr = :number AND kon = :kon AND datum >= '2019-01-01'");
			assertThat(createCountQueryFor("select s FROM users_statuses s WHERE (user_created_at BETWEEN $1 AND $2)"))
					.isEqualTo("select count(s) FROM users_statuses s WHERE (user_created_at BETWEEN $1 AND $2)");
			assertThat(
					createCountQueryFor("SELECT us FROM users_statuses us WHERE (user_created_at BETWEEN :fromDate AND :toDate)"))
					.isEqualTo("SELECT count(us) FROM users_statuses us WHERE (user_created_at BETWEEN :fromDate AND :toDate)");
		}

		@Test // DATAJPA-736
		void supportsNonAsciiCharactersInEntityNames() {
			assertThat(createCountQueryFor("select u from Usèr u")).isEqualTo("select count(u) from Usèr u");
		}

		@Test // DATAJPA-1500, GH-2393
		void createsCountQueryIgnoringLineBreaks() {

			assertThat(createCountQueryFor(" \nselect u from User u where u.age > :age"))
					.isEqualTo("select count(u) from User u where u.age > :age");

			assertThat(createCountQueryFor("""
					select user from User user
					 where user.age = 18
					 order by user.name
					""")).isEqualToIgnoringWhitespace("""
					select count(user) from User user
					 where user.age = 18
					""");

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

			assertThat(createCountQueryFor("select distinct	user.age, user.name	from User user"))
					.isEqualTo(createCountQueryFor("select distinct user.age, user.name from User user"));
		}

		@Test // GH-2341
		void createsCountQueryForDistinctQueryWithLineBreaks() {

			assertThat(createCountQueryFor(
					"SELECT DISTINCT entity1\nFROM Entity1 entity1\nLEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key"))
					.isEqualTo(
							"SELECT count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key");
			assertThat(createCountQueryFor(
					"SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key"))
					.isEqualTo(
							"SELECT count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key");
			assertThat(createCountQueryFor(
					"SELECT DISTINCT entity1\nFROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key\nwhere entity1.id = 1799"))
					.isEqualTo(
							"SELECT count(DISTINCT entity1) FROM Entity1 entity1 LEFT JOIN entity1.entity2 entity2 ON entity1.key = entity2.key where entity1.id = 1799");
		}

	}

	@Nested
	class AliasDetection {

		@Test // GH-2260, GH-3902
		void detectsAlias() {

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
			assertThat(alias("select u, (select u2 from User u2) from User u")).isEqualTo("u");
			assertThat(alias("select firstname from User where not exists (select u2 from User u2)")).isNull();
			assertThat(alias("select firstname from User UNION select lastname from User b")).isNull();
			assertThat(alias("select firstname from User UNION select lastname from User UNION select lastname from User b"))
					.isNull();
		}

		@Test // GH-2074
		void detectsPrimaryAliasAmidstJoinAliases() {
			assertThat(alias("select u from User as u left join  u.roles as r")).isEqualTo("u");
		}

		@Test // DATAJPA-798, GH-2563
		void detectsAliasWithLineBreaks() {

			assertThat(alias("select \n u \n from \n User \nu")).isEqualTo("u");

			assertThat(alias("""
					SELECT o
					FROM Order o
					WHERE EXISTS( SELECT 1
					    FROM Vehicle vehicle
					    WHERE vehicle.vehicleOrderId = o.id
					    AND LOWER(COALESCE(vehicle.make, '')) LIKE :query)
					""")).isEqualTo("o");
		}

		@Test // DATAJPA-1506
		void detectsAliasWithGroupByAndOrderBy() {

			assertThat(alias("select u from User u group by name")).isEqualTo("u");
			assertThat(alias("select u from User u order by name")).isEqualTo("u");
			assertThat(alias("select u from User u group\nby name")).isEqualTo("u");
			assertThat(alias("select u from User u order\nby name")).isEqualTo("u");
			assertThat(alias("select u from User\nu\norder \n by name")).isEqualTo("u");

			assertThatExceptionOfType(BadJpqlGrammarException.class)
					.isThrownBy(() -> alias("select * from User group by name"));
			assertThatExceptionOfType(BadJpqlGrammarException.class)
					.isThrownBy(() -> alias("select * from User order by name"));
			assertThatExceptionOfType(BadJpqlGrammarException.class)
					.isThrownBy(() -> alias("select * from User group\nby name"));
			assertThatExceptionOfType(BadJpqlGrammarException.class)
					.isThrownBy(() -> alias("select * from User order\nby name"));
		}

		@ParameterizedTest // GH-2864
		@CsvSource({ "right, rt", "left, lt", "outer, ou", "inner, inr" })
		void parsesReservedWordAsRelationshipName(String relationshipName, String joinAlias) {

			assertThat(alias("""
					select u
					from UserAccountEntity u
					join u.lossInspectorLimitConfiguration lil
					join u.companyTeam ct
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
					""".formatted(relationshipName, joinAlias, joinAlias))).isEqualTo("u");
		}

	}

	@Nested
	class Projections {

		@Test // DATAJPA-1679, DATAJPA-1696
		void extractsProjection() {

			SoftAssertions.assertSoftly(softly -> {
				softly.assertThat(createQueryEnhancer("select a,b,c from Entity x").getProjection()).isEqualTo("a, b, c");
				softly.assertThat(createQueryEnhancer("select a, b, c from Entity x").getProjection()).isEqualTo("a, b, c");
				softly.assertThat(createQueryEnhancer("select distinct a, b, c from Entity x").getProjection())
						.isEqualTo("a, b, c");
				softly.assertThat(createQueryEnhancer("select DISTINCT a, b, c from Entity x").getProjection())
						.isEqualTo("a, b, c");
				softly.assertThat(createQueryEnhancer("select x, frommage, y from Element t").getProjection())
						.isEqualTo("x, frommage, y");
			});
		}

		/**
		 * Not required behavior. Documents a current limitation of the parser.
		 */
		@Test // DATAJPA-1696
		void rejectsProjectionFromSubqueryInFromClause() {

			assertThatExceptionOfType(BadJpqlGrammarException.class)
					.isThrownBy(() -> createQueryEnhancer("select * from (select x from y)").getProjection());
		}

		@Test // DATAJPA-938
		void detectsConstructorExpression() {

			assertThat(
					createQueryEnhancer("select distinct new com.example.Foo(b.name) from Bar b").hasConstructorExpression())
					.isTrue();
			assertThat(createQueryEnhancer("select new foo.bar.FooBar(\na.id) from DtoA a ").hasConstructorExpression())
					.isTrue();
			assertThat(createQueryEnhancer(
					"""
							select new foo.bar.Foo(ip.id, ip.name, sum(lp.amount))
							from Bar lp join lp.investmentProduct ip
							where (lp.toDate is null and lp.fromDate <= :now and lp.fromDate is not null) and lp.accountId = :accountId group by ip.id, ip.name, lp.accountId
							order by ip.name ASC""")
					.hasConstructorExpression()).isTrue();
		}

	}

}
