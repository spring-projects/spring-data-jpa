package org.springframework.data.jpa.repository.support;/*
																												* Copyright 2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.data.util.Pair;

public class SimpleQueryHintsUnitTests {

	@Test
	public void emptyQueryHint() {
		new SimpleQueryHints().forEach((k, v) -> Assertions.fail("Empty SimpleQueryHints shouldn't contain a value"));
	}

	@Test
	public void queryHint() {

		SimpleQueryHints hints = new SimpleQueryHints();
		hints.add("key", "value");
		hints.add("key", "other value");
		hints.add("other key", "another value");

		List calls = new ArrayList();
		hints.forEach((k, v) -> calls.add(Pair.of(k, v)));

		assertThat(calls).containsExactlyInAnyOrder(Pair.of("key", "value"), Pair.of("key", "other value"),
				Pair.of("other key", "another value"));
	}

	@Test
	public void addingQueryHints() {

		SimpleQueryHints hints = new SimpleQueryHints();
		hints.add("key", "value");
		hints.add("key", "other value");
		hints.add("other key", "another value");

		SimpleQueryHints additionalHints = new SimpleQueryHints();
		additionalHints.add("key", "23");
		additionalHints.add("another key", "42");

		hints.addAll(additionalHints);

		List calls = new ArrayList();
		hints.forEach((k, v) -> calls.add(Pair.of(k, v)));

		assertThat(calls).containsExactlyInAnyOrder(Pair.of("key", "value"), Pair.of("key", "other value"),Pair.of("key", "23"),
				Pair.of("other key", "another value"), Pair.of("another key", "42"));
	}

}
