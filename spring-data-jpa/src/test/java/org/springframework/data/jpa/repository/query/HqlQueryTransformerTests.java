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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.StringUtils;

/**
 * HQL-specific query transformation tests through {@link JpaQueryEnhancer.HqlQueryParser}. Shared cases live in
 * {@link AbstractQueryTransformerTests}.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class HqlQueryTransformerTests extends AbstractQueryTransformerTests {

	@Override
	QueryEnhancer createQueryEnhancer(String query) {
		return JpaQueryEnhancer.forHql(query);
	}

	@Override
	String countProjectionWithoutAlias(String firstSelection) {
		return "count(*)";
	}

	@Test // GH-2260
	void appliesSortToWindowFunctionQueries() {

		Sort sort = Sort.by(Order.desc("age"));

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

	@Test // GH-2045, GH-2496, GH-2522, GH-2537
	void appliesSortToQueriesWithHqlSubselects() {

		Sort sort = Sort.by(Order.desc("age"));

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
	void appendsSortToOrderByWithFunction() {

		assertThat(createQueryFor(
				"select e from SampleEntity e  where function('nativeFunc', ?1) > 'testVal'  order by function('nativeFunc', ?1)",
				Sort.by(Order.desc("age")))).isEqualTo(
						"select e from SampleEntity e where function('nativeFunc', ?1) > 'testVal' order by function('nativeFunc', ?1), e.age desc");
	}

	@Test // GH-2626, GH-2863
	void sortsByAliasedColumn() {

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
				""", Sort.by(Direction.DESC, "uuid"))).endsWith("order by uuid desc");
	}

	@Test // GH-2322, GH-2863
	void sortsByAliasedFunctions() {

		assertThat(createQueryFor("""
				SELECT
					DISTINCT(event.id) as id,
					event.name as name,
					MIN(bundle.base_price_amount) as cheapestBundlePrice,
					MIN(DATE(bundle.start)) as earliestBundleStart
				FROM event event
				LEFT JOIN bundle bundle ON event.id = bundle.event_id
				GROUP BY event.id
				""", Sort.by(Direction.ASC, "cheapestBundlePrice") //
				.and(Sort.by(Direction.ASC, "earliestBundleStart")) //
				.and(Sort.by(Direction.ASC, "name"))))
				.endsWith(" order by cheapestBundlePrice asc, earliestBundleStart asc, name asc");
	}

	@Test // GH-1655, GH-2863
	void sortsByAliasInsideCaseExpression() {

		Sort sort = PageRequest.of(0, 20, Direction.DESC, "newDateDue").getSort();

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
	void appliesSortToFromQueryWithoutPrimaryAlias() {

		assertThat(createQueryFor("FROM Story WHERE enabled = true", Sort.by(Direction.DESC, "created")))
				.isEqualTo("FROM Story WHERE enabled = true order by created desc");
	}

	@Test // GH-3054
	void doesNotConfuseAliasesWithSortProperties() {

		assertThat(createQueryFor("select e from Employee e where e.name = :name", Sort.by(Order.desc("evaluationDate"))))
				.isEqualToIgnoringWhitespace("select e from Employee e where e.name = :name order by e.evaluationDate desc");

		assertThat(createQueryFor("select e from Employee e join training t where e.name = :name",
				Sort.by(Order.desc("trainingDueDate")))).isEqualToIgnoringWhitespace(
						"select e from Employee e join training t where e.name = :name order by e.trainingDueDate desc");

		assertThat(createQueryFor("select e from Employee e join training t where e.name = :name",
				Sort.by(Order.desc("t.trainingDueDate")))).isEqualToIgnoringWhitespace(
						"select e from Employee e join training t where e.name = :name order by t.trainingDueDate desc");

		assertThat(createQueryFor("SELECT t3 FROM Test3 t3 JOIN t3.test2 t2 JOIN t2.test1 test WHERE test.id = :test1Id",
				Sort.by(Order.desc("testDuplicateColumnName")))).isEqualToIgnoringWhitespace(
						"SELECT t3 FROM Test3 t3 JOIN t3.test2 t2 JOIN t2.test1 test WHERE test.id = :test1Id order by t3.testDuplicateColumnName desc");

		assertThat(createQueryFor("SELECT t3 FROM Test3 t3 JOIN t3.test2 x WHERE x.id = :test2Id",
				Sort.by(Order.desc("t3.testDuplicateColumnName")))).isEqualToIgnoringWhitespace(
						"SELECT t3 FROM Test3 t3 JOIN t3.test2 x WHERE x.id = :test2Id order by t3.testDuplicateColumnName desc");
	}

	@Test // GH-3427, GH-4342
	void appliesSortToParenthesizedSetQuery() {

		String source = "(SELECT tb FROM Test tb WHERE tb.type = 'A') UNION (SELECT tb FROM Test tb WHERE tb.type = 'B') UNION (SELECT tb FROM Test tb WHERE tb.type = 'C')";

		assertThat(createQueryFor(source, Sort.by("Type").ascending()))
				.isEqualTo("(SELECT tb FROM Test tb WHERE tb.type = 'A') " //
						+ "UNION (SELECT tb FROM Test tb WHERE tb.type = 'B') "
						+ "UNION (SELECT tb FROM Test tb WHERE tb.type = 'C') order by tb.Type asc");

		assertThat(createQueryFor("(SELECT tb FROM Test tb WHERE tb.type = 'A')", Sort.by("Type").ascending()))
				.isEqualTo("(SELECT tb FROM Test tb WHERE tb.type = 'A') order by tb.Type asc");
	}

	@ParameterizedTest // GH-3427
	@ValueSource(strings = { "", "res" })
	void appendsSortToSubselectWithSetOperator(String alias) {

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

	@Test // GH-3504
	void appliesSortToQueryWithCte() {

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

	@ParameterizedTest // GH-2977, GH-3649
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
			""" })
	void doesNotRewriteDmlStatements(String query) {
		assertThat(createQueryFor(query, Sort.unsorted())).isEqualToIgnoringWhitespace(query);
	}

	@Test // GH-3536
	void createsCountQueryForDistinctFunctionCall() {

		assertThat(createCountQueryFor("""
				select distinct cast(e.timestampField as date) as foo
				from ExampleEntity e
				order by cast(e.timestampField as date) desc
				""")).isEqualTo("select count(distinct cast(e.timestampField as date)) from ExampleEntity e");
	}

	@Test // GH-3504, GH-3726
	void createsCountQueryForCte() {

		assertThat(createCountQueryFor("""
				WITH cte_select AS (select u.firstname as firstname, u.lastname as lastname from User u)
						SELECT new org.springframework.data.jpa.repository.sample.UserExcerptDto(c.firstname, c.lastname)
						FROM cte_select c
				""")).isEqualToIgnoringWhitespace(
				"WITH cte_select AS (select u.firstname as firstname, u.lastname as lastname from User u) SELECT count(*) FROM cte_select c");

		assertThat(createCountQueryFor("""
				WITH maxId AS(select max(sr.snapshot.id) snapshotId from SnapshotReference sr
					where sr.id.selectionId = ?1 and sr.enabled
					group by sr.userId)
				select sr from maxId m join SnapshotReference sr on sr.snapshot.id = m.snapshotId
				""")).startsWith("WITH maxId AS (select max(sr.snapshot.id) snapshotId from SnapshotReference sr")
				.endsWith("select count(*) from maxId m join SnapshotReference sr on sr.snapshot.id = m.snapshotId");
	}

	@Test // GH-4341
	void createsDistinctCountQueryForCte() {

		assertThat(createCountQueryFor("""
				WITH cte_select AS (select u.firstname as firstname, u.lastname as lastname from User u)
						SELECT DISTINCT new org.springframework.data.jpa.repository.sample.UserExcerptDto(c.firstname, c.lastname)
						FROM cte_select c
				""")).isEqualToIgnoringWhitespace(
				"WITH cte_select AS (select u.firstname as firstname, u.lastname as lastname from User u) SELECT count(DISTINCT c.firstname, c.lastname) FROM cte_select c");
	}

	@Test // GH-4341
	void createsDistinctCountQueryForSetReturningFunction() {

		assertThat(createCountQueryFor(
				"select distinct new com.example.Dto(x.id, x.value) from some_function(:date, :integerValue) x"))
				.isEqualTo("select count(distinct x.id, x.value) from some_function(:date, :integerValue) x");
	}

	@Test // GH-3902
	void createsCountQueryForTrailingSelectClause() {

		assertThat(createCountQueryFor("from User u left outer join u.roles r where r in (select r from Role r) select u "))
				.isEqualTo("from User u left outer join u.roles r where r in (select r from Role r) select count(u)");
	}

	@Test // GH-3269, GH-3689
	void createsCountQueryForDistinctSelectAliasesWithoutPrimaryAlias() {

		assertThat(createCountQueryFor("select distinct 1 as x from Employee"))
				.isEqualTo("select count(distinct 1) from Employee");
		assertThat(createCountQueryFor("SELECT DISTINCT abc AS x FROM T")).isEqualTo("SELECT count(DISTINCT abc) FROM T");
		assertThat(createCountQueryFor("select distinct a as x, b as y from Employee"))
				.isEqualTo("select count(distinct a, b) from Employee");
		assertThat(createCountQueryFor("select distinct sum(amount) as x from Employee GROUP BY n"))
				.isEqualTo("select count(distinct sum(amount)) from Employee GROUP BY n");
		assertThat(createCountQueryFor("select distinct a, b, sum(amount) as c, d from Employee GROUP BY n"))
				.isEqualTo("select count(distinct a, b, sum(amount), d) from Employee GROUP BY n");
		assertThat(createCountQueryFor("select distinct a, count(b) as c from Employee GROUP BY n"))
				.isEqualTo("select count(distinct a, count(b)) from Employee GROUP BY n");
		assertThat(createCountQueryFor(
				"select distinct substring(e.firstname, 1, position('a' in e.lastname)) as x from from Employee"))
				.isEqualTo("select count(distinct substring(e.firstname, 1, position('a' in e.lastname))) from from Employee");
	}

	@Test // GH-2348
	void removesFetchJoinsFromCountQuery() {

		assertThat(createCountQueryFor(
				"select u from User u left outer join fetch u.roles r left outer JOIN   FETCH  u.accounts a"))
				.isEqualTo("select count(u) from User u left outer join u.roles r left outer JOIN u.accounts a");
		assertThat(createCountQueryFor("SELECT DISTINCT b FROM Board b LEFT JOIN FETCH b.comments ORDER BY b.id"))
				.isEqualTo("SELECT count(DISTINCT b) FROM Board b LEFT JOIN b.comments");
	}

	@Test // GH-3864
	void createsCountQueryForSetReturningFunction() {

		assertThat(createCountQueryFor("select x.id, x.value from some_function(:date, :integerValue) x"))
				.contains("select count(*) from some_function(:date, :integerValue) x");
		assertThat(createCountQueryFor("select id, value from some_function(:date, :integerValue)"))
				.contains("select count(*) from some_function(:date, :integerValue)");
	}

	@Test // GH-2508
	void detectsAliasWithCast() {

		assertThat(alias("from User u where (cast(:effective as date) is null) OR :effective >= u.createdAt"))
				.isEqualTo("u");
		assertThat(alias("from User u where (cast(:effectiveDate as date) is null) OR :effectiveDate >= u.createdAt"))
				.isEqualTo("u");
		assertThat(alias("from User u where (cast(:effectiveFrom as date) is null) OR :effectiveFrom >= u.createdAt"))
				.isEqualTo("u");
	}

	@Test // GH-2260, GH-3902
	void detectsAliasWithTreatAndSubqueryJoin() {

		assertThat(alias(
				"SELECT e FROM DbEvent e WHERE TREAT(modifiedFrom AS date) IS NULL OR e.modificationDate >= :modifiedFrom"))
				.isEqualTo("e");
		assertThat(alias("select firstname from User JOIN (select u2 from User u2) u2")).isNull();
	}

}
