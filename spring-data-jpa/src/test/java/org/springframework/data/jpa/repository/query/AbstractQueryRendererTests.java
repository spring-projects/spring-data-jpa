/*
 * Copyright 2026-present the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Base class for query rendering tests. Every query is parsed and rendered again, the result must equal the input. The
 * cases follow the <a href=
 * "https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc">Jakarta
 * Persistence query language chapter</a> and must pass for every dialect. Subclasses provide the parser and renderer
 * and add dialect-specific cases.
 *
 * @author Mark Paluch
 * @author Jewoo Shin
 * @author Wantaek Choi
 */
abstract class AbstractQueryRendererTests {

	/**
	 * Parse the query and render it again without applying any transformation.
	 */
	abstract String parseWithoutChanges(String query);

	/**
	 * Assert that {@code query} survives a parse and render round trip unchanged, ignoring insignificant whitespace.
	 */
	void assertQuery(String query) {

		String slimmedDownQuery = reduceWhitespace(query);
		assertThat(parseWithoutChanges(slimmedDownQuery)).isEqualTo(slimmedDownQuery);
	}

	/**
	 * Assert that {@code query} is rejected by the parser.
	 */
	void assertBadGrammar(String query) {
		assertThatExceptionOfType(BadJpqlGrammarException.class).isThrownBy(() -> parseWithoutChanges(query));
	}

	private static String reduceWhitespace(String original) {
		return original.replaceAll("[ \\t\\n]{1,}", " ").trim();
	}

	@Nested
	class SelectClause {

		@Test
		void selectClause() {

			assertQuery("SELECT o FROM Order o");
			assertQuery("Select e FROM Employee e WHERE e.salary > 100000");
			assertQuery("Select e FROM Employee e WHERE e.id = :id");
			assertQuery("Select e.firstName FROM Employee e");
			assertQuery("Select e.firstName, e.lastName FROM Employee e");
			assertQuery("Select MAX(e.salary) FROM Employee e");
		}

		@Test
		void distinct() {

			assertQuery("SELECT DISTINCT o.shippingAddress.state FROM Order o");
			assertQuery("SELECT DISTINCT o FROM Order o JOIN o.lineItems l");
		}

		@Test
		void aliasedSelection() {
			assertQuery("SELECT e.address AS addr FROM Employee e");
		}

		@Test
		void constructorExpression() {

			assertQuery("SELECT NEW com.acme.reports.EmpReport(e.firstName, e.lastName, e.salary) FROM Employee e");
			assertQuery("""
					SELECT NEW com.acme.example.CustomerDetails(c.id, c.status, o.count)
					FROM Customer c JOIN c.orders o
					WHERE o.count > 100
					""");
		}

		@Test // GH-3902
		void queryWithoutSelectClause() {

			assertQuery("from Person p");
			assertQuery("from Person p WHERE p.name = 'John' ORDER BY p.name");
			assertQuery("from Person");
			assertQuery("from Person WHERE name = 'John' ORDER BY name");
			assertQuery("from Person JOIN department WHERE name = 'John' ORDER BY name");
		}

		@Test // GH-3902
		void queryWithoutIdentificationVariable() {

			assertQuery("SELECT name, lastname from Person");
			assertQuery("SELECT name, lastname from Person WHERE lastname = 'Doe' ORDER BY name, lastname");
			assertQuery("SELECT name, lastname from Person JOIN department");
		}

	}

	@Nested
	class FromClause {

		@Test
		void rangeVariables() {

			assertQuery("SELECT e FROM Employee e");
			assertQuery("SELECT e FROM com.acme.Employee e");
			assertQuery("SELECT o.lineItems FROM Order AS o");
			assertQuery("SELECT e, a FROM Employee e, MailingAddress a WHERE e.address = a.address");
			assertQuery("SELECT c FROM Customer c, Employee e WHERE c.hatsize = e.shoesize");
			assertQuery("""
					SELECT DISTINCT o1
					FROM Order o1, Order o2
					WHERE o1.quantity > o2.quantity AND
					 o2.customer.lastname = 'Smith' AND
					 o2.customer.firstname = 'John'
					""");
		}

		@Test // GH-3902
		void collectionMemberDeclaration() {

			assertQuery("SELECT e FROM Employee e, IN(e.projects) AS p");
			assertQuery("SELECT e FROM Employee e, IN(e.projects) p");
			assertQuery("SELECT e FROM Employee e, IN(e.projects)");
			assertQuery("FROM Employee e, IN(e.projects)");
			assertQuery("SELECT OBJECT(c) FROM Customer c, IN(c.orders) o WHERE c.status = 1");
			assertQuery("""
					SELECT DISTINCT o
					FROM Order o, IN(o.lineItems) l
					WHERE l.product.productType = 'office_supplies'
					""");
		}

		@Test // GH-4272
		void collectionMemberDeclarationInSubquery() {

			assertQuery(
					"SELECT o FROM Order o WHERE EXISTS (SELECT l FROM Order o2, IN(o2.lineItems) l WHERE l.quantity > 5)");
			assertQuery(
					"SELECT o FROM Order o WHERE EXISTS (SELECT l FROM Order o2, IN(o2.lineItems) l, Order o3, IN(o3.lineItems) s WHERE l.quantity > 5)");
		}

		@Test // GH-3902
		void subqueryInFromClause() {

			assertQuery("SELECT e FROM Employee e, (SELECT p FROM Project p) AS sub");
			assertQuery("SELECT e FROM Employee e, (SELECT p FROM Project p) sub");
			assertQuery("SELECT e FROM Employee e, (SELECT p FROM Project p)");
			assertQuery("FROM Employee e, (SELECT p FROM Project p) sub");
		}

	}

	@Nested
	class Joins {

		@Test
		void innerJoin() {

			assertQuery("SELECT e FROM Employee e JOIN e.address a WHERE a.city = :city");
			assertQuery("SELECT e FROM Employee e JOIN e.projects p JOIN e.projects p2 WHERE p.name = :p1 AND p2.name = :p2");
			assertQuery("SELECT c FROM Customer c JOIN c.orders o WHERE c.status = 1");
			assertQuery("SELECT c FROM Customer c INNER JOIN c.orders o WHERE c.status = 1");
			assertQuery("SELECT o FROM Order AS o JOIN o.lineItems l JOIN l.product p");
			assertQuery("SELECT DISTINCT o FROM Order AS o JOIN o.lineItems AS l WHERE l.shipped = FALSE");
			assertQuery(
					"SELECT DISTINCT o FROM Order o JOIN o.lineItems l JOIN l.product p WHERE p.productType = 'office_supplies'");
			assertQuery("SELECT DISTINCT o FROM Order o JOIN o.lineItems l WHERE l.product.productType = 'office_supplies'");
			assertQuery("""
					SELECT p.vendor
					FROM Employee e JOIN e.contactInfo c JOIN c.phones p
					WHERE c.address.zipcode = '95054'
					""");
		}

		@Test
		void leftJoin() {

			assertQuery("SELECT e FROM Employee e LEFT JOIN e.address a ORDER BY a.city");
			assertQuery("""
					SELECT s.name, COUNT(p)
					FROM Suppliers s LEFT JOIN s.products p
					GROUP BY s.name
					""");
			assertQuery("""
					SELECT s.name, COUNT(p)
					FROM Suppliers s LEFT JOIN s.products p
					    ON p.status = 'inStock'
					GROUP BY s.name
					""");
			assertQuery("""
					SELECT s.name, COUNT(p)
					FROM Suppliers s LEFT JOIN s.products p
					WHERE p.status = 'inStock'
					GROUP BY s.name
					""");
		}

		@Test
		void joinFetch() {

			assertQuery("SELECT e FROM Employee e JOIN FETCH e.address");
			assertQuery("SELECT e FROM Employee e JOIN FETCH e.address a ORDER BY a.city");
			assertQuery("SELECT e FROM Employee e JOIN FETCH e.address AS a ORDER BY a.city");
			assertQuery("SELECT e FROM Employee e JOIN FETCH e.address ORDER BY city");
			assertQuery("SELECT d FROM Department d LEFT JOIN FETCH d.employees WHERE d.deptno = 1");
		}

		@ParameterizedTest // GH-4326
		@ValueSource(strings = { "e.manager", "e.projects", "e.contactInfo.address", "e.contactInfo.phones", "e.order",
				"TREAT(e.manager AS Manager)", "TREAT(e.projects AS LargeProject)" })
		void associationJoinPaths(String path) {

			assertQuery("SELECT e FROM Employee e JOIN " + path + " a");
			assertQuery("SELECT e FROM Employee e LEFT JOIN FETCH " + path + " a");
			assertQuery("SELECT e FROM Employee e JOIN " + path + " a ON a.id = :id");
		}

	}

	@Nested
	class Downcasting {

		@Test
		void treatInFromClause() {

			assertQuery("SELECT b.name, b.ISBN FROM Order o JOIN TREAT(o.product AS Book) b");
			assertQuery("SELECT e FROM Employee e JOIN TREAT(e.projects AS LargeProject) lp WHERE lp.budget > 1000");
			assertQuery("SELECT e FROM Employee e JOIN TREAT(e.projects AS LargeProject) p WHERE p.budget > 1000000");
		}

		@Test
		void treatInWhereClause() {

			assertQuery("""
					SELECT e FROM Employee e JOIN e.projects p
					WHERE TREAT(p AS LargeProject).budget > 1000
					    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
					    OR p.description LIKE 'cost overrun'
					""");
			assertQuery("""
					SELECT e FROM Employee e
					WHERE TREAT(e AS Exempt).vacationDays > 10
					    OR TREAT(e AS Contractor).hours > 100
					""");
		}

		@Test // GH-3711, GH-4272
		void treatInPathExpression() {

			assertQuery("SELECT TREAT(e as Integer).foo FROM Employee e");
			assertQuery("SELECT TREAT(VALUE(m) AS Employee) FROM Department d JOIN d.employees m");
			assertQuery("SELECT d FROM Department d JOIN d.employees m WHERE TREAT(VALUE(m) AS Employee) IS NOT NULL");
		}

	}

	@Nested
	class PathExpressions {

		@Test
		void stateFieldPaths() {

			assertQuery("SELECT DISTINCT l.product FROM Order AS o JOIN o.lineItems l");
			assertQuery("SELECT p.vendor FROM Employee e JOIN e.contactInfo.phones p");
			assertQuery("""
					SELECT p.vendor
					FROM Employee e JOIN e.contactInfo c JOIN c.phones p
					WHERE e.contactInfo.address.zipcode = '95054'
					""");
		}

		@Test
		void mapKeyAndValue() {

			assertQuery("SELECT i.name, VALUE(p) FROM Item i JOIN i.photos p WHERE KEY(p) LIKE '%egret'");
			assertQuery("SELECT i.name, p FROM Item i JOIN i.photos p WHERE KEY(p) LIKE '%egret'");
			assertQuery("SELECT p FROM Employee e JOIN e.priorities p WHERE KEY(p) = 'high'");
			assertQuery("""
					SELECT v.location.street, KEY(i).title, VALUE(i)
					FROM VideoStore v JOIN v.videoInventory i
					WHERE v.location.zipcode = '94301' AND VALUE(i) > 0
					""");
		}

	}

	@Nested
	class ReservedWordsAsIdentifiers {

		@Test // GH-2982
		void reservedWordAsEntityName() {

			assertQuery("SELECT f FROM Floor f WHERE f.name = :name");
			assertQuery("SELECT r FROM Room r JOIN r.floor f WHERE f.name = :name");
		}

		@Test // GH-4336
		void idAndVersionAsIdentifiers() {

			assertQuery("select e.id, e.version from Employee e where e.id = :id");
			assertQuery("select v from Version v");
			assertQuery("select new com.company.id.thing.ClassName(e.a) from Experience e");
		}

		@Test // GH-2994, GH-3028, GH-3056, GH-3062, GH-3092, GH-3128, GH-3143, GH-3496, GH-3834, GH-4335
		void reservedWordAsStateField() {

			assertQuery("select sum(i.size.foo.bar.new) from Item i");
			assertQuery("select t.sign from TestEntity t");
			assertQuery("select t.value from TestEntity t");
			assertQuery("select e.power.id from MyEntity e");
			assertQuery("select m.cast from Movie m where m.cast is not null");
			assertQuery("select e from Employee e where e.type = :_type");
			assertQuery("select te from TestEntity te where te.type = :type");
			assertQuery("select e from Employee e where e.lateral = :_lateral");
			assertQuery("select te from TestEntity te where te.lateral = :lateral");
			assertQuery("SELECT e FROM Entity e WHERE e.embeddedId.date BETWEEN :from AND :to");
			assertQuery("select ie from ItemExample ie left join ie.object io where io.externalId = :externalId");
			assertQuery("select ie.object from ItemExample ie left join ie.object io where io.externalId = :externalId");
			assertQuery("select ie from ItemExample ie left join ie.object io where io.object = :externalId");
			assertQuery("select ie from ItemExample ie where ie.status = com.app.domain.object.Status.UP");
			assertQuery("select f from FooEntity f where upper(f.name) IN :names");
			assertQuery("select f from FooEntity f where f.size IN :sizes");
			assertQuery("""
					SELECT j
					FROM AgentUpdateTask j
					WHERE j.creationTimestamp < :date
					AND (j.status = com.ca.apm.acc.configserver.core.domain.jobs.AgentUpdateTaskStatus.NEW
						OR
						j.status = com.ca.apm.acc.configserver.core.domain.jobs.AgentUpdateTaskStatus.STARTED
						OR
						j.status = com.ca.apm.acc.configserver.core.domain.jobs.AgentUpdateTaskStatus.QUEUED)
					ORDER BY j.id
					""");
		}

		@Test // GH-3092
		void reservedWordAsParameterName() {

			assertQuery("""
					UPDATE Lock L
					SET L.isLocked = TRUE, L.forceUnlockTime = :forceUnlockTime
					WHERE L.isLocked = FALSE OR L.forceUnlockTime < :time
					""");
		}

		@ParameterizedTest // GH-3451
		@ValueSource(strings = { "abs", "exp", "any", "case", "else", "index", "time" })
		void reservedWordAsPackageSegment(String reservedWord) {
			assertQuery("select new com.company.%s.thing.stuff.ClassName(e.id) from Experience e".formatted(reservedWord));
		}

	}

	@Nested
	class Literals {

		@Test
		void stringLiterals() {

			assertQuery("SELECT e FROM Employee e WHERE e.name = 'Bob'");
			assertQuery("select new com.example.demo.SampleObject(se.id, se.sampleValue, \"java\") from SampleEntity se"); // GH-3308
		}

		@Test // GH-3277
		void numericLiterals() {

			assertQuery("SELECT e FROM Employee e WHERE e.id = 1234");
			assertQuery("SELECT e FROM Employee e WHERE e.id = 1234L");
			assertQuery("SELECT s FROM Stat s WHERE s.ratio > 3.14");
			assertQuery("SELECT s FROM Stat s WHERE s.ratio > 3.14F");
			assertQuery("SELECT s FROM Stat s WHERE s.ratio > 3.14e32D");
		}

		@ParameterizedTest // GH-3342
		@ValueSource(strings = { "select 1 as value from User u", "select -1 as value from User u",
				"select +1 as value from User u", "select +1 * -100 as value from User u",
				"select count(u) * -0.7f as value from User u", "select count(oi) + (-100) as perc from StockOrderItem oi",
				"select p from Payment p where length(p.cardNumber) between +16 and -20" })
		void signedLiterals(String query) {
			assertQuery(query);
		}

		@ParameterizedTest // GH-3342
		@ValueSource(strings = { "select -count(u) from User u", "select +1 * (-count(u)) from User u" })
		void signedExpressions(String query) {
			assertQuery(query);
		}

		@Test
		void booleanLiterals() {

			assertQuery("SELECT e FROM Employee e WHERE e.active = TRUE");
			assertQuery("SELECT DISTINCT o FROM Order o JOIN o.lineItems l WHERE l.shipped = FALSE");
		}

		@Test
		void nullLiteral() {
			assertQuery("UPDATE Employee e SET e.manager = NULL WHERE e.manager = :manager");
		}

		@Test
		void enumLiterals() {
			assertQuery("SELECT e FROM Employee e WHERE e.gender = org.acme.Gender.MALE");
		}

		@Test
		void temporalLiterals() {

			assertQuery("SELECT e FROM Employee e WHERE e.startDate = {d'2012-01-03'}");
			assertQuery("SELECT e FROM Employee e WHERE e.startTime = {t'09:00:00'}");
			assertQuery("SELECT e FROM Employee e WHERE e.version = {ts'2012-01-03 09:00:00.000000001'}");
		}

		@Test
		void currentTimeLiterals() {

			assertQuery("SELECT e FROM Employee e WHERE CURRENT_DATE > CURRENT_TIME");
			assertQuery("SELECT e FROM Employee e WHERE CURRENT_TIME > CURRENT_TIMESTAMP");
			assertQuery("SELECT e.name, CURRENT_DATE FROM Employee e");
			assertQuery("SELECT e.name, CURRENT_TIME FROM Employee e");
			assertQuery("SELECT e.name, CURRENT_TIMESTAMP FROM Employee e");
			assertQuery("SELECT e FROM Entity e WHERE e.embeddedId.date BETWEEN CURRENT_DATE AND CURRENT_TIME");
		}

	}

	@Nested
	class Functions {

		@Test
		void arithmeticOperators() {

			assertQuery("SELECT e.salary - 1000 FROM Employee e");
			assertQuery("SELECT e.salary + 1000 FROM Employee e");
			assertQuery("SELECT e.salary * 2 FROM Employee e");
			assertQuery("SELECT e.salary * 2.0 FROM Employee e");
			assertQuery("SELECT e.salary / 2 FROM Employee e");
			assertQuery("SELECT e.salary / 2.0 FROM Employee e");
			assertQuery("SELECT e FROM Employee e WHERE e.salary - 1000 > 0");
			assertQuery("SELECT e FROM Employee e WHERE e.salary + 1000 > 0");
			assertQuery("SELECT e FROM Employee e WHERE e.salary * 2 > 0");
			assertQuery("SELECT e FROM Employee e WHERE e.salary * 2.0 > 0.0");
			assertQuery("SELECT e FROM Employee e WHERE e.salary / 2 > 0");
			assertQuery("SELECT e FROM Employee e WHERE e.salary / 2.0 > 0.0");
		}

		@Test // GH-3711
		void numericFunctions() {

			assertQuery("SELECT ABS(e.salary - e.manager.salary) FROM Employee e");
			assertQuery("SELECT MOD(e.hoursWorked, 8) FROM Employee e");
			assertQuery("SELECT SQRT(o.RESULT) FROM Output o");
			assertQuery("SELECT e FROM Employee e WHERE ABS(e.salary - e.manager.salary) > 0");
			assertQuery("SELECT e FROM Employee e WHERE MOD(e.hoursWorked, 8) > 0");
			assertQuery("SELECT e FROM Employee e WHERE SQRT(o.RESULT) > 0.0");
			assertQuery("select ceiling(1.5) from Element a");
			assertQuery("select ln(7.5) from Element a");
		}

		@Test // GH-3136, GH-4272
		void stringFunctions() {

			assertQuery("SELECT CONCAT(e.firstName, ' ', e.lastName) FROM Employee e");
			assertQuery("SELECT LENGTH(e.lastName) FROM Employee e");
			assertQuery("SELECT LOWER(e.lastName) FROM Employee e");
			assertQuery("SELECT UPPER(e.lastName) FROM Employee e");
			assertQuery("SELECT SUBSTRING(e.lastName, 0, 2) FROM Employee e");
			assertQuery("select substring(c.number, 1, 2) from Call c");
			assertQuery("select substring(c.number, 1) from Call c");
			assertQuery(
					"SELECT TRIM(TRAILING FROM e.lastName), TRIM(e.lastName), TRIM(LEADING '-' FROM e.lastName) FROM Employee e");
			assertQuery("SELECT LOCATE('a', e.name) FROM Employee e");
			assertQuery("SELECT LOCATE('a', e.name, 2) FROM Employee e");
			assertQuery("SELECT REPLACE(e.name, 'o', 'a') FROM Employee e");
			assertQuery("SELECT REPLACE(e.name, ' ', '_') FROM Employee e");
			assertQuery("SELECT e FROM Employee e WHERE CONCAT(e.firstName, ' ', e.lastName) = 'Bilbo'");
			assertQuery("SELECT e FROM Employee e WHERE LENGTH(e.lastName) > 0");
			assertQuery("SELECT e FROM Employee e WHERE LOWER(e.lastName) = 'bilbo'");
			assertQuery("SELECT e FROM Employee e WHERE UPPER(e.lastName) = 'BILBO'");
			assertQuery("SELECT e FROM Employee e WHERE SUBSTRING(e.lastName, 0, 2) = 'Bilbo'");
			assertQuery("SELECT e FROM Employee e WHERE TRIM(TRAILING FROM e.lastName) = 'Bilbo'");
			assertQuery("SELECT e FROM Employee e WHERE TRIM(e.lastName) = 'Bilbo'");
			assertQuery("SELECT e FROM Employee e WHERE TRIM(LEADING '-' FROM e.lastName) = 'Bilbo'");
		}

		@ParameterizedTest // GH-3136
		@ValueSource(strings = { "LEFT", "RIGHT" })
		void leftAndRightFunctions(String keyword) {
			assertQuery("SELECT %s(e.name, 3) FROM Employee e".formatted(keyword));
		}

		@Test // GH-3136
		void stringConcatenationOperator() {

			assertQuery("SELECT e.firstname || e.lastname AS name FROM Employee e");
			assertQuery("select e.name || ' ' || e.title from Employee e");
		}

		@Test // GH-3136
		void datetimeFunctions() {

			assertQuery("SELECT EXTRACT(YEAR FROM e.startDate) FROM Employee e");
			assertQuery("SELECT e FROM Employee e WHERE EXTRACT(YEAR FROM e.startDate) = '2023'");
			assertQuery("select CURRENT_DATE from Call c");
			assertQuery("select CURRENT_TIME from Call c");
			assertQuery("select CURRENT_TIMESTAMP from Call c");
			assertQuery("select LOCAL_DATE from Call c");
			assertQuery("select LOCAL_TIME from Call c");
			assertQuery("select LOCAL_DATETIME from Call c");
		}

		@Test
		void nullFunctions() {

			assertQuery("SELECT COALESCE(e.salary, 0) FROM Employee e");
			assertQuery("SELECT NULLIF(e.salary, 0) FROM Employee e");
			assertQuery("SELECT e FROM Employee e WHERE COALESCE(e.salary, 0) > 0");
		}

		@Test
		void collectionFunctions() {

			assertQuery("SELECT toDo FROM Employee e JOIN e.toDoList toDo WHERE INDEX(toDo) = 1");
			assertQuery("SELECT e FROM Employee e WHERE SIZE(e.managedEmployees) < 2");
			assertQuery("""
					SELECT w.name
					FROM Course c JOIN c.studentWaitlist w
					WHERE c.name = 'Calculus'
					AND INDEX(w) = 0
					""");
			assertQuery("""
					SELECT t
					FROM CreditCard c JOIN c.transactionHistory t
					WHERE c.holder.name = 'John Doe' AND INDEX(t) BETWEEN 0 AND 9
					""");
		}

		@Test // GH-4013
		void aggregateFunctions() {

			assertQuery("SELECT COUNT(e) FROM Employee e");
			assertQuery("SELECT COUNT(o) FROM Order o");
			assertQuery("SELECT MAX(e.salary) FROM Employee e");
			assertQuery("SELECT AVG(o.quantity) FROM Order o");
			assertQuery("SELECT MAX(e.age), e.address.city FROM Employee e");
			assertQuery("SELECT MAX(1), e.address.city FROM Employee e");
			assertQuery("SELECT MAX(MIN(MOD(e.salary, 10))), e.address.city FROM Employee e");
			assertQuery("SELECT MIN(MOD(e.salary, 10)), e.address.city FROM Employee e");
			assertQuery("""
					SELECT SUM(l.price)
					FROM Order o JOIN o.lineItems l JOIN o.customer c
					WHERE c.lastname = 'Smith' AND c.firstname = 'John'
					""");
			assertQuery("""
					SELECT COUNT(l.price)
					FROM Order o JOIN o.lineItems l JOIN o.customer c
					WHERE c.lastname = 'Smith' AND c.firstname = 'John'
					""");
			assertQuery("""
					SELECT COUNT(l)
					FROM Order o JOIN o.lineItems l JOIN o.customer c
					WHERE c.lastname = 'Smith' AND c.firstname = 'John' AND l.price IS NOT NULL
					""");
		}

		@ParameterizedTest // GH-3136
		@ValueSource(strings = { "STRING", "INTEGER", "FLOAT", "DOUBLE" })
		void cast(String targetType) {
			assertQuery("SELECT CAST(e.salary AS %s) FROM Employee e".formatted(targetType));
		}

		@Test // GH-3024, GH-3863
		void castInComparison() {

			assertQuery("select cast(i as string) from Item i where cast(i.date as date) <= cast(:currentDateTime as date)");
			assertQuery("SELECT e FROM Employee e WHERE CAST(e.salary NUMERIC(10, 2)) > 0.0");
		}

		@Test // GH-4336
		void idAndVersionFunctions() {

			assertQuery("select id(e) from Employee e");
			assertQuery("select version(e) from Employee e");
			assertQuery("select id(e.dept) from Employee e");
			assertQuery("select e from Employee e where id(e) = :id");
		}

		@Test
		void functionInvocation() {

			assertQuery("SELECT p FROM Phone p WHERE FUNCTION('TO_NUMBER', p.areaCode) > 613");
			assertQuery("SELECT c FROM Customer c WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit) = TRUE");
			assertQuery("SELECT c FROM Customer c WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit)");
		}

		@Test // GH-4326
		void nestedFunctionInvocations() {

			assertQuery("SELECT FUNCTION('round', FUNCTION('abs', e.salary)) + 1 FROM Employee e");
			assertQuery("SELECT LOWER(FUNCTION('normalize', FUNCTION('trim', e.name))) FROM Employee e");
			assertQuery("SELECT EXTRACT(YEAR FROM FUNCTION('date', e.createdAt)) FROM Employee e");
			assertQuery(
					"SELECT e FROM Employee e WHERE e.createdAt BETWEEN FUNCTION('date', :startDate) AND FUNCTION('date', :endDate)");
		}

	}

	@Nested
	class CaseExpressions {

		@Test
		void simpleCase() {

			assertQuery(
					"select e from Employee e where case e.firstName when 'Bob' then 'Robert' when 'Jill' then 'Gillian' else '' end = 'Robert'");
			assertQuery("""
					SELECT e.name,
					    CASE TYPE(e) WHEN Exempt THEN 'Exempt'
					                 WHEN Contractor THEN 'Contractor'
					                 WHEN Intern THEN 'Intern'
					                 ELSE 'NonExempt'
					    END
					FROM Employee e
					WHERE e.dept.name = 'Engineering'
					""");
			assertQuery("""
					UPDATE Employee e
					SET e.salary =
					    CASE e.rating WHEN 1 THEN e.salary * 1.1
					                  WHEN 2 THEN e.salary * 1.05
					                  ELSE e.salary * 1.01
					    END
					""");
		}

		@Test
		void searchedCase() {

			assertQuery(
					"select case when e.firstName = 'Bob' then 'Robert' when e.firstName = 'Jill' then 'Gillian' else '' end from Employee e  where e.firstName = 'Bob' or e.firstName = 'Jill'");
			assertQuery(
					"select e from Employee e where case when e.firstName = 'Bob' then 'Robert' when e.firstName = 'Jill' then 'Gillian' else '' end = 'Robert'");
			assertQuery("""
					SELECT e.name,
					       f.name,
					       CONCAT(CASE WHEN f.annualMiles > 50000 THEN 'Platinum '
					                   WHEN f.annualMiles > 25000 THEN 'Gold '
					                   ELSE ''
					              END,
					       'Frequent Flyer')
					FROM Employee e JOIN e.frequentFlierPlan f
					""");
			assertQuery("""
					UPDATE Employee e
					SET e.salary =
					    CASE WHEN e.rating = 1 THEN e.salary * 1.1
					         WHEN e.rating = 2 THEN e.salary * 1.05
					         ELSE e.salary * 1.01
					    END
					""");
		}

		@Test // GH-4142
		void caseWithoutElse() {

			assertQuery("""
					SELECT e.name,
					    CASE WHEN e.rating = 1 THEN e.salary * 1.1
					         WHEN e.rating = 2 THEN e.salary * 1.05
					    END
					FROM Employee e
					""");
			assertQuery("""
					SELECT e.name,
					    CASE e.rating WHEN 1 THEN e.salary * 1.1
					                  WHEN 2 THEN e.salary * 1.05
					    END
					FROM Employee e
					""");
			assertQuery("""
					UPDATE Employee e
					SET e.salary =
					    CASE WHEN e.rating = 1 THEN e.salary * 1.1
					         WHEN e.rating = 2 THEN e.salary * 1.05
					    END
					""");
		}

	}

	@Nested
	class TypeExpressions {

		@Test // GH-2970, GH-3711
		void entityTypeReference() {

			assertQuery("SELECT TYPE(e) FROM Employee e");
			assertQuery("SELECT TYPE(?0) FROM Employee e");
			assertQuery("SELECT p FROM Project p WHERE TYPE(p) = LargeProject");
			assertQuery("SELECT TYPE(e) FROM Employee e WHERE TYPE(e) <> Exempt");
			assertQuery("SELECT TYPE(e) FROM Employee e WHERE TYPE(e) != Exempt");
			assertQuery("SELECT e FROM Employee e WHERE TYPE(e) IN (Exempt, Contractor)");
			assertQuery("SELECT e FROM Employee e WHERE TYPE(e) IN (:empType1, :empType2)");
			assertQuery("SELECT e FROM Employee e WHERE TYPE(e) IN :empTypes");
		}
	}

	@Nested
	class WhereClause {

		@Test // GH-3061
		void comparisonOperators() {

			assertQuery("SELECT o FROM Order o WHERE o.shippingAddress.state = 'CA'");
			assertQuery("SELECT o FROM Order o WHERE o.shippingAddress <> o.billingAddress");
			assertQuery("select e from Employee e where e.firstName != :name");
			assertQuery("SELECT c.id, c.status FROM Customer c JOIN c.orders o WHERE o.count > 100");
		}

		@Test
		void logicalOperators() {

			assertQuery("""
					SELECT o
					FROM Order o
					WHERE
					NOT (o.shippingAddress.state = o.billingAddress.state AND
					o.shippingAddress.city = o.billingAddress.city AND
					o.shippingAddress.street = o.billingAddress.street)
					""");
		}

		@Test
		void parameters() {

			assertQuery("SELECT c FROM Customer c WHERE c.status = :stat");
			assertQuery("SELECT DISTINCT o FROM Order o JOIN o.lineItems l WHERE l.product.name = ?1");
		}

		@Test
		void between() {
			assertQuery("SELECT e FROM Entity e WHERE e.embeddedId.date NOT BETWEEN 'a' AND 'b'");
		}

		@Test // GH-3873
		void likeWithEscape() {

			assertQuery("select t.name from SomeDbo t where t.name LIKE :name escape '\\\\'");
			assertQuery("SELECT e FROM SampleEntity e WHERE LOWER(e.label) LIKE LOWER(?1) ESCAPE '\\\\'");
			assertQuery("SELECT e FROM SampleEntity e WHERE LOWER(e.label) LIKE LOWER(?1) ESCAPE ?1");
			assertQuery("SELECT e FROM SampleEntity e WHERE LOWER(e.label) LIKE LOWER(?1) ESCAPE :param");
		}

		@Test
		void in() {

			assertQuery("select f from FooEntity f where upper(f.name) IN ('Y', 'Basic', 'Remit')");
			assertQuery(
					"select count(f) from FooEntity f where f.status IN (com.example.eql_bug_check.entity.FooStatus.FOO, com.example.eql_bug_check.entity.FooStatus.BAR)");
		}

		@Test // GH-3314
		void nullComparison() {

			assertQuery("SELECT e FROM Employee e WHERE (e.active IS null OR e.active = true)");
			assertQuery("SELECT e FROM Employee e WHERE (e.active IS NULL OR e.active = true)");
			assertQuery("SELECT e FROM Employee e WHERE (e.active IS NOT null OR e.active = true)");
			assertQuery("SELECT e FROM Employee e WHERE (e.active IS NOT NULL OR e.active = true)");
		}

		@Test
		void emptyCollection() {

			assertQuery("SELECT o FROM Order o WHERE o.lineItems IS EMPTY");
			assertQuery("SELECT o FROM Order o WHERE o.lineItems IS NOT EMPTY");
			assertQuery("SELECT e FROM Employee e WHERE e.managedEmployees IS EMPTY");
		}

		@Test // GH-4278, GH-4292
		void collectionMember() {

			assertQuery("SELECT p FROM Person p WHERE 'Joe' MEMBER OF p.nicknames");
			assertQuery("SELECT e FROM Employee e WHERE 'write code' MEMBER OF e.responsibilities");
			assertQuery("SELECT e FROM Employee e WHERE 'c' MEMBER OF e.responsibilities");
			assertQuery("SELECT e FROM Employee e WHERE 'c' NOT MEMBER OF e.responsibilities");
			assertQuery("SELECT e FROM Employee e WHERE EXISTS (SELECT p FROM Person p WHERE 'c' MEMBER OF p.nicknames)");
			assertQuery("UPDATE Employee e SET e.name = 'x' WHERE 'c' MEMBER OF e.responsibilities");
			assertQuery("DELETE FROM Employee e WHERE 'c' MEMBER OF e.responsibilities");
			assertQuery("SELECT i FROM Item i JOIN i.photos p WHERE VALUE(p) MEMBER OF i.photoValues");
			assertQuery("SELECT i FROM Item i JOIN i.photos p WHERE KEY(p) MEMBER OF i.photoKeys");
			assertQuery("SELECT i FROM Item i JOIN i.photos p WHERE VALUE(p) NOT MEMBER OF i.photoValues");
		}

		@Test
		void exists() {

			assertQuery("""
					SELECT DISTINCT emp
					FROM Employee emp
					WHERE EXISTS (SELECT spouseEmp
					    FROM Employee spouseEmp
					    WHERE spouseEmp = emp.spouse)
					""");
		}

		@Test
		void quantifiedSubquery() {

			assertQuery("""
					SELECT emp
					FROM Employee emp
					WHERE emp.salary > ALL (SELECT m.salary
					FROM Manager m
					WHERE m.department = emp.department)
					""");
		}

		@Test
		void subqueryComparison() {

			assertQuery("SELECT c FROM Customer c WHERE (SELECT AVG(o.price) FROM c.orders o) > 100");
			assertQuery("""
					SELECT goodCustomer
					FROM Customer goodCustomer
					WHERE goodCustomer.balanceOwed < (SELECT AVG(c.balanceOwed) / 2.0 FROM Customer c)
					""");
		}

	}

	@Nested
	class GroupByAndHaving {

		@Test
		void groupBy() {

			assertQuery("SELECT AVG(e.salary), e.address.city FROM Employee e GROUP BY e.address.city");
			assertQuery("SELECT e, COUNT(p) FROM Employee e LEFT JOIN e.projects p GROUP BY e");
		}

		@Test
		void having() {

			assertQuery(
					"SELECT AVG(e.salary), e.address.city FROM Employee e GROUP BY e.address.city HAVING AVG(e.salary) > 100000");
			assertQuery("SELECT c.country, COUNT(c) FROM Customer c GROUP BY c.country HAVING COUNT(c) > 30");
			assertQuery("SELECT c, COUNT(o) FROM Customer c JOIN c.orders o GROUP BY c HAVING COUNT(o) >= 5");
			assertQuery("""
					SELECT c.status, AVG(c.filledOrderCount), COUNT(c)
					FROM Customer c
					GROUP BY c.status
					HAVING c.status IN (1, 2)
					""");
			assertQuery("""
					SELECT COUNT(f)
					FROM FooEntity f
					WHERE f.name IN ('Y', 'Basic', 'Remit')
								AND f.size = 10
					HAVING COUNT(f) > 0
					""");
		}

	}

	@Nested
	class OrderByClause {

		@Test
		void orderBy() {

			assertQuery("SELECT e FROM Employee e ORDER BY e.lastName ASC, e.firstName ASC");
			assertQuery("SELECT e FROM Employee e ORDER BY e.address");
			assertQuery("""
					SELECT o
					FROM Customer c JOIN c.orders o JOIN c.address a
					WHERE a.state = 'CA'
					ORDER BY o.quantity DESC, o.totalcost
					""");
			assertQuery("""
					SELECT o.quantity, a.zipcode
					FROM Customer c JOIN c.orders o JOIN c.address a
					WHERE a.state = 'CA'
					ORDER BY o.quantity, a.zipcode
					""");
		}

		@Test
		void orderBySelectAlias() {

			assertQuery("""
					SELECT o.quantity, o.cost * 1.08 AS taxedCost, a.zipcode
					FROM Customer c JOIN c.orders o JOIN c.address a
					WHERE a.state = 'CA' AND a.county = 'Santa Clara'
					ORDER BY o.quantity, taxedCost, a.zipcode
					""");
			assertQuery("""
					SELECT AVG(o.quantity) as q, a.zipcode
					FROM Customer c JOIN c.orders o JOIN c.address a
					WHERE a.state = 'CA'
					GROUP BY a.zipcode
					ORDER BY q DESC
					""");
			assertQuery("""
					SELECT c, COUNT(l) AS itemCount
					FROM Customer c JOIN c.Orders o JOIN o.lineItems l
					WHERE c.address.state = 'CA'
					GROUP BY c
					ORDER BY itemCount
					""");
		}

		/**
		 * The spec dubs this query illegal for semantic reasons. The parser does not check whether the ORDER BY matches the
		 * SELECT clause and leaves this to the JPA provider.
		 */
		@Test
		void orderByNotReflectedInSelectClause() {

			assertQuery("""
					SELECT p.product_name
					FROM Order o JOIN o.lineItems l JOIN l.product p JOIN o.customer c
					WHERE c.lastname = 'Smith' AND c.firstname = 'John'
					ORDER BY p.price
					""");
		}

		@Test // GH-3136
		void nullHandling() {

			assertQuery("SELECT e FROM Employee e LEFT JOIN e.manager m ORDER BY m.lastName NULLS FIRST");
			assertQuery("select a from Element a order by mutationAm desc nulls first");
			assertQuery("select a from Element a order by mutationAm desc nulls last");
		}

	}

	@Nested
	class SetOperations {

		@Test // GH-3136
		void union() {

			assertQuery("""
					SELECT MAX(e.salary) FROM Employee e WHERE e.address.city = :city1
					UNION SELECT MAX(e.salary) FROM Employee e WHERE e.address.city = :city2
					""");
		}

		@Test // GH-3136
		void intersect() {

			assertQuery("""
					SELECT e FROM Employee e JOIN e.phones p WHERE p.areaCode = :areaCode1
					INTERSECT SELECT e FROM Employee e JOIN e.phones p WHERE p.areaCode = :areaCode2
					""");
		}

		@Test // GH-3136
		void except() {

			assertQuery("""
					SELECT e FROM Employee e
					EXCEPT SELECT e FROM Employee e WHERE e.salary > e.manager.salary
					""");
		}

		@Test // GH-3136
		void combinedSetOperations() {

			assertQuery("""
					select e from Employee e where e.last_name = 'Baggins'
					intersect
					select e from Employee e where e.first_name = 'Samwise'
					union
					select e from Employee e where e.home = 'The Shire'
					except
					select e from Employee e where e.home = 'Isengard'
					""");
		}

	}

	@Nested
	class UpdateAndDelete {

		@Test
		void update() {

			assertQuery("UPDATE Employee e SET e.salary = 60000 WHERE e.salary = 50000");
			assertQuery("UPDATE Customer c SET c.status = 'outstanding' WHERE c.balance < 10000");
			assertQuery("""
					UPDATE Employee e
					SET e.address.building = 22
					WHERE e.address.building = 14
					AND e.address.city = 'Santa Clara'
					AND e.project = 'Jakarta EE'
					""");
		}

		@Test
		void delete() {

			assertQuery("DELETE FROM Employee e WHERE e.department IS NULL");
			assertQuery("DELETE FROM Customer c WHERE c.status = 'inactive'");
			assertQuery("DELETE FROM Customer c WHERE c.status = 'inactive' AND c.orders IS EMPTY");
			assertQuery("DELETE FROM Customer c WHERE c.status = 'inactive' AND c.orders IS NOT EMPTY");
		}

	}

}
