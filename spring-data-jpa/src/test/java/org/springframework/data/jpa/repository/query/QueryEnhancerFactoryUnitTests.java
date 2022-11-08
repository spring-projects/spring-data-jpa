/*
 * Copyright 2022 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.jpa.repository.sample.MyCustomQueryEnhancer;

/**
 * Unit tests for {@link QueryEnhancerFactory}.
 *
 * @author Diego Krupitza
 */
class QueryEnhancerFactoryUnitTests {

	@Test
	void createsDefaultImplementationForNonNativeQuery() {

		StringQuery query = new StringQuery("select new User(u.firstname) from User u", false);

		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query);

		assertThat(queryEnhancer) //
				.isInstanceOf(DefaultQueryEnhancer.class);
	}

	@Test
	void createsJSqlImplementationForNativeQuery() {

		StringQuery query = new StringQuery("select * from User", true);

		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query);

		assertThat(queryEnhancer) //
				.isInstanceOf(JSqlParserQueryEnhancer.class);
	}

	@Test
	void fallsBackToOtherQueryEnhancerWhenUsingHibernatePlaceHolder() {
		StringQuery query = new StringQuery("SELECT c.* FROM {h-schema}countries c", true);

		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query);

		assertThat(queryEnhancer) //
				.isNotInstanceOf(JSqlParserQueryEnhancer.class);
	}

	@ParameterizedTest
	@MethodSource("generatesCorrectQueryEnhancerUsingChoiceSource")
	void generatesCorrectQueryEnhancerUsingChoice(String stringQuery, boolean isNative,
			Class<? extends QueryEnhancer> choice, Class<? extends QueryEnhancer> expectedEnhancer) {
		QueryEnhancerChoice queryEnhancerChoice = getQueryEnhancerChoice(choice);

		StringQuery query = new StringQuery(stringQuery, isNative, queryEnhancerChoice);
		assertThat(query.getQueryEnhancer()) //
				.isNotNull() //
				.isInstanceOf(expectedEnhancer);
	}

	static Stream<Arguments> generatesCorrectQueryEnhancerUsingChoiceSource() {
		return Stream.of( //
				Arguments.of("SELECT u FROM User u", true, DefaultQueryEnhancer.class, DefaultQueryEnhancer.class), //
				Arguments.of("SELECT u FROM User u", true, JSqlParserQueryEnhancer.class, JSqlParserQueryEnhancer.class), //
				Arguments.of("SELECT u FROM User u", true, MyCustomQueryEnhancer.class, MyCustomQueryEnhancer.class), //

				Arguments.of("SELECT u FROM com.diegok.User u", false, DefaultQueryEnhancer.class, DefaultQueryEnhancer.class), //
				Arguments.of("SELECT u FROM com.diegok.User u", false, JSqlParserQueryEnhancer.class,
						JSqlParserQueryEnhancer.class), //
				Arguments.of("SELECT u FROM com.diegok.User u", false, MyCustomQueryEnhancer.class, MyCustomQueryEnhancer.class) //
		);
	}

	private static QueryEnhancerChoice getQueryEnhancerChoice(Class<? extends QueryEnhancer> choice) {
		return new QueryEnhancerChoice() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return QueryEnhancerChoice.class;
			}

			@Override
			public Class<? extends QueryEnhancer> value() {
				return choice;
			}
		};
	}

}
