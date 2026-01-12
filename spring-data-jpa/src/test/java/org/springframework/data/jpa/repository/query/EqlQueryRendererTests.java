/*
 * Copyright 2023-present the original author or authors.
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

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import org.springframework.data.jpa.repository.query.QueryRenderer.TokenRenderer;

/**
 * Tests built around examples of EQL found in the JPA spec
 * https://github.com/jakartaee/persistence/blob/master/spec/src/main/asciidoc/ch04-query-language.adoc<br/>
 * <br/>
 * IMPORTANT: Purely verifies the parser without any transformations.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class EqlQueryRendererTests extends JpqlQueryRendererTckTests {

	private static final String SPEC_FAULT = "Disabled due to spec fault> ";

	/**
	 * Parse the query using {@link EqlParser} then run it through the query-preserving {@link EqlQueryRenderer}.
	 */
	String parseWithoutChanges(String query) {

		JpaQueryEnhancer.EqlQueryParser parser = JpaQueryEnhancer.EqlQueryParser.parseQuery(query);

		return TokenRenderer.render(new EqlQueryRenderer().visit(parser.getContext()));
	}

	static Stream<Arguments> reservedWords() {
		return Stream.of("abs", "exp", "any", "case", "else", "index", "time").map(Arguments::of);
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
	void currentTimeLiterals() {

		assertQuery("SELECT e FROM Employee e WHERE CURRENT_DATE > CURRENT_TIME");
		assertQuery("SELECT e FROM Employee e WHERE CURRENT_TIME > CURRENT_TIMESTAMP");
		assertQuery("SELECT e.name, CURRENT_DATE FROM Employee e");
		assertQuery("SELECT e.name, CURRENT_TIME FROM Employee e");
		assertQuery("SELECT e.name, CURRENT_TIMESTAMP FROM Employee e");
	}

	@Test
	void numericCasting() {
		assertQuery("SELECT e FROM Employee e WHERE CAST(e.salary NUMERIC(10, 2)) > 0.0");
	}

	@Test
	void betweenDates() {
		assertQuery("SELECT e FROM Entity e WHERE e.embeddedId.date BETWEEN CURRENT_DATE AND CURRENT_TIME");
	}

	@Test
	void joinFetch() {

		assertQuery("SELECT e FROM Employee e JOIN FETCH e.address");
		assertQuery("SELECT e FROM Employee e JOIN FETCH e.address a ORDER BY a.city");
		assertQuery("SELECT e FROM Employee e JOIN FETCH e.address AS a ORDER BY a.city");
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
}
