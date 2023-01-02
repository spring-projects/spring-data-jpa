/*
 * Copyright 2022-2023 the original author or authors.
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
}
