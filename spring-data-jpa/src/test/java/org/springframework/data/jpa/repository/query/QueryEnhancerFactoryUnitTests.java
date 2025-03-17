/*
 * Copyright 2022-2025 the original author or authors.
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
 * Unit tests for {@link QueryEnhancerFactory}.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class QueryEnhancerFactoryUnitTests {

	@Test
	void createsParsingImplementationForNonNativeQuery() {

		DefaultEntityQuery query = new TestEntityQuery("select new com.example.User(u.firstname) from User u",
				false);

		QueryEnhancer queryEnhancer = QueryEnhancer.create(query);

		assertThat(queryEnhancer) //
				.isInstanceOf(JpaQueryEnhancer.class);

		JpaQueryEnhancer queryParsingEnhancer = (JpaQueryEnhancer) queryEnhancer;

		assertThat(queryParsingEnhancer).isInstanceOf(JpaQueryEnhancer.HqlQueryParser.class);
	}

	@Test
	void createsJSqlImplementationForNativeQuery() {

		DefaultEntityQuery query = new TestEntityQuery("select * from User", true);

		QueryEnhancer queryEnhancer = QueryEnhancerFactory.forQuery(query).create(query);

		assertThat(queryEnhancer) //
				.isInstanceOf(JSqlParserQueryEnhancer.class);
	}

}
