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

import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a path-based clause in a query.
 *
 * @author Greg Turnquist
 */
class PathBasedExpressionContext implements ExpressionContext {

	private final String path;

	private final List<Join> joins;

	PathBasedExpressionContext() {
		this("", new ArrayList<>());
	}

	private PathBasedExpressionContext(String path, List<Join> joins) {

		this.path = path;
		this.joins = joins;
	}

	@Override
	public String joins(String alias) {

		return joins.stream() //
				.map(join -> join.build(alias)) //
				.collect(Collectors.joining(" "));
	}

	@Override
	public String criteria(String alias) {

		if (joins.stream().noneMatch(join -> {
			return path.startsWith(join.join() + ".");
		})) {
			return alias + "." + path;
		}

		return path;
	}

	@Override
	public ExpressionContext get(String path, ManagedType<?> model, Class<?> domainClass,
			SingularAttribute<?, ?> attribute) {
		return new PathBasedExpressionContext(path, joins);
	}

	@Override
	public ExpressionContext join(String name) {

		if (joins.stream().noneMatch(join -> join.join().equals(name))) {
			joins.add(new OuterJoin(name));
		}
		return this;
	}

	@Override
	public ExpressionContext join(Join join) {

		if (joins.stream().noneMatch(j -> j.equals(join))) {
			joins.add(join);
		}
		return this;
	}

	@Override
	public List<Join> joins() {
		return joins;
	}
}
