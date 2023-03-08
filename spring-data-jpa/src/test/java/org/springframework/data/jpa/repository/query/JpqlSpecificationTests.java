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
import static org.springframework.data.jpa.repository.query.JpqlQueryParser.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests built around examples of JPQL found in the JPA spec
 * https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc
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

		parse("""
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

		parse("""
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

		parse("""
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

		parse("""
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

		parse("""
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

		parse("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo.phones p
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#path-expressions
	 */
	@Test
	void pathExpressionsExample4() {

		parse("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo c JOIN c.phones p
				WHERE e.contactInfo.address.zipcode = '95054'
				""");
	}

	@Test
	void pathExpressionSyntaxExample1() {

		parse("""
				SELECT DISTINCT l.product
				FROM Order AS o JOIN o.lineItems l
				""");
	}

	@Test
	void joinsExample1() {

		parse("""
				SELECT c FROM Customer c, Employee e WHERE c.hatsize = e.shoesize
				""");
	}

	@Test
	void joinsExample2() {

		parse("""
				SELECT c FROM Customer c JOIN c.orders o WHERE c.status = 1
				""");
	}

	@Test
	void joinsInnerExample() {

		parse("""
				SELECT c FROM Customer c INNER JOIN c.orders o WHERE c.status = 1
				""");
	}

	@Test
	void joinsInExample() {

		parse("""
				SELECT OBJECT(c) FROM Customer c, IN(c.orders) o WHERE c.status = 1
				""");
	}

	@Test
	void doubleJoinExample() {

		parse("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo c JOIN c.phones p
				WHERE c.address.zipcode = '95054'
				""");
	}

	@Test
	void leftJoinExample() {

		parse("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinOnExample() {

		parse("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				    ON p.status = 'inStock'
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinWhereExample() {

		parse("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				WHERE p.status = 'inStock'
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinFetchExample() {

		parse("""
				SELECT d
				FROM Department d LEFT JOIN FETCH d.employees
				WHERE d.deptno = 1
				""");
	}

	@Test
	void collectionMemberExample() {

		parse("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.product.productType = 'office_supplies'
				""");
	}

	@Test
	void collectionMemberInExample() {

		parse("""
				SELECT DISTINCT o
				FROM Order o, IN(o.lineItems) l
				WHERE l.product.productType = 'office_supplies'
				""");
	}

	@Test
	void fromClauseExample() {

		parse("""
				SELECT o
				FROM Order AS o JOIN o.lineItems l JOIN l.product p
				""");
	}

	@Test
	void fromClauseDowncastingExample1() {

		parse("""
				SELECT b.name, b.ISBN
				FROM Order o JOIN TREAT(o.product AS Book) b
				    """);
	}

	@Test
	void fromClauseDowncastingExample2() {

		parse("""
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

		parse("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				    OR p.description LIKE "cost overrun"
				    """);
	}

	@Test
	void fromClauseDowncastingExample3fixed() {

		parse("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				    OR p.description LIKE 'cost overrun'
				    """);
	}

	@Test
	void fromClauseDowncastingExample4() {

		parse("""
				SELECT e FROM Employee e
				WHERE TREAT(e AS Exempt).vacationDays > 10
				    OR TREAT(e AS Contractor).hours > 100
				    """);
	}

	@Test
	void pathExpressionsNamedParametersExample() {

		parse("""
				SELECT c
				FROM Customer c
				WHERE c.status = :stat
				""");
	}

	@Test
	void betweenExpressionsExample() {

		parse("""
				SELECT t
				FROM CreditCard c JOIN c.transactionHistory t
				WHERE c.holder.name = 'John Doe' AND INDEX(t) BETWEEN 0 AND 9
				""");
	}

	@Test
	void isEmptyExample() {

		parse("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS EMPTY
				""");
	}

	@Test
	void memberOfExample() {

		parse("""
				SELECT p
				FROM Person p
				WHERE 'Joe' MEMBER OF p.nicknames
				""");
	}

	@Test
	void existsSubSelectExample1() {

		parse("""
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

		parse("""
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

		parse("""
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

		parse("""
				SELECT c
				FROM Customer c
				WHERE (SELECT AVG(o.price) FROM c.orders o) > 100
				""");
	}

	@Test
	void subselectNumericComparisonExample2() {

		parse("""
				SELECT goodCustomer
				FROM Customer goodCustomer
				WHERE goodCustomer.balanceOwed < (
				    SELECT AVG(c.balanceOwed)/2.0 FROM Customer c)
				""");
	}

	@Test
	void indexExample() {

		parse("""
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

		parse("""
				SELECT c
				FROM Customer c
				WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit)
				""");
	}

	@Test
	void functionInvocationExampleWithCorrection() {

		parse("""
				SELECT c
				FROM Customer c
				WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit) = TRUE
				""");
	}

	@Test
	void updateCaseExample1() {

		parse("""
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

		parse("""
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

		parse("""
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

		parse("""
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

		parse("""
				SELECT e
				 FROM Employee e
				 WHERE TYPE(e) IN (Exempt, Contractor)
				 """);
	}

	@Test
	void theRest2() {

		parse("""
				SELECT e
				    FROM Employee e
				    WHERE TYPE(e) IN (:empType1, :empType2)
				""");
	}

	@Test
	void theRest3() {

		parse("""
				SELECT e
				FROM Employee e
				WHERE TYPE(e) IN :empTypes
				""");
	}

	@Test
	void theRest4() {

		parse("""
				SELECT TYPE(e)
				FROM Employee e
				WHERE TYPE(e) <> Exempt
				""");
	}

	@Test
	void theRest5() {

		parse("""
				SELECT c.status, AVG(c.filledOrderCount), COUNT(c)
				FROM Customer c
				GROUP BY c.status
				HAVING c.status IN (1, 2)
				""");
	}

	@Test
	void theRest6() {

		parse("""
				SELECT c.country, COUNT(c)
				FROM Customer c
				GROUP BY c.country
				HAVING COUNT(c) > 30
				""");
	}

	@Test
	void theRest7() {

		parse("""
				SELECT c, COUNT(o)
				FROM Customer c JOIN c.orders o
				GROUP BY c
				HAVING COUNT(o) >= 5
				""");
	}

	@Test
	void theRest8() {

		parse("""
				SELECT c.id, c.status
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void theRest9() {

		parse("""
				SELECT v.location.street, KEY(i).title, VALUE(i)
				FROM VideoStore v JOIN v.videoInventory i
				WHERE v.location.zipcode = '94301' AND VALUE(i) > 0
				""");
	}

	@Test
	void theRest10() {

		parse("""
				SELECT o.lineItems FROM Order AS o
				""");
	}

	@Test
	void theRest11() {

		parse("""
				SELECT c, COUNT(l) AS itemCount
				FROM Customer c JOIN c.Orders o JOIN o.lineItems l
				WHERE c.address.state = 'CA'
				GROUP BY c
				ORDER BY itemCount
				""");
	}

	@Test
	void theRest12() {

		parse("""
				SELECT NEW com.acme.example.CustomerDetails(c.id, c.status, o.count)
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void theRest13() {

		parse("""
				SELECT e.address AS addr
				FROM Employee e
				""");
	}

	@Test
	void theRest14() {

		parse("""
				SELECT AVG(o.quantity) FROM Order o
				""");
	}

	@Test
	void theRest15() {

		parse("""
				SELECT SUM(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void theRest16() {

		parse("""
				SELECT COUNT(o) FROM Order o
				""");
	}

	@Test
	void theRest17() {

		parse("""
				SELECT COUNT(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void theRest18() {

		parse("""
				SELECT COUNT(l)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John' AND l.price IS NOT NULL
				""");
	}

	@Test
	void theRest19() {

		parse("""
				SELECT o
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity DESC, o.totalcost
				""");
	}

	@Test
	void theRest20() {

		parse("""
				SELECT o.quantity, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity, a.zipcode
				""");
	}

	@Test
	void theRest21() {

		parse("""
				SELECT o.quantity, o.cost*1.08 AS taxedCost, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA' AND a.county = 'Santa Clara'
				ORDER BY o.quantity, taxedCost, a.zipcode
				""");
	}

	@Test
	void theRest22() {

		parse("""
				SELECT AVG(o.quantity) as q, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				GROUP BY a.zipcode
				ORDER BY q DESC
				""");
	}

	@Test
	void theRest23() {

		parse("""
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

		assertThatExceptionOfType(QueryParsingSyntaxError.class).isThrownBy(() -> {
			parse("""
					SELECT p.product_name
					FROM Order o, IN(o.lineItems) l JOIN o.customer c
					WHERE c.lastname = 'Smith' AND c.firstname = 'John'
					ORDER BY o.quantity
					""");
		});
	}

	@Test
	void theRest25() {

		parse("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				""");
	}

	@Test
	void theRest26() {

		parse("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				AND c.orders IS EMPTY
				""");
	}

	@Test
	void theRest27() {

		parse("""
				UPDATE Customer c
				SET c.status = 'outstanding'
				WHERE c.balance < 10000
				""");
	}

	@Test
	void theRest28() {

		parse("""
				UPDATE Employee e
				SET e.address.building = 22
				WHERE e.address.building = 14
				AND e.address.city = 'Santa Clara'
				AND e.project = 'Jakarta EE'
				""");
	}

	@Test
	void theRest29() {

		parse("""
				SELECT o
				FROM Order o
				""");
	}

	@Test
	void theRest30() {

		parse("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress.state = 'CA'
				""");
	}

	@Test
	void theRest31() {

		parse("""
				SELECT DISTINCT o.shippingAddress.state
				FROM Order o
				""");
	}

	@Test
	void theRest32() {

		parse("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				""");
	}

	@Test
	void theRest33() {

		parse("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS NOT EMPTY
				""");
	}

	@Test
	void theRest34() {

		parse("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS EMPTY
				""");
	}

	@Test
	void theRest35() {

		parse("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.shipped = FALSE
				""");
	}

	@Test
	void theRest36() {

		parse("""
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

		parse("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress <> o.billingAddress
				""");
	}

	@Test
	void theRest38() {

		parse("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.product.name = ?1
				""");
	}
}
