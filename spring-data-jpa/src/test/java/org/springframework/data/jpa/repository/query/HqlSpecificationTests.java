/*
 * Copyright 2022-2024 the original author or authors.
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
import static org.springframework.data.jpa.repository.query.JpaQueryParsingToken.*;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
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

	private static String parseWithoutChanges(String query) {

		HqlLexer lexer = new HqlLexer(CharStreams.fromString(query));
		HqlParser parser = new HqlParser(new CommonTokenStream(lexer));

		parser.addErrorListener(new BadJpqlGrammarErrorListener(query));

		HqlParser.StartContext parsedQuery = parser.start();

		return render(new HqlQueryRenderer().visit(parsedQuery));
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
				SELECT OBJECT(c) FROM Customer c , IN(c.orders) o WHERE c.status = 1
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
				FROM Order o , IN(o.lineItems) l
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
	void theRest() {

		assertQuery("""
				SELECT e
				 FROM Employee e
				 WHERE TYPE(e) IN (Exempt, Contractor)
				""");
	}

	@Test
	void theRest2() {

		assertQuery("""
				SELECT e
				    FROM Employee e
				    WHERE TYPE(e) IN (:empType1, :empType2)
				""");
	}

	@Test
	void theRest3() {

		assertQuery("""
				SELECT e
				FROM Employee e
				WHERE TYPE(e) IN :empTypes
				""");
	}

	@Test
	void theRest4() {

		assertQuery("""
				SELECT TYPE(e)
				FROM Employee e
				WHERE TYPE(e) <> Exempt
				""");
	}

	@Test
	void theRest5() {

		assertQuery("""
				SELECT c.status, AVG(c.filledOrderCount), COUNT(c)
				FROM Customer c
				GROUP BY c.status
				HAVING c.status IN (1, 2)
				""");
	}

	@Test
	void theRest6() {

		assertQuery("""
				SELECT c.country, COUNT(c)
				FROM Customer c
				GROUP BY c.country
				HAVING COUNT(c) > 30
				""");
	}

	@Test
	void theRest7() {

		assertQuery("""
				SELECT c, COUNT(o)
				FROM Customer c JOIN c.orders o
				GROUP BY c
				HAVING COUNT(o) >= 5
				""");
	}

	@Test
	void theRest8() {

		assertQuery("""
				SELECT c.id, c.status
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void theRest9() {

		assertQuery("""
				SELECT v.location.street, KEY(i).title, VALUE(i)
				FROM VideoStore v JOIN v.videoInventory i
				WHERE v.location.zipcode = '94301' AND VALUE(i) > 0
				""");
	}

	@Test
	void theRest10() {

		assertQuery("""
				SELECT o.lineItems FROM Order AS o
				""");
	}

	@Test
	void theRest11() {

		assertQuery("""
				SELECT c, COUNT(l) AS itemCount
				FROM Customer c JOIN c.Orders o JOIN o.lineItems l
				WHERE c.address.state = 'CA'
				GROUP BY c
				ORDER BY itemCount
				""");
	}

	@Test
	void theRest12() {

		assertQuery("""
				SELECT NEW com.acme.example.CustomerDetails(c.id, c.status, o.count)
				FROM Customer c JOIN c.orders o
				WHERE o.count > 100
				""");
	}

	@Test
	void theRest13() {

		assertQuery("""
				SELECT e.address AS addr
				FROM Employee e
				""");
	}

	@Test
	void theRest14() {

		assertQuery("""
				SELECT AVG(o.quantity) FROM Order o
				""");
	}

	@Test
	void theRest15() {

		assertQuery("""
				SELECT SUM(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void theRest16() {

		assertQuery("""
				SELECT COUNT(o) FROM Order o
				""");
	}

	@Test
	void theRest17() {

		assertQuery("""
				SELECT COUNT(l.price)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				""");
	}

	@Test
	void theRest18() {

		assertQuery("""
				SELECT COUNT(l)
				FROM Order o JOIN o.lineItems l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John' AND l.price IS NOT NULL
				""");
	}

	@Test
	void theRest19() {

		assertQuery("""
				SELECT o
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity DESC, o.totalcost
				""");
	}

	@Test
	void theRest20() {

		assertQuery("""
				SELECT o.quantity, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				ORDER BY o.quantity, a.zipcode
				""");
	}

	@Test
	void theRest21() {

		assertQuery("""
				SELECT o.quantity, o.cost * 1.08 AS taxedCost, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA' AND a.county = 'Santa Clara'
				ORDER BY o.quantity, taxedCost, a.zipcode
				""");
	}

	@Test
	void theRest22() {

		assertQuery("""
				SELECT AVG(o.quantity) as q, a.zipcode
				FROM Customer c JOIN c.orders o JOIN c.address a
				WHERE a.state = 'CA'
				GROUP BY a.zipcode
				ORDER BY q DESC
				""");
	}

	@Test
	void theRest23() {

		assertQuery("""
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

		assertQuery("""
				SELECT p.product_name
				FROM Order o , IN(o.lineItems) l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				ORDER BY o.quantity
				""");
	}

	@Test
	void theRest25() {

		assertQuery("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				""");
	}

	@Test
	void theRest26() {

		assertQuery("""
				DELETE
				FROM Customer c
				WHERE c.status = 'inactive'
				AND c.orders IS EMPTY
				""");
	}

	@Test
	void theRest27() {

		assertQuery("""
				UPDATE Customer c
				SET c.status = 'outstanding'
				WHERE c.balance < 10000
				""");
	}

	@Test
	void theRest28() {

		assertQuery("""
				UPDATE Employee e
				SET e.address.building = 22
				WHERE e.address.building = 14
				AND e.address.city = 'Santa Clara'
				AND e.project = 'Jakarta EE'
				""");
	}

	@Test
	void theRest29() {

		assertQuery("""
				SELECT o
				FROM Order o
				""");
	}

	@Test
	void theRest30() {

		assertQuery("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress.state = 'CA'
				""");
	}

	@Test
	void theRest31() {

		assertQuery("""
				SELECT DISTINCT o.shippingAddress.state
				FROM Order o
				""");
	}

	@Test
	void theRest32() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				""");
	}

	@Test
	void theRest33() {

		assertQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS NOT EMPTY
				""");
	}

	@Test
	void theRest34() {

		assertQuery("""
				SELECT o
				FROM Order o
				WHERE o.lineItems IS EMPTY
				""");
	}

	@Test
	void theRest35() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.shipped = FALSE
				""");
	}

	@Test
	void theRest36() {

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
	void theRest37() {

		assertQuery("""
				SELECT o
				FROM Order o
				WHERE o.shippingAddress <> o.billingAddress
				""");
	}

	@Test
	void theRest38() {

		assertQuery("""
				SELECT DISTINCT o
				FROM Order o JOIN o.lineItems l
				WHERE l.product.name = ?1
				""");
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
				"where p.person.id in (select py.person.id " + //
				"	from Payment py" + //
				"	where py.completed = true and py.amount > 50)");
		assertQuery("select distinct p " + //
				"from Phone p " + //
				"where p.person in (select py.person " + //
				"	from Payment py" + //
				"	where py.completed = true and py.amount > 50)");
		assertQuery("select distinct p " + //
				"from Payment p " + //
				"where (p.amount, p.completed) in ((50, true)," + //
				"	(100, true)," + //
				"	(5, false))");
		assertQuery("select p " + //
				"from Person p " + //
				"where 1 in indices (p.phones)");
		assertQuery("select distinct p.person " + //
				"from Phone p " + //
				"join p.calls c " + //
				"where 50 > all (select duration" + //
				"	from Call" + //
				"	where phone = p) ");
		assertQuery("select p " + //
				"from Phone p " + //
				"where local date > all elements (p.repairTimestamps)");
		assertQuery("select p " + //
				"from Person p " + //
				"where :phone = some elements (p.phones)");
		assertQuery("select p " + //
				"from Person p " + //
				"where :phone member of p.phones");
		assertQuery("select p " + //
				"from Person p " + //
				"where exists elements (p.phones)");
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
				"from (select p.person as owner, c.payment is not null as payed " + //
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
				"   or ph.type = :phoneType");
		assertQuery("select distinct pr " + //
				"from Person pr " + //
				"left outer join pr.phones ph " + //
				"where ph is null " + //
				"   or ph.type = :phoneType");
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
				"left join lateral (" + //
				"  select c.duration as duration " + //
				"  from p.calls c" + //
				"  order by c.duration desc" + //
				"  limit 1 " + //
				"  ) longest " + //
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
		assertQuery("select new org.hibernate.userguide.hql.CallStatistics(count(c), " + //
				"	sum(c.duration), " + //
				"	min(c.duration), " + //
				"	max(c.duration), " + //
				"	avg(c.duration)" + //
				")  " + //
				"from Call c ");
		assertQuery("select new map(p.number as phoneNumber, " + //
				"	sum(c.duration) as totalDuration, " + //
				"	avg(c.duration) as averageDuration)  " + //
				"from Call c " + //
				"join c.phone p " + //
				"group by p.number ");
		assertQuery("select new list(p.number," + //
				"	c.duration)  " + //
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
		assertQuery("select p " + //
				"from Phone p " + //
				"join fetch p.calls " + //
				"order by p " + //
				"limit 50");
	}
}
