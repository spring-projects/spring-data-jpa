/*
 * Copyright 2018-2024 the original author or authors.
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

import java.util.List;

/**
 * A parsed and structured representation of a query providing introspection details about parameter bindings.
 * <p>
 * Structured queries can be either created from {@link EntityQuery} introspection or through
 * {@link EntityQuery#deriveCountQuery(String) count query derivation}.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @since 4.0
 * @see EntityQuery
 * @see EntityQuery#create(DeclaredQuery, QueryEnhancerSelector)
 * @see TemplatedQuery#create(String, JpaQueryMethod, JpaQueryConfiguration)
 */
interface ParametrizedQuery extends QueryProvider {

	/**
	 * @return whether the underlying query has at least one parameter.
	 */
	boolean hasParameterBindings();

	/**
	 * Returns whether the query uses JDBC style parameters, i.e. parameters denoted by a simple ? without any index or
	 * name.
	 *
	 * @return Whether the query uses JDBC style parameters.
	 * @since 2.0.6
	 */
	boolean usesJdbcStyleParameters();

	/**
	 * @return whether the underlying query has at least one named parameter.
	 */
	boolean hasNamedParameter();

	/**
	 * @return the registered {@link ParameterBinding}s.
	 */
	List<ParameterBinding> getParameterBindings();

}
