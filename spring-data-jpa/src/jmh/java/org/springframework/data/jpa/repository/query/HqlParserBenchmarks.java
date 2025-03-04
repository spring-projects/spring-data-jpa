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

import org.junit.platform.commons.annotation.Testable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;

import org.springframework.data.domain.Sort;

/**
 * @author Mark Paluch
 */
@Testable
@Fork(1)
@Warmup(time = 2, iterations = 3)
@Measurement(time = 2)
@Timeout(time = 2)
public class HqlParserBenchmarks {

	@State(Scope.Benchmark)
	public static class BenchmarkParameters {

		DeclaredQuery query;
		Sort sort = Sort.by("foo");
		QueryEnhancer enhancer;

		@Setup(Level.Iteration)
		public void doSetup() {

			String s = """
					SELECT e FROM Employee e JOIN e.projects p
					WHERE TREAT(p AS LargeProject).budget > 1000
					    OR TREAT(p AS SmallProject).name LIKE 'Persist%'
					    OR p.description LIKE "cost overrun"
					""";

			query = DeclaredQuery.jpqlQuery(s);
			enhancer = QueryEnhancerFactory.forQuery(query).create(query);
		}
	}

	@Benchmark
	public Object measure(BenchmarkParameters parameters) {
		return parameters.enhancer.applySorting(parameters.sort);
	}

}
