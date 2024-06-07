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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.query.QueryRenderer.TokenRenderer;

/**
 * Tests built around examples of EQL found in the EclipseLink's docs at
 * https://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Basic_JPA_Development/Querying/JPQL<br/>
 * With the exception of {@literal MOD} which is defined as {@literal MOD(arithmetic_expression , arithmetic_expression)},
 * but shown in tests as {@literal MOD(arithmetic_expression ? arithmetic_expression)}.
 * <br/>
 * IMPORTANT: Purely verifies the parser without any transformations.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 */
class EqlComplianceTests {

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
	}

	@Test
	void leftJoin() {
		assertQuery("SELECT e FROM Employee e LEFT JOIN e.address a ORDER BY a.city");
	}

	@Test
	void on() {

		assertQuery("SELECT e FROM Employee e LEFT JOIN e.address ON a.city = :city");
		assertQuery("SELECT e FROM Employee e LEFT JOIN MailingAddress a ON e.address = a.address");
	}

	@Test
	void subselectsInFromClause() {
		assertQuery(
				"SELECT e, c.city FROM Employee e, (SELECT DISTINCT a.city FROM Address a) c WHERE e.address.city = c.city");
	}

	@Test
	void orderByClause() {

		assertQuery("SELECT e FROM Employee e ORDER BY e.lastName ASC, e.firstName ASC"); // Typo in EQL document
		assertQuery("SELECT e FROM Employee e ORDER BY UPPER(e.lastName)");
		assertQuery("SELECT e FROM Employee e LEFT JOIN e.manager m ORDER BY m.lastName NULLS FIRST");
		assertQuery("SELECT e FROM Employee e ORDER BY e.address");
	}

	@Test
	void groupByClause() {

		assertQuery("SELECT AVG(e.salary), e.address.city FROM Employee e GROUP BY e.address.city");
		assertQuery("SELECT AVG(e.salary), e.address.city FROM Employee e GROUP BY e.address.city ORDER BY AVG(e.salary)");
		assertQuery("SELECT e, COUNT(p) FROM Employee e LEFT JOIN e.projects p GROUP BY e");
	}

	@Test
	void havingClause() {
		assertQuery(
				"SELECT AVG(e.salary), e.address.city FROM Employee e GROUP BY e.address.city HAVING AVG(e.salary) > 100000");
	}

	@Test
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
		assertQuery("SELECT e FROM Employee e WHERE e.lastName REGEXP '^Dr.*'");
		assertQuery("SELECT e FROM Employee e WHERE e.lastName REGEXP '^Dr\\.*'");
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
		assertQuery("SELECT e FROM Employee e WHERE NULLIF(e.salary, 0) is null");
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
	void functionsInOrderBy() {

		assertQuery("SELECT e FROM Employee e ORDER BY e.salary - 1000");
		assertQuery("SELECT e FROM Employee e ORDER BY e.salary + 1000");
		assertQuery("SELECT e FROM Employee e ORDER BY e.salary * 2");
		assertQuery("SELECT e FROM Employee e ORDER BY e.salary * 2.0");
		assertQuery("SELECT e FROM Employee e ORDER BY e.salary / 2");
		assertQuery("SELECT e FROM Employee e ORDER BY e.salary / 2.0");
		assertQuery("SELECT e FROM Employee e ORDER BY ABS(e.salary - e.manager.salary)");
		assertQuery("SELECT e FROM Employee e ORDER BY COALESCE(e.salary, 0)");
		assertQuery("SELECT e FROM Employee e ORDER BY CONCAT(e.firstName, ' ', e.lastName)");
		assertQuery("SELECT e FROM Employee e ORDER BY CURRENT_DATE");
		assertQuery("SELECT e FROM Employee e ORDER BY CURRENT_TIME");
		assertQuery("SELECT e FROM Employee e ORDER BY CURRENT_TIMESTAMP");
		assertQuery("SELECT e FROM Employee e ORDER BY LENGTH(e.lastName)");
		assertQuery("SELECT e FROM Employee e ORDER BY LOWER(e.lastName)");
		assertQuery("SELECT e FROM Employee e ORDER BY MOD(e.hoursWorked, 8)");
		assertQuery("SELECT e FROM Employee e ORDER BY NULLIF(e.salary, 0)");
		assertQuery("SELECT e FROM Employee e ORDER BY SQRT(o.RESULT)");
		assertQuery("SELECT e FROM Employee e ORDER BY SUBSTRING(e.lastName, 0, 2)");
		assertQuery("SELECT e FROM Employee e ORDER BY TRIM(TRAILING FROM e.lastName)");
		assertQuery("SELECT e FROM Employee e ORDER BY TRIM(e.lastName)");
		assertQuery("SELECT e FROM Employee e ORDER BY TRIM(LEADING '-' FROM e.lastName)");
		assertQuery("SELECT e FROM Employee e ORDER BY UPPER(e.lastName)");
		assertQuery("SELECT e FROM Employee e ORDER BY CAST(e.salary NUMERIC(10, 2))");
		assertQuery("SELECT e FROM Employee e ORDER BY EXTRACT(YEAR FROM e.startDate)");
	}

	@Test
	void functionsInGroupBy() {

		assertQuery("SELECT e FROM Employee e GROUP BY e.salary - 1000");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary + 1000");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary * 2");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary * 2.0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary / 2");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary / 2.0");
		assertQuery("SELECT e FROM Employee e GROUP BY ABS(e.salary - e.manager.salary)");
		assertQuery("SELECT e FROM Employee e GROUP BY COALESCE(e.salary, 0)");
		assertQuery("SELECT e FROM Employee e GROUP BY CONCAT(e.firstName, ' ', e.lastName)");
		assertQuery("SELECT e FROM Employee e GROUP BY CURRENT_DATE");
		assertQuery("SELECT e FROM Employee e GROUP BY CURRENT_TIME");
		assertQuery("SELECT e FROM Employee e GROUP BY CURRENT_TIMESTAMP");
		assertQuery("SELECT e FROM Employee e GROUP BY LENGTH(e.lastName)");
		assertQuery("SELECT e FROM Employee e GROUP BY LOWER(e.lastName)");
		assertQuery("SELECT e FROM Employee e GROUP BY MOD(e.hoursWorked, 8)");
		assertQuery("SELECT e FROM Employee e GROUP BY NULLIF(e.salary, 0)");
		assertQuery("SELECT e FROM Employee e GROUP BY SQRT(o.RESULT)");
		assertQuery("SELECT e FROM Employee e GROUP BY SUBSTRING(e.lastName, 0, 2)");
		assertQuery("SELECT e FROM Employee e GROUP BY TRIM(TRAILING FROM e.lastName)");
		assertQuery("SELECT e FROM Employee e GROUP BY TRIM(e.lastName)");
		assertQuery("SELECT e FROM Employee e GROUP BY TRIM(LEADING '-' FROM e.lastName)");
		assertQuery("SELECT e FROM Employee e GROUP BY UPPER(e.lastName)");
		assertQuery("SELECT e FROM Employee e GROUP BY CAST(e.salary NUMERIC(10, 2))");
		assertQuery("SELECT e FROM Employee e GROUP BY EXTRACT(YEAR FROM e.startDate)");
	}

	@Test
	void functionsInHaving() {

		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING e.salary - 1000 > 0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING e.salary + 1000 > 0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING e.salary * 2 > 0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING e.salary * 2.0 > 0.0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING e.salary / 2 > 0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING e.salary / 2.0 > 0.0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING ABS(e.salary - e.manager.salary) > 0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING COALESCE(e.salary, 0) > 0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING CONCAT(e.firstName, ' ', e.lastName) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING CURRENT_DATE > CURRENT_TIME");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING CURRENT_TIME > CURRENT_TIMESTAMP");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING LENGTH(e.lastName) > 0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING LOWER(e.lastName) = 'bilbo'");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING MOD(e.hoursWorked, 8) > 0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING NULLIF(e.salary, 0) is null");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING SQRT(o.RESULT) > 0.0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING SUBSTRING(e.lastName, 0, 2) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING TRIM(TRAILING FROM e.lastName) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING TRIM(e.lastName) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING TRIM(LEADING '-' FROM e.lastName) = 'Bilbo'");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING UPPER(e.lastName) = 'BILBO'");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING CAST(e.salary NUMERIC(10, 2)) > 0.0");
		assertQuery("SELECT e FROM Employee e GROUP BY e.salary HAVING EXTRACT(YEAR FROM e.startDate) = '2023'");
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

	@Test
	void eclipseLinkSpecialOperators() {

		assertQuery("SELECT p FROM Phone p WHERE FUNC('TO_NUMBER', e.areaCode) > 613");
		assertQuery("SELECT FUNC('YEAR', e.startDate) AS YEAR, COUNT(e) FROM Employee e GROUP BY YEAR");
		assertQuery(
				"SELECT a FROM Asset a, Geography geo WHERE geo.id = :id AND a.id IN :id_list AND FUNC('ST_INTERSECTS', a.geometry, geo.geometry) = 'TRUE'");
		assertQuery(
				"SELECT s FROM SimpleSpatial s WHERE FUNC('MDSYS.SDO_RELATE', s.jGeometry, :otherGeometry, :params) = 'TRUE' ORDER BY s.id ASC");
		assertQuery("SELECT e FROM Employee e WHERE OPERATOR('ExtractXml', e.resume, '@years-experience') > 10");
	}

	@Test
	void sql() {

		assertQuery("SELECT p FROM Phone p WHERE SQL('CAST(? AS CHAR(3))', e.areaCode) = '613'");
		assertQuery("SELECT SQL('EXTRACT(YEAR FROM ?)', e.startDate) AS YEAR, COUNT(e) FROM Employee e GROUP BY YEAR");
		assertQuery("SELECT e FROM Employee e ORDER BY SQL('? NULLS FIRST', e.startDate)");
		assertQuery("SELECT e FROM Employee e WHERE e.startDate = SQL('(SELECT SYSDATE FROM DUAL)')");
	}

	@Test
	void column() {

		assertQuery("SELECT e FROM Employee e WHERE COLUMN('MANAGER_ID', e) = :id");
		assertQuery("SELECT e FROM Employee e WHERE COLUMN('ROWID', e) = :id");
	}

	@Test
	void table() {
		assertQuery(
				"SELECT e, a.LAST_UPDATE_USER FROM Employee e, TABLE('AUDIT') a WHERE a.TABLE = 'EMPLOYEE' AND a.ROWID = COLUMN('ROWID', e)");
	}

	@Test // GH-3175
	void coalesceFunctions() {

		assertQuery("SELECT b FROM Bundle b WHERE coalesce(b.deleted, false) AND b.latestImport = true");
		assertQuery("SELECT b FROM Bundle b WHERE NOT coalesce(b.deleted, false) AND b.latestImport = true");
	}

	@Test // GH-3314
	void isNullAndIsNotNull() {

		assertQuery("SELECT e FROM Employee e WHERE (e.active = null OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active = NULL OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active IS null OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active IS NULL OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active != null OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active != NULL OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active IS NOT null OR e.active = true)");
		assertQuery("SELECT e FROM Employee e WHERE (e.active IS NOT NULL OR e.active = true)");
	}
}
