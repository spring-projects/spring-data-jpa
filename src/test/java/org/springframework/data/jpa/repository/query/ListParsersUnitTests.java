/*
 * Copyright 2018 the original author or authors.
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

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link ListParsers}.
 *
 * @author Jens Schauder
 */
public class ListParsersUnitTests {

	public static class FilteringParserUnitTests {

		Parser<List<String>, List<Object>> filteringParser = //
				ListParsers.filtering( //
						Parsers.<List<String>, String> concat( //
								ListParsers::any, //
								() -> ListParsers.keyword("as"), //
								ListParsers::any //
						).map(l -> l.get(2)));

		@Test // DATAJPA-1406
		public void noMatch() {

			assertThat(filteringParser.parse(asList("blah", "blah", "blah", "blah", "blah"))) //
					.isEqualTo(new Parser.Success<>(emptyList(), emptyList()));

		}

		@Test // DATAJPA-1406
		public void startingMatch() {

			assertThat(filteringParser.parse(asList("x", "as", "y", "blah"))) //
					.isEqualTo(new Parser.Success<>(singletonList("y"), emptyList()));

		}

		@Test // DATAJPA-1406
		public void singleMatch() {

			assertThat(filteringParser.parse(asList("blah", "x", "as", "y", "blah"))) //
					.isEqualTo(new Parser.Success<>(singletonList("y"), emptyList()));

		}

		@Test // DATAJPA-1406
		public void multipleMatches() {

			assertThat(filteringParser.parse(asList("blah", "x", "as", "y", "u", "as", "i"))) //
					.isEqualTo(new Parser.Success<>(asList("y", "i"), emptyList()));

		}

		@Test // DATAJPA-1406
		public void multipleOverlappingMatches() {

			assertThat(filteringParser.parse(asList("blah", "x", "as", "y", "as", "i"))) //
					.isEqualTo(new Parser.Success<>(Collections.singletonList("y"), emptyList()));

		}
	}

	public static class ElementParserUnitTests {
		Parser<List<Object>, String> parser = ListParsers.element(String.class, s -> true);

		@Test // DATAJPA-1406
		public void emptyListFailsToParse() {
			assertThat(parser.parse(emptyList())).isInstanceOf(Parser.Failure.class);
		}

		@Test // DATAJPA-1406
		public void noMatch() {
			assertThat(parser.parse(singletonList(1))).isInstanceOf(Parser.Failure.class);
		}

		@Test // DATAJPA-1406
		public void match() {

			assertThat(parser.parse(asList("hello", "world")))
					.isEqualTo(new Parser.Success<>("hello", singletonList("world")));
		}
	}

	public static class TypedElementParserUnitTests {

		Parser<List<Object>, Integer> parser = ListParsers.element(Integer.class, i -> i > 23);

		@Test // DATAJPA-1406
		public void emptyListFailsToParse() {
			assertThat(parser.parse(emptyList())).isInstanceOf(Parser.Failure.class);
		}

		@Test // DATAJPA-1406
		public void noMatchWrongType() {
			assertThat(parser.parse(asList("x", 1))).isInstanceOf(Parser.Failure.class);
		}

		@Test // DATAJPA-1406
		public void noMatchCondition() {
			assertThat(parser.parse(asList(22, 1))).isInstanceOf(Parser.Failure.class);
		}

		@Test // DATAJPA-1406
		public void match() {
			assertThat(parser.parse(asList(24, "world"))).isEqualTo(new Parser.Success<>("hello", singletonList("world")));
		}
	}
}
