/*
 * Copyright 2018-2025 the original author or authors.
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
 * Interface defining the contract to represent a declared query.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @author Mark Paluch
 * @since 2.0.3
 */
public interface DeclaredQuery extends StructuredQuery {

	/**
	 * Creates a DeclaredQuery for a JPQL query.
	 *
	 * @param jpql the JPQL query string.
	 * @return new instance of {@link DeclaredQuery}.
	 */
	static DeclaredQuery jpqlQuery(String jpql) {
		return new JpqlQuery(jpql);
	}

	/**
	 * Creates a DeclaredQuery for a native query.
	 *
	 * @param sql the native query string.
	 * @return new instance of {@link DeclaredQuery}.
	 */
	static DeclaredQuery nativeQuery(String sql) {
		return new NativeQuery(sql);
	}

	/**
	 * Return whether the query is a native query of not.
	 *
	 * @return <code>true</code> if native query otherwise <code>false</code>
	 */
	boolean isNativeQuery();
}
