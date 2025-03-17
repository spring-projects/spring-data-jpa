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
 * <p>
 * Declared queries consist of a query string and a flag whether the query is a native (SQL) one or a JPQL query.
 * Queries can be rewritten to contain a different query string (i.e. count query derivation, sorting, projection
 * updates) while retaining their {@link #isNative() native} flag.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @author Mark Paluch
 * @since 2.0.3
 */
public interface DeclaredQuery extends QueryProvider {

	/**
	 * Creates a DeclaredQuery for a JPQL query.
	 *
	 * @param jpql the JPQL query string.
	 * @return new instance of {@link DeclaredQuery}.
	 */
	static DeclaredQuery jpqlQuery(String jpql) {
		return new DeclaredQueries.JpqlQuery(jpql);
	}

	/**
	 * Creates a DeclaredQuery for a native query.
	 *
	 * @param sql the native query string.
	 * @return new instance of {@link DeclaredQuery}.
	 */
	static DeclaredQuery nativeQuery(String sql) {
		return new DeclaredQueries.NativeQuery(sql);
	}

	/**
	 * Return whether the query is a native query of not.
	 *
	 * @return {@literal true} if native query; {@literal false} if it is a JPQL query.
	 */
	boolean isNative();

	/**
	 * Return whether the query is a JPQL query of not.
	 *
	 * @return {@literal true} if JPQL query; {@literal false} if it is a native query.
	 * @since 4.0
	 */
	default boolean isJpql() {
		return !isNative();
	}

	/**
	 * Rewrite a query string using a new query string retaining its source and {@link #isNative() native} flag.
	 *
	 * @param newQueryString the new query string.
	 * @return the rewritten {@link DeclaredQuery}.
	 * @since 4.0
	 */
	default DeclaredQuery rewrite(String newQueryString) {

		if (getQueryString().equals(newQueryString)) {
			return this;
		}

		return new DeclaredQueries.RewrittenQuery(this, newQueryString);
	}

}
