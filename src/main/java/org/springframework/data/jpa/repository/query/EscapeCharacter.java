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
package org.springframework.data.jpa.repository.query;

import lombok.Value;

import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;

/**
 * A value type encapsulating an escape character for LIKE queries and the actually usage of it in escaping
 * {@link String}s.
 *
 * @author Jens Schauder
 * @author Oliver Drotbohm
 */
@Value(staticConstructor = "of")
public class EscapeCharacter {

	private static final List<String> TO_REPLACE = Arrays.asList("_", "%");

	char value;

	/**
	 * Escapes all special like characters ({@code _}, {@code %}) using the configured escape character.
	 *
	 * @param value must not be {@literal null}.
	 * @return
	 */
	public String escape(String value) {

		Assert.notNull(value, "Value must be not null.");

		return TO_REPLACE.stream() //
				.reduce(value, (it, character) -> it.replace(character, this.value + character));
	}
}
