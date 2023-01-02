/*
 * Copyright 2019-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * A value type encapsulating an escape character for LIKE queries and the actually usage of it in escaping
 * {@link String}s.
 *
 * @author Jens Schauder
 * @author Oliver Drotbohm
 */
public final class EscapeCharacter {

	public static final EscapeCharacter DEFAULT = EscapeCharacter.of('\\');
	private static final List<String> TO_REPLACE = Arrays.asList("_", "%");

	private final char escapeCharacter;

	private EscapeCharacter(char escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

	public static EscapeCharacter of(char escapeCharacter) {
		return new EscapeCharacter(escapeCharacter);
	}

	/**
	 * Escapes all special like characters ({@code _}, {@code %}) using the configured escape character.
	 *
	 * @param value may be {@literal null}.
	 * @return
	 */
	@Nullable
	public String escape(@Nullable String value) {

		return value == null //
				? null //
				: Stream.concat(Stream.of(String.valueOf(escapeCharacter)), TO_REPLACE.stream()) //
						.reduce(value, (it, character) -> it.replace(character, this.escapeCharacter + character));
	}

	public char getEscapeCharacter() {
		return this.escapeCharacter;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof EscapeCharacter)) {
			return false;
		}

		EscapeCharacter that = (EscapeCharacter) o;
		return escapeCharacter == that.escapeCharacter;
	}

	@Override
	public int hashCode() {
		return escapeCharacter;
	}

	@Override
	public String toString() {
		return "EscapeCharacter(escapeCharacter=" + this.getEscapeCharacter() + ")";
	}
}
