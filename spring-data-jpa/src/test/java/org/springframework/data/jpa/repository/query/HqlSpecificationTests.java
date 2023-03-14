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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests built around examples of HQL found in
 * https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc and
 * https://docs.jboss.org/hibernate/orm/6.1/userguide/html_single/Hibernate_User_Guide.html#query-language<br/>
 * <br/>
 * IMPORTANT: Purely verifies the parser without any transformations.
 *
 * @author Greg Turnquist
 * @since 3.1
 */
class HqlSpecificationTests {

	private static final String SPEC_FAULT = "Disabled due to spec fault> ";

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#example
	 */
	@Test
	void joinExample1() {

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo.phones p
				""");
	}

	/**
	 * @see https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc#path-expressions
	 */
	@Test
	void pathExpressionsExample4() {

		HqlQueryParser.parseQuery("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo c JOIN c.phones p
				WHERE e.contactInfo.address.zipcode = '95054'
				""");
	}

	@Test
	void pathExpressionSyntaxExample1() {

		HqlQueryParser.parseQuery("""
				SELECT DISTINCT l.product
				FROM Order AS o JOIN o.lineItems l
				""");
	}

	@Test
	void joinsExample1() {

		HqlQueryParser.parseQuery("""
				SELECT c FROM Customer c, Employee e WHERE c.hatsize = e.shoesize
				""");
	}

	@Test
	void joinsExample2() {

		HqlQueryParser.parseQuery("""
				SELECT c FROM Customer c JOIN c.orders o WHERE c.status = 1
				""");
	}

	@Test
	void joinsInnerExample() {

		HqlQueryParser.parseQuery("""
				SELECT c FROM Customer c INNER JOIN c.orders o WHERE c.status = 1
				""");
	}

	@Test
	void joinsInExample() {

		HqlQueryParser.parseQuery("""
				SELECT OBJECT(c) FROM Customer c, IN(c.orders) o WHERE c.status = 1
				""");
	}

	@Test
	void doubleJoinExample() {

		HqlQueryParser.parseQuery("""
				SELECT p.vendor
				FROM Employee e JOIN e.contactInfo c JOIN c.phones p
				WHERE c.address.zipcode = '95054'
				""");
	}

	@Test
	void leftJoinExample() {

		HqlQueryParser.parseQuery("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinOnExample() {

		HqlQueryParser.parseQuery("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				    ON p.status = 'inStock'
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinWhereExample() {

		HqlQueryParser.parseQuery("""
				SELECT s.name, COUNT(p)
				FROM Suppliers s LEFT JOIN s.products p
				WHERE p.status = 'inStock'
				GROUP BY s.name
				""");
	}

	@Test
	void leftJoinFetchExample() {

		HqlQueryParser.parseQuery("""
				SELECT d
				FROM Department d LEFT JOIN FETCH d.employees
				WHERE d.deptno = 1
				""");
	}

	@Test
	void collectionMemberExample() {

		HqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.product.productType = 'office_supplies'
				""");
	}

	@Test
	void collectionMemberInExample() {

		HqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o, IN(o.lineItems) l
				WHERE l.product.productType = 'office_supplies'
				""");
	}

	@Test
	void fromClauseExample() {

		HqlQueryParser.parseQuery("""
				SELECT o
				FROM Order AS o JOIN o.lineItems l JOIN l.product p
				""");
	}

	@Test
	void fromClauseDowncastingExample1() {

		HqlQueryParser.parseQuery("""
				SELECT b.name, b.ISBN
				FROM Order o JOIN TREAT(o.product AS Book) b
				    """);
	}

	@Test
	void fromClauseDowncastingExample2() {

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				    OR p.description LIKE "cost overrun"
				    """);
	}

	@Test
	void fromClauseDowncastingExample3fixed() {

		HqlQueryParser.parseQuery("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				    OR p.description LIKE 'cost overrun'
				    """);
	}

	@Test
	void fromClauseDowncastingExample4() {

		HqlQueryParser.parseQuery("""
				SELECT e FROM Employee e
				WHERE TREAT(e AS Exempt).vacationDays > 10
				    OR TREAT(e AS Contractor).hours > 100
				    """);
	}

	@Test
	void pathExpressionsNamedParametersExample() {

		HqlQueryParser.parseQuery("""
				SELECT c
				FROM Customer c
				WHERE c.status = :stat
				""");
	}

	@Test
	void betweenExpressionsExample() {

		HqlQueryParser.parseQuery("""
				SELECT t
				FROM CreditCard c JOIN c.transactionHistory t
				WHERE c.holder.name = 'John Doe' AND INDEX(t) BETWEEN 0 AND 9
				""");
	}

	@Test
	void isEmptyExample() {

		HqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS EMPTY
				""");
	}

	@Test
	void memberOfExample() {

		HqlQueryParser.parseQuery("""
				SELECT p
				FROM Person p
				WHERE 'Joe' MEMBER OF p.nicknames
				""");
	}

	@Test
	void existsSubSelectExample1() {

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
				SELECT c
				FROM Customer c
				WHERE (SELECT AVG(o.price) FROM c.orders o) > 100
				""");
	}

	@Test
	void subselectNumericComparisonExample2() {

		HqlQueryParser.parseQuery("""
				SELECT goodCustomer
				FROM Customer goodCustomer
				WHERE goodCustomer.balanceOwed < (
				    SELECT AVG(c.balanceOwed)/2.0 FROM Customer c)
				""");
	}

	@Test
	void indexExample() {

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
				SELECT c
				FROM Customer c
				WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit)
				""");
	}

	@Test
	void functionInvocationExampleWithCorrection() {

		HqlQueryParser.parseQuery("""
				SELECT c
				FROM Customer c
				WHERE FUNCTION('hasGoodCredit', c.balance, c.creditLimit) = TRUE
				""");
	}

	@Test
	void updateCaseExample1() {

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
				SELECT e
				 FROM Employee e
				 WHERE TYPE(e) IN (Exempt, Contractor)
				 """);
	}

	@Test
	void theRest2() {

		HqlQueryParser.parseQuery("""
				SELECT e
				    FROM Employee e
				    WHERE TYPE(e) IN (:empType1, :empType2)
				""");
	}

	@Test
	void theRest3() {

		HqlQueryParser.parseQuery("""
				SELECT e
				FROM Employee e
				WHERE TYPE(e) IN :empTypes
				""");
	}

	@Test
	void theRest4() {

		HqlQueryParser.parseQuery("""
				SELECT TYPE(e)
				FROM Employee e
				WHERE TYPE(e) <> Exempt
				""");
	}

	@Test
	void theRest5() {

		HqlQueryParser.parseQuery("""
				SELECT c.status, AVG(c.filledOrderCount), COUNT(c)
				FROM Customer c
				GROUP BY c.status
				HAVING c.status IN (1, 2)
				""");
	}

	@Test
	void theRest6() {

		HqlQueryParser.parseQuery("""
				SELECT c.country, COUNT(c)
				FROM Customer c
				GROUP BY c.country
				HAVING COUNT(c) > 30
				""");
	}

	@Test
	void theRest7() {

		HqlQueryParser.parseQuery("""
				SELECT c, COUNT(o)
				FROM Customer c JOIN c.orders o
				GROUP BY c
				HAVING COUNT(o) >= 5
				""");
	}

	@Test
	void theRest8() {

		HqlQueryParser.parseQuery("""
				SELECT c.id, c.status
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void theRest9() {

		HqlQueryParser.parseQuery("""
				SELECT v.location.street, KEY(i).title, VALUE(i)
				FROM VideoStore v JOIN v.videoInventory i
				WHERE v.location.zipcode = '94301' AND VALUE(i) > 0
				""");
	}

	@Test
	void theRest10() {

		HqlQueryParser.parseQuery("""
				SELECT o.lineItems FROM Order AS o
				""");
	}

	@Test
	void theRest11() {

		HqlQueryParser.parseQuery("""
				SELECT c, COUNT(l) AS itemCount
				FROM Customer c JOIN c.Orders o JOIN o.lineItems l
				WHERE c.address.state = 'CA'
				GROUP BY c
				ORDER BY itemCount
				""");
	}

	@Test
	void theRest12() {

		HqlQueryParser.parseQuery("""
				SELECT NEW com.acme.example.CustomerDetails(c.id, c.status, o.count)
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void theRest13() {

		HqlQueryParser.parseQuery("""
				SELECT e.address AS addr
				FROM Employee e
				""");
	}

	@Test
	void theRest14() {

		HqlQueryParser.parseQuery("""
				SELECT AVG(o.quantity) FROM Order o
				""");
	}

	@Test
	void theRest15() {

		HqlQueryParser.parseQuery("""
				SELECT SUM(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void theRest16() {

		HqlQueryParser.parseQuery("""
				SELECT COUNT(o) FROM Order o
				""");
	}

	@Test
	void theRest17() {

		HqlQueryParser.parseQuery("""
				SELECT COUNT(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void theRest18() {

		HqlQueryParser.parseQuery("""
				SELECT COUNT(l)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John' AND l.price IS NOT NULL
				""");
	}

	@Test
	void theRest19() {

		HqlQueryParser.parseQuery("""
				SELECT o
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity DESC, o.totalcost
				""");
	}

	@Test
	void theRest20() {

		HqlQueryParser.parseQuery("""
				SELECT o.quantity, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity, a.zipcode
				""");
	}

	@Test
	void theRest21() {

		HqlQueryParser.parseQuery("""
				SELECT o.quantity, o.cost*1.08 AS taxedCost, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA' AND a.county = 'Santa Clara'
				ORDER BY o.quantity, taxedCost, a.zipcode
				""");
	}

	@Test
	void theRest22() {

		HqlQueryParser.parseQuery("""
				SELECT AVG(o.quantity) as q, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				GROUP BY a.zipcode
				ORDER BY q DESC
				""");
	}

	@Test
	void theRest23() {

		HqlQueryParser.parseQuery("""
				SELECT p.product_name
				FROM Order o JOIN o.lineItems l JOIN l.product p JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				ORDER BY p.price
				""");
	}

	/**
	 * This query is specifically dubbed illegal in the spec, but apparently works with Hibernate.
	 */
	@Test
	void theRest24() {

		HqlQueryParser.parseQuery("""
				SELECT p.product_name
				FROM Order o, IN(o.lineItems) l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				ORDER BY o.quantity
				""");
	}

	@Test
	void theRest25() {

		HqlQueryParser.parseQuery("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				""");
	}

	@Test
	void theRest26() {

		HqlQueryParser.parseQuery("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				AND c.orders IS EMPTY
				""");
	}

	@Test
	void theRest27() {

		HqlQueryParser.parseQuery("""
				UPDATE Customer c
				SET c.status = 'outstanding'
				WHERE c.balance < 10000
				""");
	}

	@Test
	void theRest28() {

		HqlQueryParser.parseQuery("""
				UPDATE Employee e
				SET e.address.building = 22
				WHERE e.address.building = 14
				AND e.address.city = 'Santa Clara'
				AND e.project = 'Jakarta EE'
				""");
	}

	@Test
	void theRest29() {

		HqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				""");
	}

	@Test
	void theRest30() {

		HqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress.state = 'CA'
				""");
	}

	@Test
	void theRest31() {

		HqlQueryParser.parseQuery("""
				SELECT DISTINCT o.shippingAddress.state
				FROM Order o
				""");
	}

	@Test
	void theRest32() {

		HqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				""");
	}

	@Test
	void theRest33() {

		HqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS NOT EMPTY
				""");
	}

	@Test
	void theRest34() {

		HqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS EMPTY
				""");
	}

	@Test
	void theRest35() {

		HqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.shipped = FALSE
				""");
	}

	@Test
	void theRest36() {

		HqlQueryParser.parseQuery("""
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

		HqlQueryParser.parseQuery("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress <> o.billingAddress
				""");
	}

	@Test
	void theRest38() {

		HqlQueryParser.parseQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.product.name = ?1
				""");
	}

	@Test
	void hqlQueries() {

		HqlQueryParser.parseQuery("from Person");
		HqlQueryParser.parseQuery("select local datetime");
		HqlQueryParser.parseQuery("from Person p select p.name");
		HqlQueryParser.parseQuery("update Person set nickName = 'Nacho' " + //
				"where name = 'Ignacio'");
		HqlQueryParser.parseQuery("update Person p " + //
				"set p.name = :newName " + //
				"where p.name = :oldName");
		HqlQueryParser.parseQuery("update Person " + //
				"set name = :newName " + //
				"where name = :oldName");
		HqlQueryParser.parseQuery("update versioned Person " + //
				"set name = :newName " + //
				"where name = :oldName");
		HqlQueryParser.parseQuery("insert Person (id, name) " + //
				"values (100L, 'Jane Doe')");
		HqlQueryParser.parseQuery("insert Person (id, name) " + //
				"values (101L, 'J A Doe III'), " + //
				"(102L, 'J X Doe'), " + //
				"(103L, 'John Doe, Jr')");
		HqlQueryParser.parseQuery("insert into Partner (id, name) " + //
				"select p.id, p.name " + //
				"from Person p ");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.name like 'Joe'");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.name like 'Joe''s'");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.id = 1");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.id = 1L");
		HqlQueryParser.parseQuery("select c " + //
				"from Call c " + //
				"where c.duration > 100.5");
		HqlQueryParser.parseQuery("select c " + //
				"from Call c " + //
				"where c.duration > 100.5F");
		HqlQueryParser.parseQuery("select c " + //
				"from Call c " + //
				"where c.duration > 1e+2");
		HqlQueryParser.parseQuery("select c " + //
				"from Call c " + //
				"where c.duration > 1e+2F");
		HqlQueryParser.parseQuery("from Phone ph " + //
				"where ph.type = LAND_LINE");
		HqlQueryParser.parseQuery("select java.lang.Math.PI");
		HqlQueryParser.parseQuery("select 'Customer ' || p.name " + //
				"from Person p " + //
				"where p.id = 1");
		HqlQueryParser.parseQuery("select sum(ch.duration) * :multiplier " + //
				"from Person pr " + //
				"join pr.phones ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = 1L ");
		HqlQueryParser.parseQuery("select year(local date) - year(p.createdOn) " + //
				"from Person p " + //
				"where p.id = 1L");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where year(local date) - year(p.createdOn) > 1");
		HqlQueryParser.parseQuery("select " + //
				"	case p.nickName " + //
				"	when 'NA' " + //
				"	then '<no nick name>' " + //
				"	else p.nickName " + //
				"	end " + //
				"from Person p");
		HqlQueryParser.parseQuery("select " + //
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
		HqlQueryParser.parseQuery("select " + //
				"	case when p.nickName is null " + //
				"		 then p.id * 1000 " + //
				"		 else p.id " + //
				"	end " + //
				"from Person p " + //
				"order by p.id");
		HqlQueryParser.parseQuery("select p " + //
				"from Payment p " + //
				"where type(p) = CreditCardPayment");
		HqlQueryParser.parseQuery("select p " + //
				"from Payment p " + //
				"where type(p) = :type");
		HqlQueryParser.parseQuery("select p " + //
				"from Payment p " + //
				"where length(treat(p as CreditCardPayment).cardNumber) between 16 and 20");
		HqlQueryParser.parseQuery("select nullif(p.nickName, p.name) " + //
				"from Person p");
		HqlQueryParser.parseQuery("select " + //
				"	case" + //
				"	when p.nickName = p.name" + //
				"	then null" + //
				"	else p.nickName" + //
				"	end " + //
				"from Person p");
		HqlQueryParser.parseQuery("select coalesce(p.nickName, '<no nick name>') " + //
				"from Person p");
		HqlQueryParser.parseQuery("select coalesce(p.nickName, p.name, '<no nick name>') " + //
				"from Person p");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where size(p.phones) >= 2");
		HqlQueryParser.parseQuery("select concat(p.number, ' : ' , cast(c.duration as string)) " + //
				"from Call c " + //
				"join c.phone p");
		HqlQueryParser.parseQuery("select substring(p.number, 1, 2) " + //
				"from Call c " + //
				"join c.phone p");
		HqlQueryParser.parseQuery("select upper(p.name) " + //
				"from Person p ");
		HqlQueryParser.parseQuery("select lower(p.name) " + //
				"from Person p ");
		HqlQueryParser.parseQuery("select trim(p.name) " + //
				"from Person p ");
		HqlQueryParser.parseQuery("select trim(leading ' ' from p.name) " + //
				"from Person p ");
		HqlQueryParser.parseQuery("select length(p.name) " + //
				"from Person p ");
		HqlQueryParser.parseQuery("select locate('John', p.name) " + //
				"from Person p ");
		HqlQueryParser.parseQuery("select abs(c.duration) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select mod(c.duration, 10) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select sqrt(c.duration) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select cast(c.duration as String) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select str(c.timestamp) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select str(cast(duration as float) / 60, 4, 2) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select c " + //
				"from Call c " + //
				"where extract(date from c.timestamp) = local date");
		HqlQueryParser.parseQuery("select extract(year from c.timestamp) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select year(c.timestamp) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select var_samp(c.duration) as sampvar, var_pop(c.duration) as popvar " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select bit_length(c.phone.number) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select c " + //
				"from Call c " + //
				"where c.duration < 30 ");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.name like 'John%' ");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.createdOn > '1950-01-01' ");
		HqlQueryParser.parseQuery("select p " + //
				"from Phone p " + //
				"where p.type = 'MOBILE' ");
		HqlQueryParser.parseQuery("select p " + //
				"from Payment p " + //
				"where p.completed = true ");
		HqlQueryParser.parseQuery("select p " + //
				"from Payment p " + //
				"where type(p) = WireTransferPayment ");
		HqlQueryParser.parseQuery("select p " + //
				"from Payment p, Phone ph " + //
				"where p.person = ph.person ");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"join p.phones ph " + //
				"where p.id = 1L and index(ph) between 0 and 3");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.createdOn between '1999-01-01' and '2001-01-02'");
		HqlQueryParser.parseQuery("select c " + //
				"from Call c " + //
				"where c.duration between 5 and 20");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.name between 'H' and 'M'");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.nickName is not null");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.nickName is null");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.name like 'Jo%'");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.name not like 'Jo%'");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.name like 'Dr|_%' escape '|'");
		HqlQueryParser.parseQuery("select p " + //
				"from Payment p " + //
				"where type(p) in (CreditCardPayment, WireTransferPayment)");
		HqlQueryParser.parseQuery("select p " + //
				"from Phone p " + //
				"where type in ('MOBILE', 'LAND_LINE')");
		HqlQueryParser.parseQuery("select p " + //
				"from Phone p " + //
				"where type in :types");
		HqlQueryParser.parseQuery("select distinct p " + //
				"from Phone p " + //
				"where p.person.id in (" + //
				"	select py.person.id " + //
				"	from Payment py" + //
				"	where py.completed = true and py.amount > 50 " + //
				")");
		HqlQueryParser.parseQuery("select distinct p " + //
				"from Phone p " + //
				"where p.person in (" + //
				"	select py.person " + //
				"	from Payment py" + //
				"	where py.completed = true and py.amount > 50 " + //
				")");
		HqlQueryParser.parseQuery("select distinct p " + //
				"from Payment p " + //
				"where (p.amount, p.completed) in (" + //
				"	(50, true)," + //
				"	(100, true)," + //
				"	(5, false)" + //
				")");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where 1 in indices(p.phones)");
		HqlQueryParser.parseQuery("select distinct p.person " + //
				"from Phone p " + //
				"join p.calls c " + //
				"where 50 > all (" + //
				"	select duration" + //
				"	from Call" + //
				"	where phone = p " + //
				") ");
		HqlQueryParser.parseQuery("select p " + //
				"from Phone p " + //
				"where local date > all elements(p.repairTimestamps)");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where :phone = some elements(p.phones)");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where :phone member of p.phones");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where exists elements(p.phones)");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.phones is empty");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.phones is not empty");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.phones is not empty");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where 'Home address' member of p.addresses");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where 'Home address' not member of p.addresses");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p");
		HqlQueryParser.parseQuery("select p " + //
				"from org.hibernate.userguide.model.Person p");
		HqlQueryParser.parseQuery("select distinct pr, ph " + //
				"from Person pr, Phone ph " + //
				"where ph.person = pr and ph is not null");
		HqlQueryParser.parseQuery("select distinct pr1 " + //
				"from Person pr1, Person pr2 " + //
				"where pr1.id <> pr2.id " + //
				"  and pr1.address = pr2.address " + //
				"  and pr1.createdOn < pr2.createdOn");
		HqlQueryParser.parseQuery("select distinct pr, ph " + //
				"from Person pr cross join Phone ph " + //
				"where ph.person = pr and ph is not null");
		HqlQueryParser.parseQuery("select p " + //
				"from Payment p ");
		HqlQueryParser.parseQuery("select d.owner, d.payed " + //
				"from (" + //
				"  select p.person as owner, c.payment is not null as payed " + //
				"  from Call c " + //
				"  join c.phone p " + //
				"  where p.number = :phoneNumber) d");
		HqlQueryParser.parseQuery("select distinct pr " + //
				"from Person pr " + //
				"join Phone ph on ph.person = pr " + //
				"where ph.type = :phoneType");
		HqlQueryParser.parseQuery("select distinct pr " + //
				"from Person pr " + //
				"join pr.phones ph " + //
				"where ph.type = :phoneType");
		HqlQueryParser.parseQuery("select distinct pr " + //
				"from Person pr " + //
				"inner join pr.phones ph " + //
				"where ph.type = :phoneType");
		HqlQueryParser.parseQuery("select distinct pr " + //
				"from Person pr " + //
				"left join pr.phones ph " + //
				"where ph is null " + //
				"   or ph.type = :phoneType");
		HqlQueryParser.parseQuery("select distinct pr " + //
				"from Person pr " + //
				"left outer join pr.phones ph " + //
				"where ph is null " + //
				"   or ph.type = :phoneType");
		HqlQueryParser.parseQuery("select pr.name, ph.number " + //
				"from Person pr " + //
				"left join pr.phones ph with ph.type = :phoneType ");
		HqlQueryParser.parseQuery("select pr.name, ph.number " + //
				"from Person pr " + //
				"left join pr.phones ph on ph.type = :phoneType ");
		HqlQueryParser.parseQuery("select distinct pr " + //
				"from Person pr " + //
				"left join fetch pr.phones ");
		HqlQueryParser.parseQuery("select a, ccp " + //
				"from Account a " + //
				"join treat(a.payments as CreditCardPayment) ccp " + //
				"where length(ccp.cardNumber) between 16 and 20");
		HqlQueryParser.parseQuery("select c, ccp " + //
				"from Call c " + //
				"join treat(c.payment as CreditCardPayment) ccp " + //
				"where length(ccp.cardNumber) between 16 and 20");
		HqlQueryParser.parseQuery("select longest.duration " + //
				"from Phone p " + //
				"left join lateral (" + //
				"  select c.duration as duration " + //
				"  from p.calls c" + //
				"  order by c.duration desc" + //
				"  limit 1 " + //
				"  ) longest " + //
				"where p.number = :phoneNumber");
		HqlQueryParser.parseQuery("select ph " + //
				"from Phone ph " + //
				"where ph.person.address = :address ");
		HqlQueryParser.parseQuery("select ph " + //
				"from Phone ph " + //
				"join ph.person pr " + //
				"where pr.address = :address ");
		HqlQueryParser.parseQuery("select ph " + //
				"from Phone ph " + //
				"where ph.person.address = :address " + //
				"  and ph.person.createdOn > :timestamp");
		HqlQueryParser.parseQuery("select ph " + //
				"from Phone ph " + //
				"inner join ph.person pr " + //
				"where pr.address = :address " + //
				"  and pr.createdOn > :timestamp");
		HqlQueryParser.parseQuery("select ph " + //
				"from Person pr " + //
				"join pr.phones ph " + //
				"join ph.calls c " + //
				"where pr.address = :address " + //
				"  and c.duration > :duration");
		HqlQueryParser.parseQuery("select ch " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		HqlQueryParser.parseQuery("select value(ch) " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		HqlQueryParser.parseQuery("select key(ch) " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		HqlQueryParser.parseQuery("select key(ch) " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		HqlQueryParser.parseQuery("select entry(ch) " + //
				"from Phone ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id ");
		HqlQueryParser.parseQuery("select sum(ch.duration) " + //
				"from Person pr " + //
				"join pr.phones ph " + //
				"join ph.callHistory ch " + //
				"where ph.id = :id " + //
				"  and index(ph) = :phoneIndex");
		HqlQueryParser.parseQuery("select value(ph.callHistory) " + //
				"from Phone ph " + //
				"where ph.id = :id ");
		HqlQueryParser.parseQuery("select key(ph.callHistory) " + //
				"from Phone ph " + //
				"where ph.id = :id ");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.phones[0].type = LAND_LINE");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where p.addresses['HOME'] = :address");
		HqlQueryParser.parseQuery("select pr " + //
				"from Person pr " + //
				"where pr.phones[max(indices(pr.phones))].type = 'LAND_LINE'");
		HqlQueryParser.parseQuery("select p.name, p.nickName " + //
				"from Person p ");
		HqlQueryParser.parseQuery("select p.name as name, p.nickName as nickName " + //
				"from Person p ");
		HqlQueryParser.parseQuery("select new org.hibernate.userguide.hql.CallStatistics(" + //
				"	count(c), " + //
				"	sum(c.duration), " + //
				"	min(c.duration), " + //
				"	max(c.duration), " + //
				"	avg(c.duration)" + //
				")  " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select new map(" + //
				"	p.number as phoneNumber , " + //
				"	sum(c.duration) as totalDuration, " + //
				"	avg(c.duration) as averageDuration " + //
				")  " + //
				"from Call c " + //
				"join c.phone p " + //
				"group by p.number ");
		HqlQueryParser.parseQuery("select new list(" + //
				"	p.number, " + //
				"	c.duration " + //
				")  " + //
				"from Call c " + //
				"join c.phone p ");
		HqlQueryParser.parseQuery("select distinct p.lastName " + //
				"from Person p");
		HqlQueryParser.parseQuery("select " + //
				"	count(c), " + //
				"	sum(c.duration), " + //
				"	min(c.duration), " + //
				"	max(c.duration), " + //
				"	avg(c.duration)  " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select count(distinct c.phone) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select p.number, count(c) " + //
				"from Call c " + //
				"join c.phone p " + //
				"group by p.number");
		HqlQueryParser.parseQuery("select p " + //
				"from Phone p " + //
				"where max(elements(p.calls)) = :call");
		HqlQueryParser.parseQuery("select p " + //
				"from Phone p " + //
				"where min(elements(p.calls)) = :call");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"where max(indices(p.phones)) = 0");
		HqlQueryParser.parseQuery("select count(c) filter (where c.duration < 30) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select p.number, count(c) filter (where c.duration < 30) " + //
				"from Call c " + //
				"join c.phone p " + //
				"group by p.number");
		HqlQueryParser.parseQuery("select listagg(p.number, ', ') within group (order by p.type,p.number) " + //
				"from Phone p " + //
				"group by p.person");
		HqlQueryParser.parseQuery("select sum(c.duration) " + //
				"from Call c ");
		HqlQueryParser.parseQuery("select p.name, sum(c.duration) " + //
				"from Call c " + //
				"join c.phone ph " + //
				"join ph.person p " + //
				"group by p.name");
		HqlQueryParser.parseQuery("select p, sum(c.duration) " + //
				"from Call c " + //
				"join c.phone ph " + //
				"join ph.person p " + //
				"group by p");
		HqlQueryParser.parseQuery("select p.name, sum(c.duration) " + //
				"from Call c " + //
				"join c.phone ph " + //
				"join ph.person p " + //
				"group by p.name " + //
				"having sum(c.duration) > 1000");
		HqlQueryParser.parseQuery("select p.name from Person p " + //
				"union " + //
				"select p.nickName from Person p where p.nickName is not null");
		HqlQueryParser.parseQuery("select p " + //
				"from Person p " + //
				"order by p.name");
		HqlQueryParser.parseQuery("select p.name, sum(c.duration) as total " + //
				"from Call c " + //
				"join c.phone ph " + //
				"join ph.person p " + //
				"group by p.name " + //
				"order by total");
		HqlQueryParser.parseQuery("select c " + //
				"from Call c " + //
				"join c.phone p " + //
				"order by p.number " + //
				"limit 50");
		HqlQueryParser.parseQuery("select c " + //
				"from Call c " + //
				"join c.phone p " + //
				"order by p.number " + //
				"fetch first 50 rows only");
		HqlQueryParser.parseQuery("select p " + //
				"from Phone p " + //
				"join fetch p.calls " + //
				"order by p " + //
				"limit 50");
	}
}
