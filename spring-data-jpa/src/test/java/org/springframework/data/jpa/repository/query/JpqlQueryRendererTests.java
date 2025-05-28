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

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.data.jpa.repository.query.QueryRenderer.TokenRenderer;

/**
 * Tests built around examples of JPQL found in the JPA spec
 * https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc<br/>
 * <br/>
 * IMPORTANT: Purely verifies the parser without any transformations.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.1
 */
class JpqlQueryRendererTests {

	private static final String SPEC_FAULT = "Disabled due to spec fault> ";

	/**
	 * Parse the query using {@link JpqlParser} then run it through the query-preserving {@link JpqlQueryRenderer}.
	 */
	private static String parseWithoutChanges(String query) {

		JpaQueryEnhancer.JpqlQueryParser parser = JpaQueryEnhancer.JpqlQueryParser.parseQuery(query);

		return TokenRenderer.render(new JpqlQueryRenderer().visit(parser.getContext()));
	}

	static Stream<Arguments> reservedWords() {
		return Stream.of("abs", "exp", "any", "case", "else", "index", "time").map(Arguments::of);
	}

	private void assertQuery(String query) {

		String slimmedDownQuery = reduceWhitespace(query);
		assertThat(parseWithoutChanges(slimmedDownQuery)).isEqualTo(slimmedDownQuery);
	}

	private String reduceWhitespace(String original) {

		return original //
				.replaceAll("[ \\t\\n]{1,}", " ") //
				.trim();
	}

	@Test
	void selectQueries() {

		assertQuery("Select e FROM Employee e WHERE e.salary > 100000");
		assertQuery("Select e FROM Employee e WHERE e.id = :id");
		assertQuery("Select MAX(e.salary) FROM Employee e");
		assertQuery("Select e.firstName FROM Employee e");
		assertQuery("Select e.firstName, e.lastName FROM Employee e");
	}

	@Test
	void selectClause() {

		assertQuery("SELECT COUNT(e) FROM Employee e");
		assertQuery("SELECT MAX(e.salary) FROM Employee e");
		assertQuery("SELECT NEW com.acme.reports.EmpReport(e.firstName, e.lastName, e.salary) FROM Employee e");
	}

	@Test
	void fromClause() {

		assertQuery("SELECT e FROM Employee e");
		assertQuery("SELECT e, a FROM Employee e, MailingAddress a WHERE e.address = a.address");
		assertQuery("SELECT e FROM com.acme.Employee e");
	}

	@Test
	void join() {

		assertQuery("SELECT e FROM Employee e JOIN e.address a WHERE a.city = :city");
		assertQuery("SELECT e FROM Employee e JOIN e.projects p JOIN e.projects p2 WHERE p.name = :p1 AND p2.name = :p2");
	}

	@Test
	void joinFetch() {

		assertQuery("SELECT e FROM Employee e JOIN FETCH e.address");
		assertQuery("SELECT e FROM Employee e JOIN FETCH e.address a ORDER BY a.city");
		assertQuery("SELECT e FROM Employee e JOIN FETCH e.address AS a ORDER BY a.city");
	}

	@Test
	void leftJoin() {
		assertQuery("SELECT e FROM Employee e LEFT JOIN e.address a ORDER BY a.city");
	}

	@Test // GH-3277
	void numericLiterals() {

		assertQuery("SELECT e FROM Employee e WHERE e.id = 1234");
		assertQuery("SELECT e FROM Employee e WHERE e.id = 1234L");
		assertQuery("SELECT s FROM Stat s WHERE s.ratio > 3.14");
		assertQuery("SELECT s FROM Stat s WHERE s.ratio > 3.14F");
		assertQuery("SELECT s FROM Stat s WHERE s.ratio > 3.14e32D");
	}

	@Test // GH-3308
	void newWithStrings() {
		assertQuery("select new com.example.demo.SampleObject(se.id, se.sampleValue, \"java\") from SampleEntity se");
	}

	@Test
	void orderByClause() {

		assertQuery("SELECT e FROM Employee e ORDER BY e.lastName ASC, e.firstName ASC"); // Typo in EQL document
		assertQuery("SELECT e FROM Employee e LEFT JOIN e.manager m ORDER BY m.lastName NULLS FIRST");
		assertQuery("SELECT e FROM Employee e ORDER BY e.address");
	}

	@Test
	void groupByClause() {

		assertQuery("SELECT AVG(e.salary), e.address.city FROM Employee e GROUP BY e.address.city");
		assertQuery("SELECT e, COUNT(p) FROM Employee e LEFT JOIN e.projects p GROUP BY e");
	}

	@Test
	void havingClause() {
		assertQuery(
				"SELECT AVG(e.salary), e.address.city FROM Employee e GROUP BY e.address.city HAVING AVG(e.salary) > 100000");
	}

	@Test // GH-3136
	void union() {

		assertQuery("""
				SELECT MAX(e.salary) FROM Employee e WHERE e.address.city = :city1
				UNION SELECT MAX(e.salary) FROM Employee e WHERE e.address.city = :city2
				""");
		assertQuery("""
				SELECT e FROM Employee e JOIN e.phones p WHERE p.areaCode = :areaCode1
				INTERSECT SELECT e FROM Employee e JOIN e.phones p WHERE p.areaCode = :areaCode2
				""");
		assertQuery("""
				SELECT e FROM Employee e
				EXCEPT SELECT e FROM Employee e WHERE e.salary > e.manager.salary
				""");
	}

	@Test
	void whereClause() {
		// TBD
	}

	@Test
	void updateQueries() {
		assertQuery("UPDATE Employee e SET e.salary = 60000 WHERE e.salary = 50000");
	}

	@Test
	void deleteQueries() {
		assertQuery("DELETE FROM Employee e WHERE e.department IS NULL");
	}

	@Test
	void literals() {

		assertQuery("SELECT e FROM  Employee e WHERE e.name = 'Bob'");
		assertQuery("SELECT e FROM  Employee e WHERE e.id = 1234");
		assertQuery("SELECT e FROM  Employee e WHERE e.id = 1234L");
		assertQuery("SELECT s FROM  Stat s WHERE s.ratio > 3.14F");
		assertQuery("SELECT s FROM  Stat s WHERE s.ratio > 3.14e32D");
		assertQuery("SELECT e FROM  Employee e WHERE e.active = TRUE");
		assertQuery("SELECT e FROM  Employee e WHERE e.startDate = {d'2012-01-03'}");
		assertQuery("SELECT e FROM  Employee e WHERE e.startTime = {t'09:00:00'}");
		assertQuery("SELECT e FROM  Employee e WHERE e.version = {ts'2012-01-03 09:00:00.000000001'}");
		assertQuery("SELECT e FROM  Employee e WHERE e.gender = org.acme.Gender.MALE");
		assertQuery("UPDATE Employee e SET e.manager = NULL WHERE e.manager = :manager");
	}

	@Test
	void functionsInSelect() {

		assertQuery("SELECT e.salary - 1000 FROM Employee e");
		assertQuery("SELECT e.salary + 1000 FROM Employee e");
		assertQuery("SELECT e.salary * 2 FROM Employee e");
		assertQuery("SELECT e.salary * 2.0 FROM Employee e");
		assertQuery("SELECT e.salary / 2 FROM Employee e");
		assertQuery("SELECT e.salary / 2.0 FROM Employee e");
		assertQuery("SELECT ABS(e.salary - e.manager.salary) FROM Employee e");
		assertQuery(
				"select e from Employee e where case e.firstName when 'Bob' then 'Robert' when 'Jill' then 'Gillian' else '' end = 'Robert'");
		assertQuery(
				"select case when e.firstName = 'Bob' then 'Robert' when e.firstName = 'Jill' then 'Gillian' else '' end from Employee e  where e.firstName = 'Bob' or e.firstName = 'Jill'");
		assertQuery(
				"select e from Employee e where case when e.firstName = 'Bob' then 'Robert' when e.firstName = 'Jill' then 'Gillian' else '' end = 'Robert'");
		assertQuery("SELECT COALESCE(e.salary, 0) FROM Employee e");
		assertQuery("SELECT CONCAT(e.firstName, ' ', e.lastName) FROM Employee e");
		assertQuery("SELECT e.name, CURRENT_DATE FROM Employee e");
		assertQuery("SELECT e.name, CURRENT_TIME FROM Employee e");
		assertQuery("SELECT e.name, CURRENT_TIMESTAMP FROM Employee e");
		assertQuery("SELECT LENGTH(e.lastName) FROM Employee e");
		assertQuery("SELECT LOWER(e.lastName) FROM Employee e");
		assertQuery("SELECT MOD(e.hoursWorked, 8) FROM Employee e");
		assertQuery("SELECT NULLIF(e.salary, 0) FROM Employee e");
		assertQuery("SELECT SQRT(o.RESULT) FROM Output o");
		assertQuery("SELECT SUBSTRING(e.lastName, 0, 2) FROM Employee e");
		assertQuery(
				"SELECT TRIM(TRAILING FROM e.lastName), TRIM(e.lastName), TRIM(LEADING '-' FROM e.lastName) FROM Employee e");
		assertQuery("SELECT UPPER(e.lastName) FROM Employee e");
		assertQuery("SELECT CAST(e.salary NUMERIC(10, 2)) FROM Employee e");
		assertQuery("SELECT EXTRACT(YEAR FROM e.startDate) FROM Employee e");
	}

	@Test
	void functionsInWhere() {

		assertQuery("SELECT e FROM Employee e WHERE e.salary - 1000 > 0");
		assertQuery("SELECT e FROM Employee e WHERE e.salary + 1000 > 0");
		assertQuery("SELECT e FROM Employee e WHERE e.salary * 2 > 0");
		assertQuery("SELECT e FROM Employee e WHERE e.salary * 2.0 > 0.0");
		assertQuery("SELECT e FROM Employee e WHERE e.salary / 2 > 0");
		assertQuery("SELECT e FROM Employee e WHERE e.salary / 2.0 > 0.0");
		assertQuery("SELECT e FROM Employee e WHERE ABS(e.salary - e.manager.salary) > 0");
		assertQuery("SELECT e FROM Employee e WHERE COALESCE(e.salary, 0) > 0");
		assertQuery("SELECT e FROM Employee e WHERE CONCAT(e.firstName, ' ', e.lastName) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e WHERE CURRENT_DATE > CURRENT_TIME");
		assertQuery("SELECT e FROM Employee e WHERE CURRENT_TIME > CURRENT_TIMESTAMP");
		assertQuery("SELECT e FROM Employee e WHERE LENGTH(e.lastName) > 0");
		assertQuery("SELECT e FROM Employee e WHERE LOWER(e.lastName) = 'bilbo'");
		assertQuery("SELECT e FROM Employee e WHERE MOD(e.hoursWorked, 8) > 0");
		assertQuery("SELECT e FROM Employee e WHERE SQRT(o.RESULT) > 0.0");
		assertQuery("SELECT e FROM Employee e WHERE SUBSTRING(e.lastName, 0, 2) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e WHERE TRIM(TRAILING FROM e.lastName) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e WHERE TRIM(e.lastName) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e WHERE TRIM(LEADING '-' FROM e.lastName) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e WHERE UPPER(e.lastName) = 'BILBO'");
		assertQuery("SELECT e FROM Employee e WHERE CAST(e.salary NUMERIC(10, 2)) > 0.0");
		assertQuery("SELECT e FROM Employee e WHERE EXTRACT(YEAR FROM e.startDate) = '2023'");
	}

	@Test
	void specialOperators() {

		assertQuery("SELECT toDo FROM Employee e JOIN e.toDoList toDo WHERE INDEX(toDo) = 1");
		assertQuery("SELECT p FROM Employee e JOIN e.priorities p WHERE KEY(p) = 'high'");
		assertQuery("SELECT e FROM Employee e WHERE SIZE(e.managedEmployees) < 2");
		assertQuery("SELECT e FROM Employee e WHERE e.managedEmployees IS EMPTY");
		assertQuery("SELECT e FROM Employee e WHERE 'write code' MEMBER OF e.responsibilities");
		assertQuery("SELECT p FROM Project p WHERE TYPE(p) = LargeProject");

		/**
		 * NOTE: The following query has been altered to properly align with EclipseLink test code despite NOT matching
		 * their ref docs. See https://github.com/eclipse-ee4j/eclipselink/issues/1949 for more details.
		 */
		assertQuery("SELECT e FROM Employee e JOIN TREAT(e.projects AS LargeProject) p WHERE p.budget > 1000000");

		assertQuery("SELECT p FROM Phone p WHERE FUNCTION('TO_NUMBER', p.areaCode) > 613");
	}

	@Test // GH-3314
	void isNullAndIsNotNull() {

		assertQuery("SELECT e FROM Employee e WHERE (e.active IS null OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active IS NULL OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active IS NOT null OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active IS NOT NULL OR e.active = true)");
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

	@ParameterizedTest // GH-3136
	@ValueSource(strings = { "STRING", "INTEGER", "FLOAT", "DOUBLE" })
	void cast(String targetType) {
		assertQuery("SELECT CAST(e.salary AS %s) FROM Employee e".formatted(targetType));
	}

	@ParameterizedTest // GH-3136
	@ValueSource(strings = { "LEFT", "RIGHT" })
	void leftRightStringFunctions(String keyword) {
		assertQuery("SELECT %s(e.name, 3) FROM Employee e".formatted(keyword));
	}

	@Test // GH-3136
	void replaceStringFunctions() {
		assertQuery("SELECT REPLACE(e.name, 'o', 'a') FROM Employee e");
		assertQuery("SELECT REPLACE(e.name, ' ', '_') FROM Employee e");
	}

	@Test // GH-3136
	void stringConcatWithPipes() {
		assertQuery("SELECT e.firstname || e.lastname AS name FROM Employee e");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#example
	 */
	@Test
	void joinExample1() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order AS o JOIN o.lineItems AS l
				WHERE l.shipped = FALSE
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#example
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#identification-variables
	 */
	@Test
	void joinExample2() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l JOIN l.product p
				WHERE p.productType = 'office_supplies'
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#range-variable-declarations
	 */
	@Test
	void rangeVariableDeclarations() {

		assertQuery("""
				SELECT DISTINCT o1
				FROM Order o1, Order o2
				WHERE o1.quantity > o2.quantity AND
				 o2.customer.lastname = 'Smith' AND
				 o2.customer.firstname = 'John'
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#path-expressions
	 */
	@Test
	void pathExpressionsExample1() {

		assertQuery("""
				SELECT i.name, VALUE(p)
				FROM Item i JOIN i.photos p
				WHERE KEY(p) LIKE '%egret'
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#path-expressions
	 */
	@Test
	void pathExpressionsExample2() {

		assertQuery("""
				SELECT i.name, p
				FROM Item i JOIN i.photos p
				WHERE KEY(p) LIKE '%egret'
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#path-expressions
	 */
	@Test
	void pathExpressionsExample3() {

		assertQuery("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo.phones p
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#path-expressions
	 */
	@Test
	void pathExpressionsExample4() {

		assertQuery("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo c JOIN c.phones p
				WHERE e.contactInfo.address.zipcode = '95054'
				""");
	}

	@Test
	void pathExpressionSyntaxExample1() {

		assertQuery("""
				SELECT DISTINCT l.product
				FROM Order AS o JOIN o.lineItems l
				""");
	}

	@Test
	void joinsExample1() {

		assertQuery("""
				SELECT c FROM Customer c, Employee e WHERE c.hatsize = e.shoesize
				""");
	}

	@Test
	void joinsExample2() {

		assertQuery("""
				SELECT c FROM Customer c JOIN c.orders o WHERE c.status = 1
				""");
	}

	@Test
	void joinsInnerExample() {

		assertQuery("""
				SELECT c FROM Customer c INNER JOIN c.orders o WHERE c.status = 1
				""");
	}

	@Test
	void joinsInExample() {

		assertQuery("""
				SELECT OBJECT(c) FROM Customer c, IN(c.orders) o WHERE c.status = 1
				""");
	}

	@Test
	void doubleJoinExample() {

		assertQuery("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo c JOIN c.phones p
				WHERE c.address.zipcode = '95054'
				""");
	}

	@Test
	void leftJoinExample() {

		assertQuery("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinOnExample() {

		assertQuery("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				    ON p.status = 'inStock'
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinWhereExample() {

		assertQuery("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				WHERE p.status = 'inStock'
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinFetchExample() {

		assertQuery("""
				SELECT d
				FROM Department d LEFT JOIN FETCH d.employees
				WHERE d.deptno = 1
				""");
	}

	@Test
	void collectionMemberExample() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.product.productType = 'office_supplies'
				""");
	}

	@Test
	void collectionMemberInExample() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order o, IN(o.lineItems) l
				WHERE l.product.productType = 'office_supplies'
				""");
	}

	@Test
	void fromClauseExample() {

		assertQuery("""
				SELECT o
				FROM Order AS o JOIN o.lineItems l JOIN l.product p
				""");
	}

	@Test
	void fromClauseDowncastingExample1() {

		assertQuery("""
				SELECT b.name, b.ISBN
				FROM Order o JOIN TREAT(o.product AS Book) b
				""");
	}

	@Test
	void fromClauseDowncastingExample2() {

		assertQuery("""
				SELECT e FROM Employee e JOIN TREAT(e.projects AS LargeProject) lp
				WHERE lp.budget > 1000
				""");
	}

	/**
	 * @see #fromClauseDowncastingExample3fixed()
	 */
	@Test
	@Disabled(SPEC_FAULT + "Use double-quotes when it should be using single-quotes for a string literal")
	void fromClauseDowncastingExample3_SPEC_BUG() {

		assertQuery("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				    OR p.description LIKE "cost overrun"
				""");
	}

	@Test
	void fromClauseDowncastingExample3fixed() {

		assertQuery("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				    OR p.description LIKE 'cost overrun'
				""");
	}

	@Test
	void fromClauseDowncastingExample4() {

		assertQuery("""
				SELECT e FROM Employee e
				WHERE TREAT(e AS Exempt).vacationDays > 10
				    OR TREAT(e AS Contractor).hours > 100
				""");
	}

	@Test // GH-3024, GH-3863
	void casting() {

		assertQuery("""
				select cast(i as string) from Item i where cast(i.date as date) <= cast(:currentDateTime as date)
				""");
		assertQuery("SELECT e FROM Employee e WHERE CAST(e.salary NUMERIC(10, 2)) > 0.0");
	}

	@Test // GH-3136
	void substring() {

		assertQuery("select substring(c.number, 1, 2) " + //
				"from Call c");

		assertQuery("select substring(c.number, 1) " + //
				"from Call c");
	}

	@Test // GH-3136
	void currentDateFunctions() {

		assertQuery("select CURRENT_DATE " + //
				"from Call c ");

		assertQuery("select CURRENT_TIME " + //
				"from Call c ");

		assertQuery("select CURRENT_TIMESTAMP " + //
				"from Call c ");

		assertQuery("select LOCAL_DATE " + //
				"from Call c ");

		assertQuery("select LOCAL_TIME " + //
				"from Call c ");

		assertQuery("select LOCAL_DATETIME " + //
				"from Call c ");
	}

	@Test
	void pathExpressionsNamedParametersExample() {

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE c.status = :stat
				""");
	}

	@Test
	void betweenExpressionsExample() {

		assertQuery("""
				SELECT t
				FROM CreditCard c JOIN c.transactionHistory t
				WHERE c.holder.name = 'John Doe' AND INDEX(t) BETWEEN 0 AND 9
				""");
	}

	@Test
	void isEmptyExample() {

		assertQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS EMPTY
				""");
	}

	@Test
	void memberOfExample() {

		assertQuery("""
				SELECT p
				FROM Person p
				WHERE 'Joe' MEMBER OF p.nicknames
				""");
	}

	@Test
	void existsSubSelectExample1() {

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE EXISTS (SELECT spouseEmp
				    FROM Employee spouseEmp
				    WHERE spouseEmp = emp.spouse)
				""");
	}

	@Test
	void allExample() {

		assertQuery("""
				SELECT emp
				FROM Employee emp
				WHERE emp.salary > ALL (SELECT m.salary
				FROM Manager m
				WHERE m.department = emp.department)
				""");
	}

	@Test
	void existsSubSelectExample2() {

		assertQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE EXISTS (SELECT spouseEmp
				    FROM Employee spouseEmp
				    WHERE spouseEmp = emp.spouse)
				""");
	}

	@Test
	void subselectNumericComparisonExample1() {

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE (SELECT AVG(o.price) FROM c.orders o) > 100
				""");
	}

	@Test
	void subselectNumericComparisonExample2() {

		assertQuery("""
				SELECT goodCustomer
				FROM Customer goodCustomer
				WHERE goodCustomer.balanceOwed < (SELECT AVG(c.balanceOwed) / 2.0 FROM Customer c)
				""");
	}

	@Test
	void indexExample() {

		assertQuery("""
				SELECT w.name
				FROM Course c JOIN c.studentWaitlist w
				WHERE c.name = 'Calculus'
				AND INDEX(w) = 0
				""");
	}

	/**
	 * @see #functionInvocationExampleWithCorrection()
	 */
	@Test
	@Disabled(SPEC_FAULT + "FUNCTION calls needs a comparator")
	void functionInvocationExample_SPEC_BUG() {

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit)
				""");
	}

	@Test
	void functionInvocationExampleWithCorrection() {

		assertQuery("""
				SELECT c
				FROM Customer c
				WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit) = TRUE
				""");
	}

	@Test
	void updateCaseExample1() {

		assertQuery("""
				UPDATE Employee e
				SET e.salary =
				    CASE WHEN e.rating = 1 THEN e.salary * 1.1
				         WHEN e.rating = 2 THEN e.salary * 1.05
				         ELSE e.salary * 1.01
				    END
				""");
	}

	@Test
	void updateCaseExample2() {

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
	void selectCaseExample1() {

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
	}

	@Test
	void selectCaseExample2() {

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
	}

	@Test
	void inClauseWithTypeLiteralsShouldWork() {

		assertQuery("""
				SELECT e
				FROM Employee e
				WHERE TYPE(e) IN (Exempt, Contractor)
				""");
	}

	@Test
	void inClauseWithParametersShouldWork() {

		assertQuery("""
				SELECT e
				    FROM Employee e
				    WHERE TYPE(e) IN (:empType1, :empType2)
				""");
	}

	@Test
	void inClauseWithSingleParameterShouldWork() {

		assertQuery("""
				SELECT e
				FROM Employee e
				WHERE TYPE(e) IN :empTypes
				""");
	}

	@Test
	void inClauseWithFunctionAndLiterals() {

		assertQuery("""
				select f from FooEntity f where upper(f.name) IN ('Y', 'Basic', 'Remit')
				""");
		assertQuery(
				"""
						select count(f) from FooEntity f where f.status IN (com.example.eql_bug_check.entity.FooStatus.FOO, com.example.eql_bug_check.entity.FooStatus.BAR)
						""");
	}

	@Test
	void notEqualsForTypeShouldWork() {

		assertQuery("""
				SELECT TYPE(e)
				FROM Employee e
				WHERE TYPE(e) <> Exempt
				""");
	}

	@Test
	void havingWithInClauseShouldWork() {

		assertQuery("""
				SELECT c.status, AVG(c.filledOrderCount), COUNT(c)
				FROM Customer c
				GROUP BY c.status
				HAVING c.status IN (1, 2)
				""");
	}

	@Test
	void havingClauseWithComparisonShouldWork() {

		assertQuery("""
				SELECT c.country, COUNT(c)
				FROM Customer c
				GROUP BY c.country
				HAVING COUNT(c) > 30
				""");

		assertQuery("""
				SELECT COUNT(f)
				FROM FooEntity f
				WHERE f.name IN ('Y', 'Basic', 'Remit')
							AND f.size = 10
				HAVING COUNT(f) > 0
				""");
	}

	@Test
	void havingClauseWithAnotherComparisonShouldWork() {

		assertQuery("""
				SELECT c, COUNT(o)
				FROM Customer c JOIN c.orders o
				GROUP BY c
				HAVING COUNT(o) >= 5
				""");
	}

	@Test
	void whereClauseWithComparisonShouldWork() {

		assertQuery("""
				SELECT c.id, c.status
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void keyValueFunctionsShouldWork() {

		assertQuery("""
				SELECT v.location.street, KEY(i).title, VALUE(i)
				FROM VideoStore v JOIN v.videoInventory i
				WHERE v.location.zipcode = '94301' AND VALUE(i) > 0
				""");
	}

	@Test
	void fromClauseWithAsShouldWork() {

		assertQuery("""
				SELECT o.lineItems FROM Order AS o
				""");
	}

	@Test
	void countFunctionWithAsClauseShouldWork() {

		assertQuery("""
				SELECT c, COUNT(l) AS itemCount
				FROM Customer c JOIN c.Orders o JOIN o.lineItems l
				WHERE c.address.state = 'CA'
				GROUP BY c
				ORDER BY itemCount
				""");
	}

	@Test
	void objectConstructionShouldWork() {

		assertQuery("""
				SELECT NEW com.acme.example.CustomerDetails(c.id, c.status, o.count)
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void selectWithAsClauseShouldWork() {

		assertQuery("""
				SELECT e.address AS addr
				FROM Employee e
				""");
	}

	@Test
	void averageFunctionShouldWork() {

		assertQuery("""
				SELECT AVG(o.quantity) FROM Order o
				""");
	}

	@Test
	void sumFunctionShouldWork() {

		assertQuery("""
				SELECT SUM(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void countFunctionShouldWork() {

		assertQuery("""
				SELECT COUNT(o) FROM Order o
				""");
	}

	@Test
	void countFunctionOnSubElementShouldWork() {

		assertQuery("""
				SELECT COUNT(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void equivalentCountFunctionShouldAlsoWork() {

		assertQuery("""
				SELECT COUNT(l)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John' AND l.price IS NOT NULL
				""");
	}

	@Test
	void orderByBasedOnSelectClauseShouldWork() {

		assertQuery("""
				SELECT o
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity DESC, o.totalcost
				""");
	}

	@Test
	void orderByThatMatchesSelectClauseShouldWork() {

		assertQuery("""
				SELECT o.quantity, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity, a.zipcode
				""");
	}

	@Test
	void orderByThatMatchesAllSelectAliasesShouldWork() {

		assertQuery("""
				SELECT o.quantity, o.cost * 1.08 AS taxedCost, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA' AND a.county = 'Santa Clara'
				ORDER BY o.quantity, taxedCost, a.zipcode
				""");
	}

	@Test
	void orderByThatMatchesSelectFunctionAliasShouldWork() {

		assertQuery("""
				SELECT AVG(o.quantity) as q, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				GROUP BY a.zipcode
				ORDER BY q DESC
				""");
	}

	/**
	 * NOTE: This query is specifically dubbed illegal in the spec. However, it's not due to a grammar failure but instead
	 * for semantic reasons. Our parser does NOT check if the ORDER BY matches the SELECT or not. Hence, this is left to
	 * the JPA provider.
	 */
	@Test
	void orderByClauseThatIsNotReflectedInTheSelectClause() {

		assertQuery("""
				SELECT p.product_name
				FROM Order o JOIN o.lineItems l JOIN l.product p JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				ORDER BY p.price
				""");
	}

	/**
	 * NOTE: This query is specifically dubbed illegal in the spec. It may actually be failing for a different reason.
	 */
	@Test
	void orderByClauseThatIsNotReflectedInTheSelectClauseButAlsoHasAnInClauseInTheFromClause() {

		assertThatExceptionOfType(BadJpqlGrammarException.class).isThrownBy(() -> {
			assertQuery("""
					SELECT p.product_name
					FROM Order o, IN(o.lineItems) l JOIN o.customer c
					WHERE c.lastname = 'Smith' AND c.firstname = 'John'
					ORDER BY o.quantity
					""");
		});
	}

	@Test
	void simpleDeleteShouldWork() {

		assertQuery("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				""");
	}

	@Test
	void deleteWithMoreComplexCriteriaShouldWork() {

		assertQuery("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				AND c.orders IS EMPTY
				""");
	}

	@Test
	void simpleUpdateShouldWork() {

		assertQuery("""
				UPDATE Customer c
				SET c.status = 'outstanding'
				WHERE c.balance < 10000
				""");
	}

	@Test
	void moreComplexUpdateShouldWork() {

		assertQuery("""
				UPDATE Employee e
				SET e.address.building = 22
				WHERE e.address.building = 14
				AND e.address.city = 'Santa Clara'
				AND e.project = 'Jakarta EE'
				""");
	}

	@Test
	void simpleSelectShouldWork() {

		assertQuery("""
				SELECT o
				FROM Order o
				""");
	}

	@Test
	void selectWithWhereClauseShouldWork() {

		assertQuery("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress.state = 'CA'
				""");
	}

	@Test
	void selectWithDistinctSubElementShouldWork() {

		assertQuery("""
				SELECT DISTINCT o.shippingAddress.state
				FROM Order o
				""");
	}

	@Test
	void selectWithSimpleDistinctShouldWork() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				""");
	}

	@Test
	void selectWithIsNotEmptyCriteriaShouldWork() {

		assertQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS NOT EMPTY
				""");
	}

	@Test
	void selectWithIsEmptyCriteriaShouldWork() {

		assertQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS EMPTY
				""");
	}

	@Test
	void findAllPendingOrders() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.shipped = FALSE
				""");
	}

	@Test
	void findAllOrdersWhereShippingAddressDoesNotMatchBillingAddress() {

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
	void simplerVersionOfShippingAddressNotMatchingBillingAddress() {

		assertQuery("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress <> o.billingAddress
				""");
	}

	@Test
	void findOrdersThatHaveProductNamedByAParameter() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.product.name = ?1
				""");
	}

	@Test // GH-2982
	void floorShouldBeValidEntityName() {

		assertQuery("""
				SELECT f
				FROM Floor f
				WHERE f.name = :name
				""");

		assertQuery("""
				SELECT r
				FROM Room r
				JOIN r.floor f
				WHERE f.name = :name
				""");
	}

	@Test // GH-2994
	void queryWithSignShouldWork() {
		assertQuery("select t.sign from TestEntity t");
	}

	@Test // GH-3028
	void queryWithValueShouldWork() {
		assertQuery("select t.value from TestEntity t");
	}

	@Test // GH-3062, GH-3056
	void typeShouldBeAValidParameter() {

		assertQuery("select e from Employee e where e.type = :_type");
		assertQuery("select te from TestEntity te where te.type = :type");
	}

	@Test // GH-3061
	void alternateNotEqualsOperatorShouldWork() {
		assertQuery("select e from Employee e where e.firstName != :name");
	}

	@Test
	void regexShouldWork() {
		assertQuery("select e from Employee e where e.lastName REGEXP '^Dr\\.*'");
	}

	@Test // GH-3092
	void dateAndFromShouldBeValidNames() {
		assertQuery("SELECT e FROM Entity e WHERE e.embeddedId.date BETWEEN :from AND :to");
	}

	@Test
	void betweenStrings() {
		assertQuery("SELECT e FROM Entity e WHERE e.embeddedId.date NOT BETWEEN 'a' AND 'b'");
	}

	@Test
	void betweenDates() {
		assertQuery("SELECT e FROM Entity e WHERE e.embeddedId.date BETWEEN CURRENT_DATE AND CURRENT_TIME");
	}

	@Test // GH-3092
	void timeShouldBeAValidParameterName() {
		assertQuery("""
				UPDATE Lock L
				SET L.isLocked = TRUE, L.forceUnlockTime = :forceUnlockTime
				WHERE L.isLocked = FALSE OR L.forceUnlockTime < :time
				""");
	}

	@Test // GH-3128
	void newShouldBeLegalAsPartOfAStateFieldPathExpression() {

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

	@Test // GH-3143
	void powerShouldBeLegalInAQuery() {
		assertQuery("select e.power.id from MyEntity e");
	}

	@Test // GH-3136
	void doublePipeShouldBeValidAsAStringConcatOperator() {

		assertQuery("""
				select e.name || ' ' || e.title
				from Employee e
				""");
	}

	@Test // GH-3136
	void combinedSelectStatementsShouldWork() {

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

	@Disabled
	@Test // GH-3136
	void additionalStringOperationsShouldWork() {

		assertQuery("""
				select
					replace(e.name, 'Baggins', 'Proudfeet'),
					left(e.role, 4),
					right(e.home, 5),
					cast(e.distance_from_home, int)
				from Employee e
				""");
	}

	@Test // GH-3136
	void orderByWithNullsFirstOrLastShouldWork() {

		assertQuery("""
				select a
				from Element a
				order by mutationAm desc nulls first
				""");

		assertQuery("""
				select a
				from Element a
				order by mutationAm desc nulls last
				""");
	}

	@ParameterizedTest // GH-3342
	@ValueSource(strings = { "select 1 as value from User u", "select -1 as value from User u",
			"select +1 as value from User u", "select +1 * -100 as value from User u",
			"select count(u) * -0.7f as value from User u", "select count(oi) + (-100) as perc from StockOrderItem oi",
			"select p from Payment p where length(p.cardNumber) between +16 and -20" })
	void signedLiteralShouldWork(String query) {
		assertQuery(query);
	}

	@ParameterizedTest // GH-3342
	@ValueSource(strings = { "select -count(u) from User u", "select +1 * (-count(u)) from User u" })
	void signedExpressionsShouldWork(String query) {
		assertQuery(query);
	}

	@Test // GH-3873
	void escapeClauseShouldWork() {
		assertQuery("select t.name from SomeDbo t where t.name LIKE :name escape '\\\\'");
		assertQuery("SELECT e FROM SampleEntity e WHERE LOWER(e.label) LIKE LOWER(?1) ESCAPE '\\\\'");
		assertQuery("SELECT e FROM SampleEntity e WHERE LOWER(e.label) LIKE LOWER(?1) ESCAPE ?1");
		assertQuery("SELECT e FROM SampleEntity e WHERE LOWER(e.label) LIKE LOWER(?1) ESCAPE :param");
	}

	@ParameterizedTest // GH-3451
	@MethodSource("reservedWords")
	void entityNameWithPackageContainingReservedWord(String reservedWord) {

		String source = "select new com.company.%s.thing.stuff.ClassName(e.id) from Experience e".formatted(reservedWord);
		assertQuery(source);
	}

	@Test // GH-3496
	void lateralShouldBeAValidParameter() {

		assertQuery("select e from Employee e where e.lateral = :_lateral");
		assertQuery("select te from TestEntity te where te.lateral = :lateral");
	}

	@Test // GH-3834
	void reservedWordsShouldWork() {

		assertQuery("select ie from ItemExample ie left join ie.object io where io.externalId = :externalId");
		assertQuery("select ie.object from ItemExample ie left join ie.object io where io.externalId = :externalId");
		assertQuery("select ie from ItemExample ie left join ie.object io where io.object = :externalId");
		assertQuery("select ie from ItemExample ie where ie.status = com.app.domain.object.Status.UP");
		assertQuery("select f from FooEntity f where upper(f.name) IN :names");
		assertQuery("select f from FooEntity f where f.size IN :sizes");
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
	}

	@Test // GH-3902
	void queryWithoutIdentificationVariableShouldWork() {

		assertQuery("SELECT name, lastname from Person");
		assertQuery("SELECT name, lastname from Person WHERE lastname = 'Doe' ORDER BY name, lastname");
		assertQuery("SELECT name, lastname from Person JOIN department");
	}

}
