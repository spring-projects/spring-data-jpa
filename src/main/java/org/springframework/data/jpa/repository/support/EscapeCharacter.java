/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.support;

import lombok.Value;

import org.springframework.util.Assert;

/**
 * A Value-class encapsulating an escape character for LIKE queries and the actually usage of it in escaping Strings.
 *
 * @author Jens Schauder
 */
@Value(staticConstructor = "of")
public class EscapeCharacter {
	char value;

	public String escape(String value) {

		Assert.notNull(value, "Value must be not null.");

		return value.replace("_", value + "_").replace("%", value + "%");
	}

	// used for SpEL expressions
	static String escape(String value, String escape) {

		Assert.hasText(escape, "escape must be a sinlge character String.");
		char[] chars = escape.toCharArray();
		Assert.isTrue(chars.length == 1, "escape must be a single character String.");

		return EscapeCharacter.of(chars[0]).escape(value);
	}
}
