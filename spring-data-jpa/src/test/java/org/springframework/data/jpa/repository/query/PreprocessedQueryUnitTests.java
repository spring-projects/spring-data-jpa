/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link PreprocessedQuery}.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @author Mark Paluch
 */
class PreprocessedQueryUnitTests {

	@ParameterizedTest // DATAJPA-1200
	@MethodSource("parameters")
	void identificationOfParameters(String jpql, boolean containsParameter) {

		DefaultEntityQuery stringQuery = new TestEntityQuery(jpql, false);

		if (containsParameter) {
			assertThat(stringQuery.getParameterBindings()).isNotEmpty();
		} else {
			assertThat(stringQuery.getParameterBindings()).isEmpty();
		}
	}

	static Stream<Arguments.ArgumentSet> parameters() {

		return Stream.of(Arguments.argumentSet("named parameter", "select u from User u where u.name = :name", true),
				Arguments.argumentSet("indexed parameter", "select u from User u where u.age > ?1", true),
				Arguments.argumentSet("no parameter", "select u from User u where u.email = u.email", false));
	}

}
