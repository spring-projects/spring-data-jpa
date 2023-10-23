/*
 * Copyright 2023 the original author or authors.
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
 * Represents a query before it's executed by the entity manager.
 *
 * @author Greg Turnquist
 */
abstract class ContextualQuery {

	/**
	 * Extract the string version of the query.
	 */
	abstract String getQuery();

	ContextualQuery alterQuery(String newQueryString) {
		return of(newQueryString);
	}

	/**
	 * Create a new {@link ContextualQuery} using a string-based query.
	 *
	 * @param query
	 * @return
	 */
	static ContextualQuery of(String query) {
		return new StringQuery(query);
	}

	/**
	 * String-based variant of a {@link ContextualQuery}.
	 */
	private static final class StringQuery extends ContextualQuery {
		private final String query;

		StringQuery(String query) {
			this.query = query;
		}

		@Override
		String getQuery() {
			return query;
		}
	}
}
