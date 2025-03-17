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

/**
 * Test-variant of {@link DefaultEntityQuery} with a simpler constructor.
 *
 * @author Mark Paluch
 */
class TestEntityQuery extends DefaultEntityQuery {

	/**
	 * Creates a new {@link DefaultEntityQuery} from the given JPQL query.
	 *
	 * @param query must not be {@literal null} or empty.
	 */
	TestEntityQuery(String query, boolean isNative) {

		super(PreprocessedQuery.parse(isNative ? DeclaredQuery.nativeQuery(query) : DeclaredQuery.jpqlQuery(query)),
				QueryEnhancerSelector.DEFAULT_SELECTOR
						.select(isNative ? DeclaredQuery.nativeQuery(query) : DeclaredQuery.jpqlQuery(query)));
	}
}
