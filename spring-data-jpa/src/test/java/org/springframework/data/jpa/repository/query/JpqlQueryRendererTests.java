/*
 * Copyright 2022-present the original author or authors.
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
import org.junit.jupiter.params.provider.Arguments;

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
class JpqlQueryRendererTests extends JpqlQueryRendererTckTests {

	private static final String SPEC_FAULT = "Disabled due to spec fault> ";

	static Stream<Arguments> reservedWords() {
		return Stream.of("abs", "exp", "any", "case", "else", "index", "time").map(Arguments::of);
	}

	/**
	 * Parse the query using {@link JpqlParser} then run it through the query-preserving {@link JpqlQueryRenderer}.
	 */
	@Override
	String parseWithoutChanges(String query) {

		JpaQueryEnhancer.JpqlQueryParser parser = JpaQueryEnhancer.JpqlQueryParser.parseQuery(query);

		return TokenRenderer.render(new JpqlQueryRenderer().visit(parser.getContext()));
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
}
