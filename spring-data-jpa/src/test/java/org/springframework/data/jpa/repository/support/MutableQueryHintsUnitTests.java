/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.data.util.Pair;

/**
 * Unit tests for {@link MutableQueryHints}.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 */
class MutableQueryHintsUnitTests {

	@Test // DATAJPA-872
	void emptyQueryHint() {
		new MutableQueryHints().forEach((k, v) -> Assertions.fail("Empty SimpleQueryHints shouldn't contain a value"));
	}

	@Test // DATAJPA-872
	void queryHint() {

		MutableQueryHints hints = new MutableQueryHints();
		hints.add("key", "value");
		hints.add("key", "other value");
		hints.add("other key", "another value");

		List<Object> calls = new ArrayList<>();
		hints.forEach((k, v) -> calls.add(Pair.of(k, v)));

		assertThat(calls).containsExactlyInAnyOrder(Pair.of("key", "value"), Pair.of("key", "other value"),
				Pair.of("other key", "another value"));
	}

	@Test // DATAJPA-872
	void shouldMergeQueryHints() {

		MutableQueryHints hints = new MutableQueryHints();
		hints.add("key", "value");
		hints.add("key", "other value");
		hints.add("other key", "another value");

		MutableQueryHints additionalHints = new MutableQueryHints();
		additionalHints.add("key", "23");
		additionalHints.add("another key", "42");

		QueryHints merged = QueryHints.from(hints, additionalHints);

		List<Object> calls = new ArrayList<>();
		merged.forEach((k, v) -> calls.add(Pair.of(k, v)));

		assertThat(calls).containsExactlyInAnyOrder(Pair.of("key", "value"), Pair.of("key", "other value"),Pair.of("key", "23"),
				Pair.of("other key", "another value"), Pair.of("another key", "42"));
	}
}
