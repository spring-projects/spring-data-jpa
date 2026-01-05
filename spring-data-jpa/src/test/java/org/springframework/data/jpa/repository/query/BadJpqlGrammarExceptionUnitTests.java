/*
 * Copyright 2025-present the original author or authors.
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

/**
 * Unit tests for {@link BadJpqlGrammarException}.
 *
 * @author Mark Paluch
 */
class BadJpqlGrammarExceptionUnitTests {

	@Test // GH-3757
	void shouldContainOriginalText() {

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> JpaQueryEnhancer.HqlQueryParser
						.parseQuery("SELECT e FROM Employee e WHERE FOO(x).bar RESPECTING NULLS"))
				.withMessageContaining("no viable alternative")
				.withMessageContaining("SELECT e FROM Employee e WHERE FOO(x).bar *RESPECTING NULLS")
				.withMessageContaining("Bad HQL grammar [SELECT e FROM Employee e WHERE FOO(x).bar RESPECTING NULLS]");
	}

	@Test // GH-3757
	void shouldReportExtraneousInput() {

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> JpaQueryEnhancer.HqlQueryParser.parseQuery("select * from User group by name"))
				.withMessageContaining("extraneous input '*'")
				.withMessageContaining("Bad HQL grammar [select * from User group by name]");
	}

	@Test // GH-3757
	void shouldReportMismatchedInput() {

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> JpaQueryEnhancer.HqlQueryParser.parseQuery("SELECT AVG(m.price) AS m.avg FROM Magazine m"))
				.withMessageContaining("mismatched input '.'").withMessageContaining("expecting one of the following tokens:")
				.withMessageContaining("EXCEPT")
				.withMessageContaining("Bad HQL grammar [SELECT AVG(m.price) AS m.avg FROM Magazine m]");
	}

}
