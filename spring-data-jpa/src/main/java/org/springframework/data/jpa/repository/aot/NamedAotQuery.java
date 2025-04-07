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
package org.springframework.data.jpa.repository.aot;

import java.util.List;

import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.ParameterBinding;
import org.springframework.data.jpa.repository.query.PreprocessedQuery;

/**
 * Value object to describe a named AOT query.
 *
 * @author Mark Paluch
 * @since 4.0
 */
class NamedAotQuery extends AotQuery {

	private final String name;
	private final DeclaredQuery query;

	private NamedAotQuery(String name, DeclaredQuery queryString, List<ParameterBinding> parameterBindings) {
		super(parameterBindings);
		this.name = name;
		this.query = queryString;
	}

	/**
	 * Creates a new {@code NamedAotQuery}.
	 */
	public static NamedAotQuery named(String namedQuery, DeclaredQuery queryString) {

		PreprocessedQuery parsed = PreprocessedQuery.parse(queryString);
		return new NamedAotQuery(namedQuery, queryString, parsed.getBindings());
	}

	public String getName() {
		return name;
	}

	public DeclaredQuery getQuery() {
		return query;
	}

	public String getQueryString() {
		return getQuery().getQueryString();
	}

	@Override
	public boolean isNative() {
		return query.isNative();
	}

}
