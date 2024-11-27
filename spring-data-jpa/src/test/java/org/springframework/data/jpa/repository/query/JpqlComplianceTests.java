/*
 * Copyright 2024-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test to verify compliance of {@link JpqlParser} with standard SQL. Other than {@link JpqlSpecificationTests} tests in
 * this class check that the parser follows a lenient approach and does not error on well known concepts like numeric
 * suffix.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class JpqlComplianceTests {

	private static String parseWithoutChanges(String query) {

		JpaQueryEnhancer.JpqlQueryParser parser = JpaQueryEnhancer.JpqlQueryParser.parseQuery(query);

		return QueryRenderer.render(new JpqlQueryRenderer().visit(parser.getContext()));
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

	@Test // GH-3496
	void lateralShouldBeAValidParameter() {

		assertQuery("select e from Employee e where e.lateral = :_lateral");
		assertQuery("select te from TestEntity te where te.lateral = :lateral");
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

}
