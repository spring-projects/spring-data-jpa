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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.mapping.PropertyPath;

/**
 * Represents a {@link PropertyPath}-based clause of a query.
 * 
 * @author Greg Turnquist
 */
class PropertyBasedExpressionContext implements ExpressionContext {

	private final PropertyPath property;

	private final List<Join> joins;

	public PropertyBasedExpressionContext(PropertyPath property) {

		this.property = property;
		this.joins = new ArrayList<>();
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
			return property.getSegment().startsWith(join.join());
		})) {
			return alias + "." + property.toDotPath();
		}

		return property.toDotPath();
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
