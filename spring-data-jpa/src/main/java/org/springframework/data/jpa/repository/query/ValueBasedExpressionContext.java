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
 * Represents a value in a query.
 * 
 * @author Greg Turnquist
 */
class ValueBasedExpressionContext implements ExpressionContext {

	private final Object value;

	public ValueBasedExpressionContext(Object value) {
		this.value = value;
	}

	@Override
	public String joins(String alias) {
		return "";
	}

	@Override
	public String criteria(String alias) {

		// Numbers and booleans don't need to be wrapped in anything.
		if (Number.class.isAssignableFrom(value.getClass()) || Boolean.class.isAssignableFrom(value.getClass())) {
			return value.toString();
		}

		if (value instanceof String stringValue) {

			// Strings that start with ":" or "?" are parameters, and also must NOT be wrapped in anything.
			if (stringValue.startsWith(":") || stringValue.startsWith("?")) {
				return stringValue;
			}
		}

		// Anything else, string or not, can be wrapped in single quotes
		return "'" + value + "'";
	}
}
