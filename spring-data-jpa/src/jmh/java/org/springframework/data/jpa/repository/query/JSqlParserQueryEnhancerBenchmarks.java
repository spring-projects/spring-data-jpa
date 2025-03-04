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

import java.io.IOException;

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
public class JSqlParserQueryEnhancerBenchmarks {

	@State(Scope.Benchmark)
	public static class BenchmarkParameters {

		JSqlParserQueryEnhancer enhancer;
		Sort sort = Sort.by("foo");
		private byte[] serialized;

		@Setup(Level.Iteration)
		public void doSetup() throws IOException {

			String s = """
					select SOME_COLUMN from SOME_TABLE where REPORTING_DATE = :REPORTING_DATE
					except
					select SOME_COLUMN from SOME_OTHER_TABLE where REPORTING_DATE = :REPORTING_DATE
					union select SOME_COLUMN from SOME_OTHER_OTHER_TABLE""";

			enhancer = new JSqlParserQueryEnhancer(DeclaredQuery.nativeQuery(s));
		}
	}

	@Benchmark
	public Object applySortWithParsing(BenchmarkParameters p) {
		return p.enhancer.applySorting(p.sort);
	}

}
