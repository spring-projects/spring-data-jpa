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
 * A wrapper for a String representation of a query offering information about the query.
 *
 * @author Jens Schauder
 * @author Diego Krupitza
 * @since 2.0.3
 */
interface IntrospectedQuery extends StructuredQuery {

	DeclaredQuery getDeclaredQuery();

	default String getQueryString() {
		return getDeclaredQuery().getQueryString();
	}

	/**
	 * @return whether the underlying query has at least one named parameter.
	 */
	boolean hasNamedParameter();

	/**
	 * Returns whether the query uses the default projection, i.e. returns the main alias defined for the query.
	 */
	boolean isDefaultProjection();

	/**
	 * Returns the {@link ParameterBinding}s registered.
	 */
	List<ParameterBinding> getParameterBindings();

	/**
	 * Returns whether the query uses JDBC style parameters, i.e. parameters denoted by a simple ? without any index or
	 * name.
	 *
	 * @return Whether the query uses JDBC style parameters.
	 * @since 2.0.6
	 */
	boolean usesJdbcStyleParameters();

}
