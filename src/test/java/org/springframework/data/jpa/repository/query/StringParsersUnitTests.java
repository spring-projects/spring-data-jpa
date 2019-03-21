/*
 * Copyright 2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * Unit tests for {@code String} based unit tests.
 *
 * @author Jens Schauder
 */
public class StringParsersUnitTests {

	public static class KeyWordParserUnitTests {

		public static final String KEYWORD_STRING = "keyword";
		public static final Parser<String, String> KEYWORD = StringParsers.keyword(KEYWORD_STRING);

		@Test // DATAJPA-1406
		public void exactMatch() {

			assertThat(KEYWORD.parse(KEYWORD_STRING)).isEqualTo(new Parser.Success<>(KEYWORD_STRING, ""));
		}

		@Test // DATAJPA-1406
		public void ignoreCaseMatch() {

			assertThat(KEYWORD.parse("kEyWoRd")).isEqualTo(new Parser.Success<>(KEYWORD_STRING, ""));
		}

		@Test // DATAJPA-1406
		public void literalMatch() {

			assertThat(StringParsers.keyword(".*").parse(KEYWORD_STRING)).isInstanceOf(Parser.Failure.class);
			assertThat(StringParsers.keyword("Test*pattern").parse("TEST*PATTERN"))
					.isEqualTo(new Parser.Success<>("Test*pattern", ""));

		}

		@Test // DATAJPA-1406
		public void startsWithMatch() {

			assertThat(KEYWORD.parse(KEYWORD_STRING + "Something"))
					.isEqualTo(new Parser.Success<>(KEYWORD_STRING, "Something"));
		}

		@Test // DATAJPA-1406
		public void noMatch() {

			assertThat(KEYWORD.parse("Something")).isInstanceOf(Parser.Failure.class);
		}

		@Test // DATAJPA-1406
		public void lateMatchDoesNotMatch() {

			assertThat(KEYWORD.parse("Something" + KEYWORD_STRING)).isInstanceOf(Parser.Failure.class);
		}
	}

	public static class RegexParserUnitTests {

		Parser<String, String> REGEX = StringParsers.regex("ab+c*");

		@Test // DATAJPA-1406
		public void exactMatch() {

			assertThat(REGEX.parse("abbb")).isEqualTo(new Parser.Success<>("abbb", ""));
		}

		@Test // DATAJPA-1406
		public void startsWithMatch() {

			assertThat(REGEX.parse("abbcc" + "Something")).isEqualTo(new Parser.Success<>("abbcc", "Something"));
		}

		@Test // DATAJPA-1406
		public void noMatch() {

			assertThat(REGEX.parse("Something")).isInstanceOf(Parser.Failure.class);
		}

		@Test // DATAJPA-1406
		public void lateMatchDoesNotMatch() {

			assertThat(REGEX.parse("Something" + "abc")).isInstanceOf(Parser.Failure.class);
		}
	}
}
