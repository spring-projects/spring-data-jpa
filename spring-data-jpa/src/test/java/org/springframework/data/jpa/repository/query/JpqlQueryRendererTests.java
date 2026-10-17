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

import org.junit.jupiter.api.Test;

/**
 * JPQL rendering tests. Shared cases live in {@link AbstractQueryRendererTests}, this class holds the queries that the
 * strict JPQL grammar must reject.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Jewoo Shin
 * @since 3.1
 */
class JpqlQueryRendererTests extends AbstractQueryRendererTests {

	@Override
	String parseWithoutChanges(String query) {

		JpaQueryEnhancer.JpqlQueryParser parser = JpaQueryEnhancer.JpqlQueryParser.parseQuery(query);

		return QueryRenderer.TokenRenderer.render(new JpqlQueryRenderer().visit(parser.getContext()));
	}

	/**
	 * The spec example uses double quotes where a string literal requires single quotes.
	 */
	@Test
	void rejectsStringLiteralInDoubleQuotes() {

		assertBadGrammar("""
				SELECT e FROM Employee e JOIN e.projects p
				WHERE TREAT(p AS LargeProject).budget > 1000
				    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
				    OR p.description LIKE "cost overrun"
				""");
	}


	/**
	 * The spec dubs this query illegal. It may fail for a different reason than the one given there.
	 */
	@Test
	void rejectsJoinAfterCollectionMemberDeclaration() {

		assertBadGrammar("""
				SELECT p.product_name
				FROM Order o, IN(o.lineItems) l JOIN o.customer c
				WHERE c.lastname = 'Smith' AND c.firstname = 'John'
				ORDER BY o.quantity
				""");
	}

}
