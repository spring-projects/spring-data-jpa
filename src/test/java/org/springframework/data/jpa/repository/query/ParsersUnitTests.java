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
import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.springframework.data.util.Pair;

/**
 * Unit tests for the classes in the Parser interface.
 *
 * @author Jens Schauder
 */
public class ParsersUnitTests {

	Parser<String, String> a = StringParsers.keyword("a");
	Parser<String, String> openingParen = StringParsers.keyword("(");
	Parser<String, String> closingParen = StringParsers.keyword(")");
	Parser<String, List<Object>> expression = recursiveExpression();

	@SuppressWarnings("unchecked")
	private Parser<String, List<Object>> recursiveExpression() {

		return Parsers.concat( //
				() -> openingParen, //
				() -> Parsers.either(a, recursiveExpression()), //
				() -> closingParen //
		);
	}

	@Test // DATAJPA-1406
	public void recursionMatchingParens() {

		assertThat(expression.parse("(((a)))")) //
				.describedAs("matching parens") //
				.isInstanceOf(Parser.Success.class);
	}

	@Test // DATAJPA-1406
	public void recursionSuperfluousClosingParens() {

		assertThat(expression.parse("(((a))))")) //
				.describedAs("superfluos closing paren") //
				.extracting(Parser.ParseResult::getRemainder) //
				.isEqualTo(")");
	}

	@Test // DATAJPA-1406
	public void recursionMissingClosingParens() {

		assertThat(expression.parse("(((a))")) //
				.describedAs("missing closing paren") //
				.isInstanceOf(Parser.Failure.class);
	}

	public static class ConcatParserUnitTests {

		Parser<String, String> ws = StringParsers.regex("\\s+");
		Parser<String, String> token = StringParsers.regex("\\w+");
		Parser<String, String> select = StringParsers.keyword("select");
		Parser<String, String> from = StringParsers.keyword("from");

		Parser<String, List<String>> sql = Parsers.concat(() -> select, () -> ws, () -> token, () -> ws, () -> from);

		@Test // DATAJPA-1406
		public void exactMatch() {

			assertThat(sql.parse("select a from"))
					.isEqualTo(new Parser.Success<>(asList("select", " ", "a", " ", "from"), ""));
		}

		@Test // DATAJPA-1406
		public void startsWithMatch() {

			assertThat(sql.parse("select a from where clause"))
					.isEqualTo(new Parser.Success<>(asList("select", " ", "a", " ", "from"), " where clause"));
		}

		@Test // DATAJPA-1406
		public void failureToMatch() {

			assertThat(sql.parse("select a as b from")) //
					.isInstanceOf(Parser.Failure.class) //
					.extracting(Parser.ParseResult::getRemainder) //
					.isEqualTo("as b from");
		}
	}

	public static class EitherParserUnitTests {

		Parser<String, String> aa = StringParsers.keyword("aa");
		Parser<String, String> aaa = StringParsers.keyword("aaa");
		Parser<String, String> bb = StringParsers.keyword("bb");

		@Test // DATAJPA-1406
		public void matchOnFirst() {

			assertThat(Parsers.either(aa, bb).parse("aabb")).isEqualTo(new Parser.Success<>("aa", "bb"));
		}

		@Test // DATAJPA-1406
		public void matchOnSecond() {

			assertThat(Parsers.either(aa, bb).parse("bbaa")).isEqualTo(new Parser.Success<>("bb", "aa"));
		}

		@Test // DATAJPA-1406
		public void prefersFirst() {

			assertThat(Parsers.either(aa, aaa).parse("aaaa")).isEqualTo(new Parser.Success<>("aa", "aa"));
		}

		@Test // DATAJPA-1406
		public void failure() {

			assertThat(Parsers.either(aaa, bb).parse("aabb")).isInstanceOf(Parser.Failure.class)
					.extracting((Parser.ParseResult::getRemainder)).isEqualTo("aabb");
		}
	}

	public static class OptionalParserUnitTests {

		Parser<String, String> aa = StringParsers.keyword("aa");

		@Test // DATAJPA-1406
		public void prefersFirst() {

			assertThat(Parsers.opt(aa).parse("aaaa")).isEqualTo(new Parser.Success<>(Optional.of("aa"), "aa"));
		}

		@Test // DATAJPA-1406
		public void failureToMatchResultsInEmptyResult() {

			assertThat(Parsers.opt(aa).parse("bbb")).isEqualTo(new Parser.Success<>(Optional.empty(), "bbb"));
		}
	}

	public static class ManyParserUnitTest {

		Parser<String, String> aa = StringParsers.keyword("aa");

		@Test // DATAJPA-1406
		public void match() {

			assertThat(Parsers.many(aa).parse("aaaaaaa")) //
					.isEqualTo(new Parser.Success<>(asList("aa", "aa", "aa"), "a"));
		}

		@Test // DATAJPA-1406
		public void matchNone() {

			assertThat(Parsers.many(aa).parse("axaaa")) //
					.isEqualTo(new Parser.Success<>(Collections.emptyList(), "axaaa"));
		}

		@Test // DATAJPA-1406
		public void matchAtLeastSuccess() {

			assertThat(Parsers.many(aa, 3).parse("aaaaaaa")) //
					.isEqualTo(new Parser.Success<>(asList("aa", "aa", "aa"), "a"));
		}

		@Test // DATAJPA-1406
		public void matchAtLeastFailure() {

			assertThat(Parsers.many(aa, 3).parse("aaaaaxaa")) //
					.isInstanceOf(Parser.Failure.class) //
					.extracting(Parser.ParseResult::getRemainder).isEqualTo("aaaaaxaa");
		}

		@Test // DATAJPA-1406
		public void matchAtMostSuccess() {

			assertThat(Parsers.many(aa, 3, 3).parse("aaaaaaa")) //
					.isEqualTo(new Parser.Success<>(asList("aa", "aa", "aa"), "a"));
		}

		@Test // DATAJPA-1406
		public void matchAtMostFailure() {

			assertThat(Parsers.many(aa, 1, 2).parse("aaaaaaa")) //
					.isEqualTo(new Parser.Success<>(asList("aa", "aa"), "aaa"));
		}

	}

	public static class NonStringParsersUnitTests {

		@Test // DATAJPA-1406
		public void parseAListOfStringsIntoTuples() {

			Parser<List<String>, Pair<String, String>> pairParser = toParse -> {

				if (toParse.size() < 2) {
					return new Parser.Failure<>("Not enough elements in list", toParse);
				}

				return new Parser.Success<>(Pair.of(toParse.get(0), toParse.get(1)), toParse.subList(2, toParse.size()));
			};

			Parser.ParseResult<List<String>, Pair<String, String>> result = pairParser.parse(asList("one", "two", "three"));
			assertThat(result).isEqualTo(new Parser.Success<>(Pair.of("one", "two"), Collections.singletonList("three")));

		}
	}
}
