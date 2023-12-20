/*
 * Copyright 2023 the original author or authors.
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

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

/**
 * @author Christoph Strobl
 */
public abstract class SqlParserTests {

	abstract <T extends ParseTree> T parse(String query);

	abstract <T extends ParseTree> List<JpaQueryParsingToken> analyze(T parseTree);

	protected String render(List<JpaQueryParsingToken> tokens) {

		StringBuilder results = new StringBuilder();

		tokens.forEach(token -> {

			results.append(token.getToken());

			if (token.getSpace()) {
				results.append(" ");
			}
		});

		return results.toString().trim();
	}

	@Test // GH-3277
	void numericLiterals() {

		assertQuery("SELECT e FROM  Employee e WHERE e.id = 1234");
		assertQuery("SELECT e FROM  Employee e WHERE e.id = 1234L");
		assertQuery("SELECT s FROM  Stat s WHERE s.ratio > 3.14");
		assertQuery("SELECT s FROM  Stat s WHERE s.ratio > 3.14F");
		assertQuery("SELECT s FROM  Stat s WHERE s.ratio > 3.14e32D");
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
	void leftJoin() {
		assertQuery("SELECT e FROM Employee e LEFT JOIN e.address a ORDER BY a.city");
	}

	protected void assertQuery(String query) {

		String slimmedDownQuery = reduceWhitespace(query);
		ParseTree tree = parse(slimmedDownQuery);
		List<JpaQueryParsingToken> tokens = analyze(tree);

		assertThat(render(tokens)).isEqualTo(slimmedDownQuery);
	}

	private static String reduceWhitespace(String original) {

		return original //
				.replaceAll("[ \\t\\n]{1,}", " ") //
				.trim();
	}
}
