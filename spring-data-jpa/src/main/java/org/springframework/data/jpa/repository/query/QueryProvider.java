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
 * Interface indicating an object that contains and exposes an {@code query string}. This can be either a JPQL query
 * string or a SQL query string.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 * @see DeclaredQuery#jpqlQuery(String)
 * @see DeclaredQuery#nativeQuery(String)
 */
public interface QueryProvider {

	/**
	 * Return the query string.
	 *
	 * @return the query string.
	 */
	String getQueryString();

}
