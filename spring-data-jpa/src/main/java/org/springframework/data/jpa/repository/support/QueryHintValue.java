/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import java.util.Objects;

import org.springframework.util.Assert;

/**
 * Value object carrying a query hint consisting of a name/key and a value.
 *
 * @author Jens Schauder
 * @since 2.4
 */
public class QueryHintValue {

	public final String name;
	public final Object value;

	public QueryHintValue(String name, Object value) {

		Assert.notNull(name, "Name must not be null");
		Assert.notNull(value, "Value must not be null");

		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return "QueryHintValue{" + "name='" + name + '\'' + ", value='" + value + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		QueryHintValue that = (QueryHintValue) o;
		return name.equals(that.name) && value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}
}
