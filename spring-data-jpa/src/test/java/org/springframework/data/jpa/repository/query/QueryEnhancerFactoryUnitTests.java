/*
 * Copyright 2022-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.jpa.repository.query.QueryEnhancerFactory.NativeQueryEnhancer;
import org.springframework.data.jpa.util.ClassPathExclusions;
import org.springframework.lang.Nullable;

/**
 * Unit tests for {@link QueryEnhancerFactory}.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Christoph Strobl
 */
class QueryEnhancerFactoryUnitTests {

	@Test
	void createsParsingImplementationForNonNativeQuery() {

		StringQuery query = new StringQuery("select new com.example.User(u.firstname) from User u", false);

		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query);

		assertThat(queryEnhancer) //
				.isInstanceOf(JpaQueryEnhancer.class);

		JpaQueryEnhancer queryParsingEnhancer = (JpaQueryEnhancer) queryEnhancer;

		assertThat(queryParsingEnhancer).isInstanceOf(JpaQueryEnhancer.HqlQueryParser.class);
	}

	@Test
	void createsJSqlImplementationForNativeQuery() {

		StringQuery query = new StringQuery("select * from User", true);

		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query);

		assertThat(queryEnhancer) //
				.isInstanceOf(JSqlParserQueryEnhancer.class);
	}

	@ParameterizedTest // GH-2989
	@MethodSource("nativeEnhancerSelectionArgs")
	void createsNativeImplementationAccordingToUserChoice(@Nullable String selection, NativeQueryEnhancer enhancer) {

		withSystemProperty(NativeQueryEnhancer.NATIVE_PARSER_PROPERTY, selection, () -> {
			assertThat(NativeQueryEnhancer.select(this.getClass().getClassLoader())).isEqualTo(enhancer);
		});
	}

	@Test // GH-2989
	@ClassPathExclusions(packages = { "net.sf.jsqlparser.parser" })
	void selectedDefaultImplementationIfJsqlNotAvailable() {

		assertThat(assertThat(NativeQueryEnhancer.select(this.getClass().getClassLoader()))
				.isEqualTo(NativeQueryEnhancer.DEFAULT));
	}

	@Test // GH-2989
	@ClassPathExclusions(packages = { "net.sf.jsqlparser.parser" })
	void selectedDefaultImplementationIfJsqlNotAvailableEvenIfExplicitlyStated/* or should we raise an error? */() {

		withSystemProperty(NativeQueryEnhancer.NATIVE_PARSER_PROPERTY, "jsql", () -> {
			assertThat(NativeQueryEnhancer.select(this.getClass().getClassLoader())).isEqualTo(NativeQueryEnhancer.DEFAULT);
		});
	}

	void withSystemProperty(String property, @Nullable String value, Runnable exeution) {

		String currentValue = System.getProperty(property);
		if (value != null) {
			System.setProperty(property, value);
		} else {
			System.clearProperty(property);
		}
		try {
			exeution.run();
		} finally {
			if (currentValue != null) {
				System.setProperty(property, currentValue);
			} else {
				System.clearProperty(property);
			}
		}

	}

	static Stream<Arguments> nativeEnhancerSelectionArgs() {
		return Stream.of(Arguments.of(null, NativeQueryEnhancer.JSQL), Arguments.of("", NativeQueryEnhancer.JSQL),
				Arguments.of("auto", NativeQueryEnhancer.JSQL), Arguments.of("default", NativeQueryEnhancer.DEFAULT),
				Arguments.of("jsql", NativeQueryEnhancer.JSQL));
	}
}
