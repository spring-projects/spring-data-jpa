/*
 * Copyright 2022-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests built around examples of HQL found in <a href=
 * "https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc">...</a> and
 * <a href=
 * "https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#query-language">...</a><br/>
 * <br/>
 * IMPORTANT: Purely verifies the parser without any transformations.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Yannick Brandt
 * @author Oscar Fanchin
 * @since 3.1
 */
class HqlQueryRendererTests extends JpqlQueryRendererTckTests {

	/**
	 * Parse the query using {@link HqlParser} then run it through the query-preserving {@link HqlQueryRenderer}.
	 */
	@Override
	String parseWithoutChanges(String query) {

		JpaQueryEnhancer.HqlQueryParser parser = JpaQueryEnhancer.HqlQueryParser.parseQuery(query);

		QueryTokenStream tokens = new HqlQueryRenderer().visit(parser.getContext());
		return QueryRenderer.from(tokens).render();
	}

	static Stream<Arguments> reservedWords() {
		return Stream.of("abs", "exp", "any", "case", "else", "index", "time").map(Arguments::of);
	}

	@Test // GH-3711, GH-2970
	void entityTypeReference() {

		super.entityTypeReference();

		assertQuery("""
				SELECT TYPE(e)
				FROM Employee e
				WHERE TYPE(e) ^= Exempt
				""");
	}

	@Test // GH-3711
	void entityIdReference() {

		assertQuery("""
				SELECT ID(e)
				FROM Employee e
				""");

		assertQuery("""
				SELECT ID(e).foo
				FROM Employee e
				""");
	}

	@Test // GH-3711
	void entityNaturalIdReference() {

		assertQuery("""
				SELECT NATURALID(e)
				FROM Employee e
				""");

		assertQuery("""
				SELECT NATURALID(e).foo
				FROM Employee e
				""");
	}

	@Test // GH-3711
	void entityVersionReference() {

		assertQuery("""
				SELECT VERSION(e)
				FROM Employee e
				""");
	}

	@Test // GH-3711
	void collectionValueNavigablePath() {

		assertQuery("""
				SELECT ELEMENT(e)
				FROM Employee e
				""");

		assertQuery("""
				SELECT ELEMENT(e).foo
				FROM Employee e
				""");

		assertQuery("""
				SELECT VALUE(e)
				FROM Employee e
				""");

		assertQuery("""
				SELECT VALUE(e).foo
				FROM Employee e
				""");
	}

	@Test // GH-3711
	void mapKeyNavigablePath() {

		assertQuery("""
				SELECT KEY(e)
				FROM Employee e
				""");

		assertQuery("""
				SELECT KEY(e).foo
				FROM Employee e
				""");

		assertQuery("""
				SELECT INDEX(e)
				FROM Employee e
				""");
	}

	@Test // GH-3711
	void toOneFkReference() {

		assertQuery("""
				SELECT FK(e)
				FROM Employee e
				""");

		assertQuery("""
				SELECT FK(e.foo)
				FROM Employee e
				""");
	}

	@Test // GH-3711
	void indexedPathAccessFragment() {

		assertQuery("""
				SELECT e.names[0]
				FROM Employee e
				""");

		assertQuery("""
				SELECT e.payments[1].id
				FROM Employee e
				""");

		assertQuery("""
				SELECT some_function()[0]
				FROM Employee e
				""");

		assertQuery("""
				SELECT some_function()[1].id
				FROM Employee e
				""");
	}

	@Test // GH-3711
	void slicedPathAccessFragment() {

		assertQuery("""
				SELECT e.names[0:1]
				FROM Employee e
				""");

		assertQuery("""
				SELECT e.payments[1:2].id
				FROM Employee e
				""");

		assertQuery("""
				SELECT some_function()[0:1]
				FROM Employee e
				""");

		assertQuery("""
				SELECT some_function()[1:2].id
				FROM Employee e
				""");
	}

	@Test // GH-3711
	void functionPathContinuation() {

		assertQuery("""
				SELECT some_function().foo
				FROM Employee e
				""");
	}

	@ParameterizedTest // GH-3689
	@ValueSource(strings = { "RESPECT NULLS", "IGNORE NULLS" })
	void generic(String nullHandling) {

		// not in the official documentation but supported in the grammar.
		assertQuery("""
				SELECT e FROM Employee e
				WHERE FOO(x).bar %s
				""".formatted(nullHandling));
	}

	@Test // GH-3689
	void size() {

		assertQuery("""
				SELECT e FROM Employee e
				WHERE SIZE(x) > 1
				""");

		assertQuery("""
				SELECT e FROM Employee e
				WHERE SIZE(e.skills) > 1
				""");
	}

	@Test // GH-3689
	void collectionAggregate() {

		assertQuery("""
				SELECT e FROM Employee e
				WHERE MAXELEMENT(foo) > MINELEMENT(bar)
				""");

		assertQuery("""
				SELECT e FROM Employee e
				WHERE MININDEX(foo) > MAXINDEX(bar)
				""");
	}

	@Test // GH-3689
	void trunc() {

		assertQuery("""
				SELECT e FROM Employee e
				WHERE TRUNC(x) = TRUNCATE(y)
				""");

		assertQuery("""
				SELECT e FROM Employee e
				WHERE TRUNC(e, 'foo') = TRUNCATE(e, 'bar')
				""");

		assertQuery("""
				SELECT e FROM Employee e
				WHERE TRUNC(e, 'YEAR') = TRUNCATE(LOCAL DATETIME, 'YEAR')
				""");
	}

	@ParameterizedTest // GH-3689
	@ValueSource(strings = { "YEAR", "MONTH", "DAY", "WEEK", "QUARTER", "HOUR", "MINUTE", "SECOND", "NANOSECOND",
			"NANOSECOND", "EPOCH" })
	void trunc(String truncation) {

		assertQuery("""
				SELECT e FROM Employee e
				WHERE TRUNC(e, %1$s) = TRUNCATE(e, %1$s)
				""".formatted(truncation));
	}

	@Test // GH-3689
	void format() {

		assertQuery("""
				SELECT e FROM Employee e
				WHERE FORMAT(x AS 'yyyy') = FORMAT(e.hiringDate AS 'yyyy')
				""");

		assertQuery("""
				SELECT e FROM Employee e
				WHERE e.hiringDate = format(LOCAL DATETIME as 'yyyy-MM-dd')
				""");

		assertQuery("""
				SELECT e FROM Employee e
				WHERE e.hiringDate = format(LOCAL_DATE() as 'yyyy-MM-dd')
				""");
	}

	@Test // GH-3689
	void collate() {

		assertQuery("""
				SELECT e FROM Employee e
				WHERE COLLATE(x AS ucs_basic) = COLLATE(e.name AS ucs_basic)
				""");
	}

	@Test // GH-3689
	void substring() {

		super.substring();

		assertQuery("select substring(c.number, 1, position('/0' in c.number)) " + //
				"from Call c");

		assertQuery("select substring(c.number FROM 1 FOR 2) " + //
				"from Call c");

		assertQuery("select substring(c.number FROM 1) " + //
				"from Call c");

		assertQuery("select substring(c.number FROM 1 FOR position('/0' in c.number)) " + //
				"from Call c");

		assertQuery("select substring(c.number FROM 1) AS shortNumber " + //
				"from Call c");
	}

	@Test // GH-3689
	void overlay() {

		assertQuery("select OVERLAY(c.number PLACING 1 FROM 2) " + //
				"from Call c ");

		assertQuery("select OVERLAY(p.number PLACING 1 FROM 2 FOR 3) " + //
				"from Call c ");
	}

	@Test // GH-3689
	void pad() {

		assertQuery("select PAD(c.number WITH 1 LEADING) " + //
				"from Call c ");

		assertQuery("select PAD(c.number WITH 1 TRAILING) " + //
				"from Call c ");

		assertQuery("select PAD(c.number WITH 1 LEADING '0') " + //
				"from Call c ");

		assertQuery("select PAD(c.number WITH 1 TRAILING '0') " + //
				"from Call c ");
	}

	@Test // GH-3689
	void position() {

		assertQuery("select POSITION(c.number IN 'foo') " + //
				"from Call c ");

		assertQuery("select POSITION(c.number IN 'foo') + 1 AS pos " + //
				"from Call c ");
	}

	@Test // GH-3689
	void currentDateFunctions() {

		assertQuery("select CURRENT DATE, CURRENT_DATE() " + //
				"from Call c ");

		assertQuery("select CURRENT TIME, CURRENT_TIME() " + //
				"from Call c ");

		assertQuery("select CURRENT TIMESTAMP, CURRENT_TIMESTAMP() " + //
				"from Call c ");

		assertQuery("select INSTANT, CURRENT_INSTANT() " + //
				"from Call c ");

		assertQuery("select LOCAL DATE, LOCAL_DATE() " + //
				"from Call c ");

		assertQuery("select LOCAL TIME, LOCAL_TIME() " + //
				"from Call c ");

		assertQuery("select LOCAL DATETIME, LOCAL_DATETIME() " + //
				"from Call c ");

		assertQuery("select OFFSET DATETIME, OFFSET_DATETIME() " + //
				"from Call c ");

		assertQuery("select OFFSET DATETIME AS offsetDatetime, OFFSET_DATETIME() AS offset_datetime " + //
				"from Call c ");
	}

	@Test // GH-3689
	void cube() {

		assertQuery("select CUBE(foo), CUBE(foo, bar) " + //
				"from Call c ");

		assertQuery("select c.callerId from Call c GROUP BY CUBE(state, province)");
	}

	@Test // GH-3689
	void rollup() {

		assertQuery("select ROLLUP(foo), ROLLUP(foo, bar) " + //
				"from Call c ");

		assertQuery("select c.callerId from Call c GROUP BY ROLLUP(state, province)");
	}

	@Test // GH-3689
	void everyAll() {

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE EVERY (SELECT spouseEmp
				 FROM Employee spouseEmp) > 1
				""");

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE ALL (SELECT spouseEmp
				 FROM Employee spouseEmp) > 1
				""");

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE ALL (foo > 1) OVER (PARTITION BY bar)
				""");

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE ALL VALUES(foo) > 1
				""");

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE ALL ELEMENTS(foo) > 1
				""");
	}

	@Test // GH-3689
	void anySome() {

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE ANY (SELECT spouseEmp
				 FROM Employee spouseEmp) > 1
				""");

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE SOME (SELECT spouseEmp
				 FROM Employee spouseEmp) > 1
				""");

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE ANY (foo > 1) OVER (PARTITION BY bar)
				""");

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE ANY VALUES(foo) > 1
				""");
	}

	@Test // GH-3689
	void listAgg() {

		assertQuery("select listagg(p.number, ', ') within group (order by p.type, p.number) " + //
				"from Phone p " + //
				"group by p.person");
	}

	/**
	 * @see #fromClauseDowncastingExample3fixed()
	 */
	@Test
	void fromClauseDowncastingExample3() {

		assertQuery("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				 OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				 OR p.description LIKE "cost overrun"
				""");

		assertQuery("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				 OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				 OR p.description LIKE 'cost overrun'
				""");
	}

	@ParameterizedTest // GH-3628
	@ValueSource(strings = { "is true", "is not true", "is false", "is not false" })
	void functionInvocationWithIsBoolean(String booleanComparison) {

		assertQuery("""
				from RoleTmpl where find_in_set(:appId, appIds) %s
				""".formatted(booleanComparison));
	}

	@Test // GH-3628
	void booleanPredicate() {

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE c.orders IS TRUE
				""");

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE c.orders IS NOT TRUE
				""");

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE c.orders IS FALSE
				""");

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE c.orders IS NOT FALSE
				""");

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE c.orders IS NULL
				""");

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE c.orders IS NOT NULL
				""");
	}

	@ParameterizedTest // GH-3628
	@ValueSource(strings = { "IS DISTINCT FROM", "IS NOT DISTINCT FROM" })
	void distinctFromPredicate(String distinctFrom) {

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE c.orders %s c.payments
				""".formatted(distinctFrom));

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE c.orders %s c.payments
				""".formatted(distinctFrom));

		assertQuery("""
				SELECT c
				FROM Customer c
				GROUP BY c.lastname
				HAVING c.orders %s c.payments
				""".formatted(distinctFrom));

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE EXISTS (SELECT c2
				 FROM Customer c2
				  WHERE c2.orders %s c.orders)
				""".formatted(distinctFrom));
	}

	@Test // GH-3689
	void insertQueries() {

		assertQuery("insert Person (id, name) values (100L, 'Jane Doe')");

		assertQuery("insert Person (id, name) values " + //
				"(101L, 'J A Doe III'), " + //
				"(102L, 'J X Doe'), " + //
				"(103L, 'John Doe, Jr')");

		assertQuery("insert into Partner (id, name) " + //
				"select p.id, p.name from Person p ");

		assertQuery("INSERT INTO AggregationPrice (range, price, type) " + "VALUES (:range, :price, :priceType) "
				+ "ON CONFLICT (range) DO UPDATE  SET price = :price, type = :priceType");

		assertQuery("INSERT INTO AggregationPrice (range, price, type) " + "VALUES (:range, :price, :priceType) "
				+ "ON CONFLICT ON CONSTRAINT foo DO UPDATE  SET price = :price, type = :priceType");

		assertQuery("INSERT INTO AggregationPrice (range, price, type) " + "VALUES (:range, :price, :priceType) "
				+ "ON CONFLICT ON CONSTRAINT foo DO NOTHING");
	}

	@Test
	void hqlQueries() {

		assertQuery("from Person");
		assertQuery("select local datetime");
		assertQuery("from Person p select p.name");
		assertQuery("update Person set nickName = 'Nacho' " + //
				"where name = 'Ignacio'");
		assertQuery("update Person p " + //
				"set p.name = :newName " + //
				"where p.name = :oldName");
		assertQuery("update Person " + //
				"set name = :newName " + //
				"where name = :oldName");
		assertQuery("update versioned Person " + //
				"set name = :newName " + //
				"where name = :oldName");
		assertQuery("insert Person (id, name) " + //
				"values (100L, 'Jane Doe')");
		assertQuery("insert Person (id, name) " + //
				"values (101L, 'J A Doe III'), " + //
				"(102L, 'J X Doe'), " + //
				"(103L, 'John Doe, Jr')");
		assertQuery("insert into Partner (id, name) " + //
				"select p.id, p.name " + //
				"from Person p ");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.name like 'Joe'");

		assertQuery("select p " + //
				"from Person p " + //
				"where p.name ilike 'Joe'");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.name like 'Joe''s'");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.id = 1");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.id = 1L");
		assertQuery("select c " + //
				"from Call c " + //
				"where c.duration > 100.5");
		assertQuery("select c " + //
				"from Call c " + //
				"where c.duration > 100.5F");
		assertQuery("select c " + //
				"from Call c " + //
				"where c.duration > 1e+2");
		assertQuery("select c " + //
				"from Call c " + //
				"where c.duration > 1e+2F");
		assertQuery("from Phone ph " + //
				"where ph.type = LAND_LINE");
		assertQuery("select java.lang.Math.PI");
		assertQuery("select 'Customer ' || p.name " + //
				"from Person p " + //
				"where p.id = 1");
		assertQuery("select sum(ch.duration) * :multiplier " + //
				"from Person pr " + //
				"join pr.phones ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = 1L ");
		assertQuery("select year(local date) - year(p.createdOn) " + //
				"from Person p " + //
				"where p.id = 1L");
		assertQuery("select p " + //
				"from Person p " + //
				"where year(local date) - year(p.createdOn) > 1");
		assertQuery("select " + //
				"	case p.nickName " + //
				"	when 'NA' " + //
				"	then '<no nick name>' " + //
				"	else p.nickName " + //
				"	end " + //
				"from Person p");
		assertQuery("select " + //
				"	case " + //
				"	when p.nickName is null " + //
				"	then " + //
				"		case " + //
				"		when p.name is null " + //
				"		then '<no nick name>' " + //
				"		else p.name " + //
				"		end" + //
				"	else p.nickName " + //
				"	end " + //
				"from Person p");
		assertQuery("select " + //
				"	case when p.nickName is null " + //
				"		 then p.id * 1000 " + //
				"		 else p.id " + //
				"	end " + //
				"from Person p " + //
				"order by p.id");
		assertQuery("select p " + //
				"from Payment p " + //
				"where type(p) = CreditCardPayment");
		assertQuery("select p " + //
				"from Payment p " + //
				"where type(p) = :type");
		assertQuery("select p " + //
				"from Payment p " + //
				"where length(treat(p as CreditCardPayment).cardNumber) between 16 and 20");
		assertQuery("select nullif(p.nickName, p.name) " + //
				"from Person p");
		assertQuery("select " + //
				"	case" + //
				"	when p.nickName = p.name" + //
				"	then null" + //
				"	else p.nickName" + //
				"	end " + //
				"from Person p");
		assertQuery("select coalesce(p.nickName, '<no nick name>') " + //
				"from Person p");
		assertQuery("select coalesce(p.nickName, p.name, '<no nick name>') " + //
				"from Person p");
		assertQuery("select p " + //
				"from Person p " + //
				"where size(p.phones) >= 2");
		assertQuery("select concat(p.number, ' : ', cast(c.duration as string)) " + //
				"from Call c " + //
				"join c.phone p");
		assertQuery("select substring(p.number, 1, 2) " + //
				"from Call c " + //
				"join c.phone p");
		assertQuery("select upper(p.name) " + //
				"from Person p ");
		assertQuery("select lower(p.name) " + //
				"from Person p ");
		assertQuery("select trim(p.name) " + //
				"from Person p ");
		assertQuery("select trim(leading ' ' from p.name) " + //
				"from Person p ");
		assertQuery("select length(p.name) " + //
				"from Person p ");
		assertQuery("select locate('John', p.name) " + //
				"from Person p ");
		assertQuery("select abs(c.duration) " + //
				"from Call c ");
		assertQuery("select mod(c.duration, 10) " + //
				"from Call c ");
		assertQuery("select sqrt(c.duration) " + //
				"from Call c ");
		assertQuery("select cast(c.duration as String) " + //
				"from Call c ");
		assertQuery("select str(c.timestamp) " + //
				"from Call c ");
		assertQuery("select str(cast(duration as float) / 60, 4, 2) " + //
				"from Call c ");
		assertQuery("select c " + //
				"from Call c " + //
				"where extract(date from c.timestamp) = local date");
		assertQuery("select extract(year from c.timestamp) " + //
				"from Call c ");
		assertQuery("select year(c.timestamp) " + //
				"from Call c ");
		assertQuery("select var_samp(c.duration) as sampvar, var_pop(c.duration) as popvar " + //
				"from Call c ");
		assertQuery("select bit_length(c.phone.number) " + //
				"from Call c ");
		assertQuery("select c " + //
				"from Call c " + //
				"where c.duration < 30 ");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.name like 'John%' ");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.createdOn > '1950-01-01' ");
		assertQuery("select p " + //
				"from Phone p " + //
				"where p.type = 'MOBILE' ");
		assertQuery("select p " + //
				"from Payment p " + //
				"where p.completed = true ");
		assertQuery("select p " + //
				"from Payment p " + //
				"where type(p) = WireTransferPayment ");
		assertQuery("select p " + //
				"from Payment p, Phone ph " + //
				"where p.person = ph.person ");
		assertQuery("select p " + //
				"from Person p " + //
				"join p.phones ph " + //
				"where p.id = 1L and index(ph) between 0 and 3");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.createdOn between '1999-01-01' and '2001-01-02'");
		assertQuery("select c " + //
				"from Call c " + //
				"where c.duration between 5 and 20");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.name between 'H' and 'M'");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.nickName is not null");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.nickName is null");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.name like 'Jo%'");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.name not like 'Jo%'");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.name like 'Dr|_%' escape '|'");
		assertQuery("select p " + //
				"from Payment p " + //
				"where type(p) in (CreditCardPayment, WireTransferPayment)");
		assertQuery("select p " + //
				"from Phone p " + //
				"where type in ('MOBILE', 'LAND_LINE')");
		assertQuery("select p " + //
				"from Phone p " + //
				"where type in :types");
		assertQuery("select distinct p " + //
				"from Phone p " + //
				"where p.person.id in " + //
				"(select py.person.id " + //
				"	from Payment py" + //
				"	where py.completed = true and py.amount > 50" + //
				")");
		assertQuery("select distinct p " + //
				"from Phone p " + //
				"where p.person in " + //
				"(select py.person " + //
				"	from Payment py" + //
				"	where py.completed = true and py.amount > 50" + //
				")");
		assertQuery("select distinct p " + //
				"from Payment p " + //
				"where (p.amount, p.completed) in (" + //
				"(50, true)," + //
				"	(100, true)," + //
				"	(5, false)" + //
				")");
		assertQuery("select p " + //
				"from Person p " + //
				"where 1 in indices(p.phones)");
		assertQuery("select distinct p.person " + //
				"from Phone p " + //
				"join p.calls c " + //
				"where 50 > all " + //
				"(select duration" + //
				"	from Call" + //
				"	where phone = p" + //
				") ");
		assertQuery("select p " + //
				"from Phone p " + //
				"where local date > all elements(p.repairTimestamps)");
		assertQuery("select p " + //
				"from Person p " + //
				"where :phone = some elements(p.phones)");
		assertQuery("select p " + //
				"from Person p " + //
				"where :phone member of p.phones");
		assertQuery("select p " + //
				"from Person p " + //
				"where exists elements(p.phones)");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.phones is empty");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.phones is not empty");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.phones is not empty");
		assertQuery("select p " + //
				"from Person p " + //
				"where 'Home address' member of p.addresses");
		assertQuery("select p " + //
				"from Person p " + //
				"where 'Home address' not member of p.addresses");
		assertQuery("select p " + //
				"from Person p");
		assertQuery("select p " + //
				"from org.hibernate.userguide.model.Person p");
		assertQuery("select distinct pr, ph " + //
				"from Person pr, Phone ph " + //
				"where ph.person = pr and ph is not null");
		assertQuery("select distinct pr1 " + //
				"from Person pr1, Person pr2 " + //
				"where pr1.id <> pr2.id " + //
				"  and pr1.address = pr2.address " + //
				"  and pr1.createdOn < pr2.createdOn");
		assertQuery("select distinct pr, ph " + //
				"from Person pr cross join Phone ph " + //
				"where ph.person = pr and ph is not null");
		assertQuery("select p " + //
				"from Payment p ");
		assertQuery("select d.owner, d.payed " + //
				"from " + //
				"(select p.person as owner, c.payment is not null as payed " + //
				"  from Call c " + //
				"  join c.phone p " + //
				"  where p.number = :phoneNumber) d");
		assertQuery("select distinct pr " + //
				"from Person pr " + //
				"join Phone ph on ph.person = pr " + //
				"where ph.type = :phoneType");
		assertQuery("select distinct pr " + //
				"from Person pr " + //
				"join pr.phones ph " + //
				"where ph.type = :phoneType");
		assertQuery("select distinct pr " + //
				"from Person pr " + //
				"inner join pr.phones ph " + //
				"where ph.type = :phoneType");
		assertQuery("select distinct pr " + //
				"from Person pr " + //
				"left join pr.phones ph " + //
				"where ph is null " + //
				"or ph.type = :phoneType");
		assertQuery("select distinct pr " + //
				"from Person pr " + //
				"left outer join pr.phones ph " + //
				"where ph is null " + //
				"or ph.type = :phoneType");
		assertQuery("select pr.name, ph.number " + //
				"from Person pr " + //
				"left join pr.phones ph with ph.type = :phoneType ");
		assertQuery("select pr.name, ph.number " + //
				"from Person pr " + //
				"left join pr.phones ph on ph.type = :phoneType ");
		assertQuery("select distinct pr " + //
				"from Person pr " + //
				"left join fetch pr.phones ");
		assertQuery("select a, ccp " + //
				"from Account a " + //
				"join treat(a.payments as CreditCardPayment) ccp " + //
				"where length(ccp.cardNumber) between 16 and 20");
		assertQuery("select c, ccp " + //
				"from Call c " + //
				"join treat(c.payment as CreditCardPayment) ccp " + //
				"where length(ccp.cardNumber) between 16 and 20");
		assertQuery("select longest.duration " + //
				"from Phone p " + //
				"left join lateral " + //
				"(select c.duration as duration " + //
				"  from p.calls c" + //
				"  order by c.duration desc" + //
				"  limit 1) longest " + //
				"where p.number = :phoneNumber");
		assertQuery("select ph " + //
				"from Phone ph " + //
				"where ph.person.address = :address ");
		assertQuery("select ph " + //
				"from Phone ph " + //
				"join ph.person pr " + //
				"where pr.address = :address ");
		assertQuery("select ph " + //
				"from Phone ph " + //
				"where ph.person.address = :address " + //
				"  and ph.person.createdOn > :timestamp");
		assertQuery("select ph " + //
				"from Phone ph " + //
				"inner join ph.person pr " + //
				"where pr.address = :address " + //
				"  and pr.createdOn > :timestamp");
		assertQuery("select ph " + //
				"from Person pr " + //
				"join pr.phones ph " + //
				"join ph.calls c " + //
				"where pr.address = :address " + //
				"  and c.duration > :duration");
		assertQuery("select ch " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		assertQuery("select value(ch) " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		assertQuery("select key(ch) " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		assertQuery("select key(ch) " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		assertQuery("select entry (ch) " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		assertQuery("select sum(ch.duration) " + //
				"from Person pr " + //
				"join pr.phones ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id " + //
				"  and index(ph) = :phoneIndex");
		assertQuery("select value(ph.callHistory) " + //
				"from Phone ph " + //
				"where ph.id = :id ");
		assertQuery("select key(ph.callHistory) " + //
				"from Phone ph " + //
				"where ph.id = :id ");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.phones[0].type = LAND_LINE");
		assertQuery("select p " + //
				"from Person p " + //
				"where p.addresses['HOME'] = :address");
		assertQuery("select pr " + //
				"from Person pr " + //
				"where pr.phones[max(indices(pr.phones))].type = 'LAND_LINE'");
		assertQuery("select p.name, p.nickName " + //
				"from Person p ");
		assertQuery("select p.name as name, p.nickName as nickName " + //
				"from Person p ");
		assertQuery("select new org.hibernate.userguide.hql.CallStatistics" + //
				"(count(c), " + //
				"	sum(c.duration), " + //
				"	min(c.duration), " + //
				"	max(c.duration), " + //
				"	avg(c.duration)," + //
				"	1" + //
				")  " + //
				"from Call c ");
		assertQuery("select new map(" + //
				"p.number as phoneNumber, " + //
				"	sum(c.duration) as totalDuration, " + //
				"	avg(c.duration) as averageDuration" + //
				")  " + //
				"from Call c " + //
				"join c.phone p " + //
				"group by p.number ");
		assertQuery("select new list(" + //
				"p.number, " + //
				"	c.duration) " + //
				"from Call c " + //
				"join c.phone p ");
		assertQuery("select distinct p.lastName " + //
				"from Person p");
		assertQuery("select " + //
				"	count(c), " + //
				"	sum(c.duration), " + //
				"	min(c.duration), " + //
				"	max(c.duration), " + //
				"	avg(c.duration)  " + //
				"from Call c ");
		assertQuery("select count(distinct c.phone) " + //
				"from Call c ");
		assertQuery("select p.number, count(c) " + //
				"from Call c " + //
				"join c.phone p " + //
				"group by p.number");
		assertQuery("select p " + //
				"from Phone p " + //
				"where max(elements(p.calls)) = :call");
		assertQuery("select p " + //
				"from Phone p " + //
				"where min(elements(p.calls)) = :call");
		assertQuery("select p " + //
				"from Person p " + //
				"where max(indices(p.phones)) = 0");
		assertQuery("select count(c) filter (where c.duration < 30) " + //
				"from Call c ");
		assertQuery("select p.number, count(c) filter (where c.duration < 30) " + //
				"from Call c " + //
				"join c.phone p " + //
				"group by p.number");
		assertQuery("select listagg(p.number, ', ') within group (order by p.type, p.number) " + //
				"from Phone p " + //
				"group by p.person");
		assertQuery("select sum(c.duration) " + //
				"from Call c ");
		assertQuery("select p.name, sum(c.duration) " + //
				"from Call c " + //
				"join c.phone ph " + //
				"join ph.person p " + //
				"group by p.name");
		assertQuery("select p, sum(c.duration) " + //
				"from Call c " + //
				"join c.phone ph " + //
				"join ph.person p " + //
				"group by p");
		assertQuery("select p.name, sum(c.duration) " + //
				"from Call c " + //
				"join c.phone ph " + //
				"join ph.person p " + //
				"group by p.name " + //
				"having sum(c.duration) > 1000");
		assertQuery("select p.name from Person p " + //
				"union " + //
				"select p.nickName from Person p where p.nickName is not null");
		assertQuery("select p " + //
				"from Person p " + //
				"order by p.name");
		assertQuery("select p.name, sum(c.duration) as total " + //
				"from Call c " + //
				"join c.phone ph " + //
				"join ph.person p " + //
				"group by p.name " + //
				"order by total");
		assertQuery("select c " + //
				"from Call c " + //
				"join c.phone p " + //
				"order by p.number " + //
				"limit 50");
		assertQuery("select c " + //
				"from Call c " + //
				"join c.phone p " + //
				"order by p.number " + //
				"fetch first 50 rows only");
		assertQuery("select c " + //
				"from Call c " + //
				"join c.phone p " + //
				"order by p.number " + //
				"offset 10 rows " + //
				"fetch first 50 rows with ties");
		assertQuery("select p " + //
				"from Phone p " + //
				"join fetch p.calls " + //
				"order by p " + //
				"limit 50");
	}

	@Test // GH-2962
	void orderByWithNullsFirstOrLastShouldWork() {

		assertQuery("""
				select a,
					case
						when a.geaendertAm is null then a.erstelltAm
						else a.geaendertAm end as mutationAm
				from Element a
				where a.erstelltDurch = :variable
				order by mutationAm desc nulls first
				""");

		assertQuery("""
				select a,
					case
						when a.geaendertAm is null then a.erstelltAm
						else a.geaendertAm end as mutationAm
				from Element a
				where a.erstelltDurch = :variable
				order by mutationAm desc nulls last
				""");
	}

	@Test // GH-3882
	void shouldSupportLimitOffset() {

		assertQuery("SELECT si from StockItem si order by si.id LIMIT 10 OFFSET 10 FETCH FIRST 10 ROWS ONLY");
		assertQuery("SELECT si from StockItem si order by si.id LIMIT ? OFFSET ? FETCH FIRST ? ROWS ONLY");
		assertQuery("SELECT si from StockItem si order by si.id LIMIT :l OFFSET :o");
		assertQuery("SELECT si from StockItem si LIMIT :l OFFSET :o");
		assertQuery("SELECT si from StockItem si order by si.id LIMIT :l");
		assertQuery("SELECT si from StockItem si order by si.id OFFSET 1");
		assertQuery("SELECT si from StockItem si LIMIT 1");
		assertQuery("SELECT si from StockItem si OFFSET 1");
		assertQuery("SELECT si from StockItem si FETCH FIRST 1 ROWS ONLY");
	}

	@Test // GH-2964
	void roundFunctionShouldWorkLikeAnyOtherFunction() {

		assertQuery("""
				select round(count(ri) * 100 / max(ri.receipt.positions), 0) as perc
				from StockOrderItem oi
				right join StockReceiptItem ri
				on ri.article = oi.article
				""");
	}

	@Test // GH-2981
	void cteWithClauseShouldWork() {

		assertQuery("""
				WITH maxId AS (select max(sr.snapshot.id) snapshotId from SnapshotReference sr
					where sr.id.selectionId = ?1 and sr.enabled
					group by sr.userId)
				select sr from maxId m join SnapshotReference sr on sr.snapshot.id = m.snapshotId
				""");
	}

	@Test // GH-4012
	void cteWithSearch() {

		assertQuery("""
				WITH Tree AS (SELECT o.uuid AS test_uuid FROM DemoEntity o)
				SEARCH BREADTH FIRST BY foo ASC NULLS FIRST, bar DESC NULLS LAST SET baz
					SELECT test_uuid FROM Tree
				""");
	}

	@Test // GH-4012
	void cteWithCycle() {

		assertQuery("""
				WITH Tree AS (SELECT o.uuid AS test_uuid FROM DemoEntity o) CYCLE test_uuid SET circular TO true DEFAULT false
					SELECT test_uuid FROM Tree
				""");

		assertQuery(
				"""
						WITH Tree AS (SELECT o.uuid AS test_uuid FROM DemoEntity o) CYCLE test_uuid SET circular TO true DEFAULT false USING bar
							SELECT test_uuid FROM Tree
						""");
	}

	@Test // GH-3024
	void castFunctionWithFqdnShouldWork() {
		assertQuery("SELECT o FROM Order o WHERE CAST(:userId AS java.util.UUID) IS NULL OR o.user.id = :userId");
	}

	@ParameterizedTest // GH-3025
	@ValueSource(strings = { "YEAR", "MONTH", "DAY", "WEEK", "QUARTER", "HOUR", "MINUTE", "SECOND", "NANOSECOND",
			"NANOSECOND", "EPOCH" })
	void durationLiteralsShouldWork(String dtField) {

		assertQuery("SELECT ce.id FROM CalendarEvent ce WHERE (ce.endDate - ce.startDate) > 5 %s".formatted(dtField));
		assertQuery(
				"SELECT ce.id FROM CalendarEvent ce WHERE ce.text LIKE :text GROUP BY year(cd.date) HAVING (ce.endDate - ce.startDate) > 5 %s"
						.formatted(dtField));
		assertQuery("SELECT ce.id as id, cd.startDate + 5 %s AS summedDate FROM CalendarEvent ce".formatted(dtField));
	}

	@Test // GH-3739
	void dateTimeLiterals() {

		assertQuery("SELECT e FROM  Employee e WHERE e.startDate = {d'2012-01-03'}");
		assertQuery("SELECT e FROM  Employee e WHERE e.startTime = {t'09:00:00'}");
		assertQuery("SELECT e FROM  Employee e WHERE e.version = {ts'2012-01-03 09:00:00'}");
		assertQuery("SELECT e FROM  Employee e WHERE e.version = {ts'something weird'}");
		assertQuery("SELECT e FROM  Employee e WHERE e.version = {ts2012-01-03 09:00:00+1}");
		assertQuery("SELECT e FROM  Employee e WHERE e.version = {ts2012-01-03 09:00:00-1}");
		assertQuery("SELECT e FROM  Employee e WHERE e.version = {ts2012-01-03 09:00:00+1:00}");
		assertQuery("SELECT e FROM  Employee e WHERE e.version = {ts2012-01-03 09:00:00-1:00}");

		assertQuery("SELECT e FROM  Employee e WHERE e.version = OFFSET DATETIME 2012-01-03 09:00:00+1:01");
		assertQuery("SELECT e FROM  Employee e WHERE e.version = OFFSET DATETIME 2012-01-03 09:00:00-1:01");
	}

	@ParameterizedTest // GH-3711
	@ValueSource(strings = { "1", "1_000", "1L", "1_000L", "1bi", "1.1f", "2.2d", "2.2bd" })
	void numberLiteralsShouldWork(String literal) {
		assertQuery(String.format("SELECT %s FROM User u where u.id = %s", literal, literal));
	}

	@Test // GH-3025
	void binaryLiteralsShouldWork() {

		assertQuery("SELECT ce.id FROM CalendarEvent ce WHERE ce.value = {0xDE, 0xAD, 0xBE, 0xEF}");
		assertQuery("SELECT ce.id FROM CalendarEvent ce WHERE ce.value = X'DEADBEEF'");
		assertQuery("SELECT ce.id FROM CalendarEvent ce WHERE ce.value = x'deadbeef'");
	}

	@Test // GH-3099
	void functionNamesShouldSupportSchemaScoping() {

		assertQuery("""
				SELECT b
				FROM MyEntity b
				WHERE b.status = :status
				AND utl_raw.cast_to_varchar2((nlssort(lower(b.name), 'nls_sort=binary_ai'))) LIKE lower(:name)
				ORDER BY utl_raw.cast_to_varchar2((nlssort(lower(b.name), 'nls_sort=binary_ai'))) ASC
				""");

		assertQuery("""
				select b
				from Bairro b
				where b.situacao = :situacao
				and utl_raw.cast_to_varchar2((nlssort(lower(b.nome), 'nls_sort=binary_ai'))) like lower(:nome)
				order by utl_raw.cast_to_varchar2((nlssort(lower(b.nome), 'nls_sort=binary_ai'))) ASC
				""");

		assertQuery("""
				select b
				from Bairro b
				where b.situacao = :situacao
				and CTM_UTLRAW_NLSSORT_LOWER(b.nome) like lower(:nome)
				order by CTM_UTLRAW_NLSSORT_LOWER(b.nome) ASC
				""");
	}

	@Test // GH-3219
	void extractFunctionShouldSupportAdditionalExtensions() {

		assertQuery("""
				select extract(day of week from departureTime) AS day, sum(duration) as duration from JourneyEntity
				group by extract(day of week from departureTime)
				""");
		assertQuery("""
				select extract(day of month from departureTime) AS day, sum(duration) as duration from JourneyEntity
				group by extract(day of month from departureTime)
				""");
		assertQuery("""
				select extract(week of year from departureTime) AS day, sum(duration) as duration from JourneyEntity
				group by extract(week of year from departureTime)
				""");

		assertQuery("""
				select extract(date from departureTime) AS date
				group by extract(date from departureTime)
				""");
		assertQuery("""
				select extract(time from departureTime) AS time
				group by extract(time from departureTime)
				""");
		assertQuery("""
				select extract(epoch from departureTime) AS epoch
				group by extract(epoch from departureTime)
				""");
	}

	@Test // GH-3757
	void arithmeticDate() {

		assertQuery("SELECT a FROM foo a WHERE (cast(a.createdAt as date) - CURRENT_DATE()) BY day - 2 = 0");
		assertQuery("SELECT a FROM foo a WHERE (cast(a.createdAt as date) - CURRENT_DATE()) BY day - 2 = 0");
		assertQuery("SELECT a FROM foo a WHERE (cast(a.createdAt as date)) BY day - 2 = 0");

		assertQuery("SELECT f.start BY DAY - 2 FROM foo f");
		assertQuery("SELECT f.start - 1 minute FROM foo f");

		assertQuery("SELECT f FROM foo f WHERE (cast(f.start as date) - CURRENT_DATE()) BY day - 2 = 0");
		assertQuery("SELECT 1 week - 1 day FROM foo f");
		assertQuery("SELECT f.birthday - local date day FROM foo f");
		assertQuery("SELECT local datetime - f.birthday FROM foo f");
		assertQuery("SELECT (1 year) by day FROM foo f");
	}

	@ParameterizedTest // GH-3342
	@ValueSource(
			strings = { "select 1 from User", "select -1 from User", "select +1 from User", "select +1 * -100 from User",
					"select count(u) * -0.7f from User u", "select count(oi) + (-100) as perc from StockOrderItem oi",
					"select p from Payment p where length(p.cardNumber) between +16 and -20" })
	void signedLiteralShouldWork(String query) {
		assertQuery(query);
	}

	@Test
	void reservedWordsShouldWork() {

		assertQuery("select ie from ItemExample ie left join ie.object io where io.externalId = :externalId");
		assertQuery("select ie.object from ItemExample ie left join ie.object io where io.externalId = :externalId");
		assertQuery("select ie from ItemExample ie left join ie.object io where io.object = :externalId");
		assertQuery("select ie from ItemExample ie where ie.status = com.app.domain.object.Status.UP");
	}

	@Test // GH-3864
	void fromSRFWithAlias() {

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date, :integerValue) d
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date) d
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from some_function() d
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date, :integerValue, :longValue) d
				""");
	}

	@Test // GH-3864
	void fromSRFWithoutAlias() {

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date, :integerValue)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from some_function()
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date, :integerValue, :longValue)
				""");
	}

	@Test // GH-3864
	void joinEntityToSRFWithFunctionAlias() {

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from EntityClass e join some_function(:date, :integerValue) d on (e.id = d.idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from EntityClass e join some_function(:date) d on (e.id = d.idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from EntityClass e join some_function() d on (e.id = d.idFunction)
				""");

		assertQuery("""
				select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from EntityClass e join some_function(:date, :integerValue, :longValue) d on (e.id = d.idFunction)
				""");
	}

	@Test // GH-3864
	void joinEntityToSRFWithoutFunctionAlias() {

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from EntityClass e join some_function(:date, :integerValue) on (e.id = idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from EntityClass e join some_function(:date) on (e.id = idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from EntityClass e join some_function() on (e.id = idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from EntityClass e join some_function(:date, :integerValue, :longValue) on (e.id = idFunction)
				""");
	}

	@Test // GH-3864
	void joinSRFToEntityWithoutFunctionWithAlias() {

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date, :integerValue) d join EntityClass e on (e.id = d.idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date) d join EntityClass e on (e.id = idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from some_function() d join EntityClass e on (e.id = idFunction)
				""");

		assertQuery("""
				select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date, :integerValue, :longValue) d join EntityClass e on (e.id = d.idFunction)
				""");
	}

	@Test // GH-3864
	void joinSRFToEntityWithoutFunctionWithoutAlias() {

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date, :integerValue) join EntityClass e on (e.id = idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date) join EntityClass e on (e.id = idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from some_function() join EntityClass e on (e.id = idFunction)
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date, :integerValue, :longValue) join EntityClass e on (e.id = idFunction)
				""");
	}

	@Test // GH-3864
	void selectSRFIntoSubquery() {

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function(:date, :integerValue) x) d
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function(:date) x) d
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from (select x.idFunction idFunction, x.nameFunction nameFunction
					from some_function() x) d
				""");

		assertQuery("""
					select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
					from (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function(:date, :integerValue, :longValue) x) d
				""");
	}

	@Test // GH-3864
	void joinEntityToSRFIntoSubquery() {

		assertQuery("""
				select new com.example.dto.SampleDto(k.id, d.nameFunction)
				from EntityClass k
				inner join (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function(:date, :integerValue) x) d on (k.id = d.idFunction)
				""");

		assertQuery("""
				select new com.example.dto.SampleDto(k.id, d.nameFunction)
				from EntityClass k
				inner join (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function(:date) x) d on (k.id = d.idFunction)
				""");

		assertQuery("""
				select new com.example.dto.SampleDto(k.id, d.nameFunction)
				from EntityClass k
				inner join (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function() x) d on (k.id = d.idFunction)
				""");

		assertQuery("""
				select new com.example.dto.SampleDto(k.id, d.nameFunction)
				from EntityClass k
				inner join (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function(:date, :integerValue, :longValue) x) d on (k.id = d.idFunction)
				""");
	}

	@Test // GH-3864
	void joinLateralEntityToSRF() {

		assertQuery("""
				select new com.example.dto.SampleDto(k.id, d.nameFunction)
				from EntityClass k
				join lateral (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function(:date, :integerValue) x where x.idFunction = k.id) d
				""");

		assertQuery("""
				select new com.example.dto.SampleDto(k.id, d.nameFunction)
				from EntityClass k
				join lateral (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function(:date) x where x.idFunction = k.id) d
				""");

		assertQuery("""
				select new com.example.dto.SampleDto(k.id, d.nameFunction)
				from EntityClass k
				join lateral (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function() x where x.idFunction = k.id) d
				""");

		assertQuery("""
				select new com.example.dto.SampleDto(k.id, d.nameFunction)
				from EntityClass k
				join lateral (select x.idFunction idFunction, x.nameFunction nameFunction
				from some_function(:date, :integerValue, :longValue) x where x.idFunction = k.id) d
				""");

	}

	@Test // GH-3864
	void joinTwoFunctions() {

		assertQuery("""
				select new com.example.dto.SampleDto(d.idFunction, d.nameFunction)
				from some_function(:date, :integerValue) d
				inner join some_function_single_param(:date) k on (d.idFunction = k.idFunctionSP)
				""");
	}

	@Test // GH-3902
	void queryWithoutSelectShouldWork() {

		assertQuery("from Person p");
		assertQuery("from Person p WHERE p.name = 'John' ORDER BY p.name");
	}

	@Test // GH-3902
	void queryWithoutSelectAndIdentificationVariableShouldWork() {

		assertQuery("from Person");
		assertQuery("from Person WHERE name = 'John' ORDER BY name");
		assertQuery("from Person JOIN department WHERE name = 'John' ORDER BY name");
		assertQuery(
				"from Person JOIN (select phone.number as n, phone.person as pp from Phone phone) WHERE name = 'John' ORDER BY name");
		assertQuery("from Person JOIN (select number, person from Phone) WHERE name = 'John' ORDER BY name");
	}

	@Test // GH-3902
	void queryWithoutIdentificationVariableShouldWork() {

		assertQuery("SELECT name, lastname from Person");
		assertQuery("SELECT name, lastname from Person WHERE lastname = 'Doe' ORDER BY name, lastname");
		assertQuery("SELECT name, lastname from Person JOIN department");
		assertQuery(
				"SELECT name, lastname from Person JOIN (select phone.number as n, phone.person as pp from Phone phone) WHERE name = 'John' ORDER BY name");
		assertQuery(
				"SELECT name, lastname from Person JOIN (select number, person from Phone) WHERE name = 'John' ORDER BY name");
	}

	@Test // GH-3883
	void jsonArray() {

		assertQuery("select json_array(1, false, 'val1', 'val2' null on null)");
		assertQuery("select json_array(1, false, 'val1', 'val2' absent on null)");
	}

	@Test // GH-3883
	void jsonExists() {

		assertQuery("select json_exists(1, e.foo)");
		assertQuery("select json_exists(e.json, '$.theArray[$idx]' passing 1 as idx ERROR ON ERROR) from Entity e");

		assertQuery("select json_exists(e.json, '$.theArray[$idx]' passing 1 as idx TRUE ON ERROR) from Entity e");

		assertQuery("select json_exists(1, e.foo FALSE ON ERROR)");
	}

	@Test // GH-3883
	void jsonObject() {

		assertQuery("select json_object('key', 'value')");
		assertQuery("select json_object('key' VALUE 'value')");
		assertQuery("select json_object(KEY 'key' VALUE 'value')");
		assertQuery(
				"select json_object('key1', 'value1', KEY 'key2' VALUE 'value2', 'key3' : 'value3', 'key4', 'value4', KEY 'key5' VALUE 'value5', 'key6' : 'value6')");
		assertQuery("select json_object('key', 'value' absent on null)");
		assertQuery("select json_object('key', 'value' null on null)");
	}

	@Test // GH-3883
	void jsonQuery() {

		assertQuery("select json_query(e.json, '$.theString') from Entity e");
		assertQuery("select json_query(e.json, '$.theString' with wrapper) from Entity e");
		assertQuery("select json_query(e.json, '$.theString' without wrapper) from Entity e");
		assertQuery("select json_query(e.json, '$.theString' without array wrapper) from Entity e");
		assertQuery("select json_query(e.json, '$.theString' with conditional array wrapper) from Entity e");
		assertQuery("select json_query(e.json, '$.theArray[$idx]' passing 1 as idx) from Entity e");

		assertQuery(
				"select json_query(e.json, '$.theString' without array wrapper ERROR ON ERROR EMPTY ARRAY ON EMPTY) from Entity e");

		assertQuery(
				"select json_query(e.json, '$.theString' without array wrapper EMPTY OBJECT ON ERROR NULL ON EMPTY) from Entity e");
	}

	@Test // GH-3883
	void jsonValue() {

		assertQuery("select json_value(e.json, '$.theString') from Entity e");
		assertQuery("select json_value(e.json, '$.theArray[$idx]' passing 1 as idx) from Entity e");
		assertQuery(
				"select json_value(e.json, '$.theArray[$idx]' passing 1 as idx RETURNING NUMBER(12, 2) NULL ON ERROR eRRor ON error) from Entity e");
		assertQuery(
				"select json_value(e.json, '$.theArray[$idx]' passing 1 as idx DEFAULT 7 ON ERROR NULL ON EMPTY) from Entity e");
	}

	@Test // GH-3883
	void jsonArrayagg() {

		assertQuery("select json_arrayagg(e.theString null on null) from Entity e");
		assertQuery(
				"select json_arrayagg(e.theString absent on null order by e.id) FILTER (where foo = bar) from Entity e");
	}

	@Test // GH-3883
	void jsonObjectagg() {

		assertQuery("select json_objectagg(e.theString : e.id) from Entity e");
		assertQuery("select json_objectagg(KEY e.theString VALUE e.id) from Entity e");
		assertQuery("select json_objectagg(e.theString VALUE e.id) from Entity e");
		assertQuery(
				"select json_objectagg(foo : bar ABSENT ON NULL WITH UNIQUE KEYS) FILTER (where foo = bar) from Entity e");
	}

	@Test // GH-3883
	void jsonTable() {

		assertQuery("""
				SELECT e FROM from json_table(e.json, '$'
				columns(theInt Integer,
				theFloat Float,
				nonExisting exists) ERROR ON ERROR)
				""");

		assertQuery("""
				SELECT e FROM from EntityWithJson e
						join lateral json_table(e.json, '$' columns(theInt Integer,
						theFloat Float,
						theString String,
						theBoolean Boolean,
						theNull String,
						theObject JSON,
						theObject JSON WITH UNCONDITIONAL ARRAY WRAPPER ERROR ON EMPTY EMPTY ON ERROR,
						theObject JSON ERROR ON EMPTY EMPTY ON ERROR,
						theNestedInt Integer path '$.theObject.theInt',
						theNestedFloat Float path '$.theObject.theFloat',
						theNestedString String path '$.theObject.theString',
						nested '$.theArray[*]' columns(arrayIndex for ordinality,
							arrayValue String path '$'),
						nonExisting exists) ERROR ON ERROR)
				""");
	}

	@Test // GH-3883
	void xmlElement() {

		assertQuery("select xmlelement(name myelement)");
		assertQuery(
				"select xmlelement(name `my-element`, xmlattributes(123 as attr1, '456' as `attr-2`), 'myContent', xmlelement(name empty))");
	}

	@Test // GH-3883
	void xmlForest() {

		assertQuery("select xmlforest(123 as e1)");
		assertQuery("select xmlforest(123 as e1, 'text' as e2)");
	}

	@Test // GH-3883
	void xmlPi() {

		assertQuery("select xmlpi(name php)");
		assertQuery("select xmlpi(name php, foo)");
	}

	@Test // GH-3883
	void xmlQuery() {

		assertQuery("select xmlquery('/a/val' passing '<a><val>asd</val></a>')");
		assertQuery("select xmlquery('/a/val' passing e.xml) from Entity e");
	}

	@Test // GH-3883
	void xmlExists() {

		assertQuery("select xmlexists('/a/val' passing '<a><val>asd</val></a>')");
		assertQuery("select xmlexists('/a/val' passing e.xml) from Entity e");
	}

	@Test // GH-3883
	void xmlAgg() {

		assertQuery("select xmlagg(xmlelement(name a, e.theString))");
		assertQuery(
				"select xmlagg(xmlelement(name a, e.theString) order by e.id) FILTER (WHERE  foo = bar) OVER (PARTITION BY expression) from Entity e");
	}

	@Test // GH-3883
	void xmlTable() {

		assertQuery("""
				select
						t.nonExistingWithDefault
						from xmltable('/root/elem' passing :xml columns theInt Integer,
						theFloat Float,
						theString String,
						theBoolean Boolean,
						theNull String,
						theObject XML,
						theNestedString String path 'theObject/nested',
						nonExisting String,
						nonExistingWithDefault String default 'none') t
				""");
	}

}
