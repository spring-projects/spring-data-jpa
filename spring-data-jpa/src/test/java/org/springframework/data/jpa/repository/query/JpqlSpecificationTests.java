/*
 * Copyright 2022-2023 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests built around examples of JPQL found in the JPA spec
 * https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc<br/>
 * <br/>
 * IMPORTANT: Purely verifies the parser without any transformations.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class JpqlSpecificationTests {

	private static final String SPEC_FAULT = "Disabled due to spec fault> ";

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#example
	 */
	@Test
	void joinExample1() {

		JpqlQueryParser.parseQuery("""
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

		JpqlQueryParser.parseQuery("""
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

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT o1
				FROM Order o1, Order o2
				WHERE o1.quantity > o2.quantity AND
				 o2.customer.lastname = 'Smith' AND
				 o2.customer.firstname= 'John'
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#path-expressions
	 */
	@Test
	void pathExpressionsExample1() {

		JpqlQueryParser.parseQuery("""
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

		JpqlQueryParser.parseQuery("""
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

		JpqlQueryParser.parseQuery("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo.phones p
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#path-expressions
	 */
	@Test
	void pathExpressionsExample4() {

		JpqlQueryParser.parseQuery("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo c JOIN c.phones p
				WHERE e.contactInfo.address.zipcode = '95054'
				""");
	}

	@Test
	void pathExpressionSyntaxExample1() {

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT l.product
				FROM Order AS o JOIN o.lineItems l
				""");
	}

	@Test
	void joinsExample1() {

		JpqlQueryParser.parseQuery("""
				SELECT c FROM Customer c, Employee e WHERE c.hatsize = e.shoesize
				""");
	}

	@Test
	void joinsExample2() {

		JpqlQueryParser.parseQuery("""
				SELECT c FROM Customer c JOIN c.orders o WHERE c.status = 1
				""");
	}

	@Test
	void joinsInnerExample() {

		JpqlQueryParser.parseQuery("""
				SELECT c FROM Customer c INNER JOIN c.orders o WHERE c.status = 1
				""");
	}

	@Test
	void joinsInExample() {

		JpqlQueryParser.parseQuery("""
				SELECT OBJECT(c) FROM Customer c, IN(c.orders) o WHERE c.status = 1
				""");
	}

	@Test
	void doubleJoinExample() {

		JpqlQueryParser.parseQuery("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo c JOIN c.phones p
				WHERE c.address.zipcode = '95054'
				""");
	}

	@Test
	void leftJoinExample() {

		JpqlQueryParser.parseQuery("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinOnExample() {

		JpqlQueryParser.parseQuery("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				    ON p.status = 'inStock'
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinWhereExample() {

		JpqlQueryParser.parseQuery("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				WHERE p.status = 'inStock'
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinFetchExample() {

		JpqlQueryParser.parseQuery("""
				SELECT d
				FROM Department d LEFT JOIN FETCH d.employees
				WHERE d.deptno = 1
				""");
	}

	@Test
	void collectionMemberExample() {

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.product.productType = 'office_supplies'
				""");
	}

	@Test
	void collectionMemberInExample() {

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o, IN(o.lineItems) l
				WHERE l.product.productType = 'office_supplies'
				""");
	}

	@Test
	void fromClauseExample() {

		JpqlQueryParser.parseQuery("""
				SELECT o
				FROM Order AS o JOIN o.lineItems l JOIN l.product p
				""");
	}

	@Test
	void fromClauseDowncastingExample1() {

		JpqlQueryParser.parseQuery("""
				SELECT b.name, b.ISBN
				FROM Order o JOIN TREAT(o.product AS Book) b
				    """);
	}

	@Test
	void fromClauseDowncastingExample2() {

		JpqlQueryParser.parseQuery("""
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

		JpqlQueryParser.parseQuery("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				    OR p.description LIKE "cost overrun"
				    """);
	}

	@Test
	void fromClauseDowncastingExample3fixed() {

		JpqlQueryParser.parseQuery("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				    OR p.description LIKE 'cost overrun'
				    """);
	}

	@Test
	void fromClauseDowncastingExample4() {

		JpqlQueryParser.parseQuery("""
				SELECT e FROM Employee e
				WHERE TREAT(e AS Exempt).vacationDays > 10
				    OR TREAT(e AS Contractor).hours > 100
				    """);
	}

	@Test
	void pathExpressionsNamedParametersExample() {

		JpqlQueryParser.parseQuery("""
				SELECT c
				FROM Customer c
				WHERE c.status = :stat
				""");
	}

	@Test
	void betweenExpressionsExample() {

		JpqlQueryParser.parseQuery("""
				SELECT t
				FROM CreditCard c JOIN c.transactionHistory t
				WHERE c.holder.name = 'John Doe' AND INDEX(t) BETWEEN 0 AND 9
				""");
	}

	@Test
	void isEmptyExample() {

		JpqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS EMPTY
				""");
	}

	@Test
	void memberOfExample() {

		JpqlQueryParser.parseQuery("""
				SELECT p
				FROM Person p
				WHERE 'Joe' MEMBER OF p.nicknames
				""");
	}

	@Test
	void existsSubSelectExample1() {

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE EXISTS (
				    SELECT spouseEmp
				    FROM Employee spouseEmp
				        WHERE spouseEmp = emp.spouse)
				""");
	}

	@Test
	void allExample() {

		JpqlQueryParser.parseQuery("""
				SELECT emp
				FROM Employee emp
				WHERE emp.salary > ALL (
				    SELECT m.salary
				    FROM Manager m
				    WHERE m.department = emp.department)
				    """);
	}

	@Test
	void existsSubSelectExample2() {

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT emp
				FROM Employee emp
				WHERE EXISTS (
				    SELECT spouseEmp
				    FROM Employee spouseEmp
				    WHERE spouseEmp = emp.spouse)
				    """);
	}

	@Test
	void subselectNumericComparisonExample1() {

		JpqlQueryParser.parseQuery("""
				SELECT c
				FROM Customer c
				WHERE (SELECT AVG(o.price) FROM c.orders o) > 100
				""");
	}

	@Test
	void subselectNumericComparisonExample2() {

		JpqlQueryParser.parseQuery("""
				SELECT goodCustomer
				FROM Customer goodCustomer
				WHERE goodCustomer.balanceOwed < (
				    SELECT AVG(c.balanceOwed)/2.0 FROM Customer c)
				""");
	}

	@Test
	void indexExample() {

		JpqlQueryParser.parseQuery("""
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

		JpqlQueryParser.parseQuery("""
				SELECT c
				FROM Customer c
				WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit)
				""");
	}

	@Test
	void functionInvocationExampleWithCorrection() {

		JpqlQueryParser.parseQuery("""
				SELECT c
				FROM Customer c
				WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit) = TRUE
				""");
	}

	@Test
	void updateCaseExample1() {

		JpqlQueryParser.parseQuery("""
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

		JpqlQueryParser.parseQuery("""
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

		JpqlQueryParser.parseQuery("""
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

		JpqlQueryParser.parseQuery("""
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
	void theRest() {

		JpqlQueryParser.parseQuery("""
				SELECT e
				 FROM Employee e
				 WHERE TYPE(e) IN (Exempt, Contractor)
				 """);
	}

	@Test
	void theRest2() {

		JpqlQueryParser.parseQuery("""
				SELECT e
				    FROM Employee e
				    WHERE TYPE(e) IN (:empType1, :empType2)
				""");
	}

	@Test
	void theRest3() {

		JpqlQueryParser.parseQuery("""
				SELECT e
				FROM Employee e
				WHERE TYPE(e) IN :empTypes
				""");
	}

	@Test
	void theRest4() {

		JpqlQueryParser.parseQuery("""
				SELECT TYPE(e)
				FROM Employee e
				WHERE TYPE(e) <> Exempt
				""");
	}

	@Test
	void theRest5() {

		JpqlQueryParser.parseQuery("""
				SELECT c.status, AVG(c.filledOrderCount), COUNT(c)
				FROM Customer c
				GROUP BY c.status
				HAVING c.status IN (1, 2)
				""");
	}

	@Test
	void theRest6() {

		JpqlQueryParser.parseQuery("""
				SELECT c.country, COUNT(c)
				FROM Customer c
				GROUP BY c.country
				HAVING COUNT(c) > 30
				""");
	}

	@Test
	void theRest7() {

		JpqlQueryParser.parseQuery("""
				SELECT c, COUNT(o)
				FROM Customer c JOIN c.orders o
				GROUP BY c
				HAVING COUNT(o) >= 5
				""");
	}

	@Test
	void theRest8() {

		JpqlQueryParser.parseQuery("""
				SELECT c.id, c.status
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void theRest9() {

		JpqlQueryParser.parseQuery("""
				SELECT v.location.street, KEY(i).title, VALUE(i)
				FROM VideoStore v JOIN v.videoInventory i
				WHERE v.location.zipcode = '94301' AND VALUE(i) > 0
				""");
	}

	@Test
	void theRest10() {

		JpqlQueryParser.parseQuery("""
				SELECT o.lineItems FROM Order AS o
				""");
	}

	@Test
	void theRest11() {

		JpqlQueryParser.parseQuery("""
				SELECT c, COUNT(l) AS itemCount
				FROM Customer c JOIN c.Orders o JOIN o.lineItems l
				WHERE c.address.state = 'CA'
				GROUP BY c
				ORDER BY itemCount
				""");
	}

	@Test
	void theRest12() {

		JpqlQueryParser.parseQuery("""
				SELECT NEW com.acme.example.CustomerDetails(c.id, c.status, o.count)
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void theRest13() {

		JpqlQueryParser.parseQuery("""
				SELECT e.address AS addr
				FROM Employee e
				""");
	}

	@Test
	void theRest14() {

		JpqlQueryParser.parseQuery("""
				SELECT AVG(o.quantity) FROM Order o
				""");
	}

	@Test
	void theRest15() {

		JpqlQueryParser.parseQuery("""
				SELECT SUM(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void theRest16() {

		JpqlQueryParser.parseQuery("""
				SELECT COUNT(o) FROM Order o
				""");
	}

	@Test
	void theRest17() {

		JpqlQueryParser.parseQuery("""
				SELECT COUNT(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void theRest18() {

		JpqlQueryParser.parseQuery("""
				SELECT COUNT(l)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John' AND l.price IS NOT NULL
				""");
	}

	@Test
	void theRest19() {

		JpqlQueryParser.parseQuery("""
				SELECT o
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity DESC, o.totalcost
				""");
	}

	@Test
	void theRest20() {

		JpqlQueryParser.parseQuery("""
				SELECT o.quantity, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity, a.zipcode
				""");
	}

	@Test
	void theRest21() {

		JpqlQueryParser.parseQuery("""
				SELECT o.quantity, o.cost*1.08 AS taxedCost, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA' AND a.county = 'Santa Clara'
				ORDER BY o.quantity, taxedCost, a.zipcode
				""");
	}

	@Test
	void theRest22() {

		JpqlQueryParser.parseQuery("""
				SELECT AVG(o.quantity) as q, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				GROUP BY a.zipcode
				ORDER BY q DESC
				""");
	}

	@Test
	void theRest23() {

		JpqlQueryParser.parseQuery("""
				SELECT p.product_name
				FROM Order o JOIN o.lineItems l JOIN l.product p JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				ORDER BY p.price
				""");
	}

	/**
	 * This query is specifically dubbed illegal in the spec. It may actually be failing for a different reason.
	 */
	@Test
	void theRest24() {

		assertThatExceptionOfType(BadJpqlGrammarException.class).isThrownBy(() -> {
			JpqlQueryParser.parseQuery("""
					SELECT p.product_name
					FROM Order o, IN(o.lineItems) l JOIN o.customer c
					WHERE c.lastname = 'Smith' AND c.firstname = 'John'
					ORDER BY o.quantity
					""");
		});
	}

	@Test
	void theRest25() {

		JpqlQueryParser.parseQuery("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				""");
	}

	@Test
	void theRest26() {

		JpqlQueryParser.parseQuery("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				AND c.orders IS EMPTY
				""");
	}

	@Test
	void theRest27() {

		JpqlQueryParser.parseQuery("""
				UPDATE Customer c
				SET c.status = 'outstanding'
				WHERE c.balance < 10000
				""");
	}

	@Test
	void theRest28() {

		JpqlQueryParser.parseQuery("""
				UPDATE Employee e
				SET e.address.building = 22
				WHERE e.address.building = 14
				AND e.address.city = 'Santa Clara'
				AND e.project = 'Jakarta EE'
				""");
	}

	@Test
	void theRest29() {

		JpqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				""");
	}

	@Test
	void theRest30() {

		JpqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress.state = 'CA'
				""");
	}

	@Test
	void theRest31() {

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT o.shippingAddress.state
				FROM Order o
				""");
	}

	@Test
	void theRest32() {

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				""");
	}

	@Test
	void theRest33() {

		JpqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS NOT EMPTY
				""");
	}

	@Test
	void theRest34() {

		JpqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS EMPTY
				""");
	}

	@Test
	void theRest35() {

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.shipped = FALSE
				""");
	}

	@Test
	void theRest36() {

		JpqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE
				NOT (o.shippingAddress.state = o.billingAddress.state AND
				o.shippingAddress.city = o.billingAddress.city AND
				o.shippingAddress.street = o.billingAddress.street)
				""");
	}

	@Test
	void theRest37() {

		JpqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress <> o.billingAddress
				""");
	}

	@Test
	void theRest38() {

		JpqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.product.name = ?1
				""");
	}
}
