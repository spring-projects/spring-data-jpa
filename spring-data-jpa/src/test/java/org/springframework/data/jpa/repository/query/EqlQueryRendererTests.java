/*
 * Copyright 2023-2024 the original author or authors.
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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.query.QueryRenderer.TokenRenderer;

/**
 * Tests built around examples of EQL found in the JPA spec
 * https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc<br/>
 * <br/>
 * IMPORTANT: Purely verifies the parser without any transformations.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 */
class EqlQueryRendererTests {

	private static final String SPEC_FAULT = "Disabled due to spec fault> ";

	/**
	 * Parse the query using {@link EqlParser} then run it through the query-preserving {@link EqlQueryRenderer}.
	 */
	private static String parseWithoutChanges(String query) {

		EqlLexer lexer = new EqlLexer(CharStreams.fromString(query));
		EqlParser parser = new EqlParser(new CommonTokenStream(lexer));

		parser.addErrorListener(new BadJpqlGrammarErrorListener(query));

		EqlParser.StartContext parsedQuery = parser.start();

		return TokenRenderer.render(new EqlQueryRenderer().visit(parsedQuery));
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
	 * for semantic reasons. Our parser does NOT check if the ORDER BY clause matches the SELECT or not. Hence, this is
	 * left to the JPA provider.
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

	@ParameterizedTest // GH-3342
	@ValueSource(strings = { "select 1 from User u", "select -1 from User u", "select +1 from User u",
			"select +1 * -100 from User u", "select count(u) * -0.7f from User u",
			"select count(oi) + (-100) as perc from StockOrderItem oi",
			"select p from Payment p where length(p.cardNumber) between +16 and -20" })
	void signedLiteralShouldWork(String query) {
		assertQuery(query);
	}

	@ParameterizedTest // GH-3342
	@ValueSource(strings = { "select -count(u) from User u", "select +1 * (-count(u)) from User u" })
	void signedExpressionsShouldWork(String query) {
		assertQuery(query);
	}

	@ParameterizedTest // GH-3451
	@MethodSource("reservedWords")
	void entityNameWithPackageContainingReservedWord(String reservedWord) {

		String source = "select new com.company.%s.thing.stuff.ClassName(e.id) from Experience e".formatted(reservedWord);
		assertQuery(source);
	}
}
