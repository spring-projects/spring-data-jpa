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

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.data.util.Pair;

/**
 * Unit tests for the classes in the Parser interface.
 *
 * @author Jens Schauder
 */
public class ParserUnitTests {

	Parser<String, String> a = StringParsers.keyword("a");

	@Test // DATAJPA-1406
	public void mappingOfSuccess() {

		Parser.Success<String, String> success = new Parser.Success<>("value", "rest");

		assertThat(success.map(String::toUpperCase)).isEqualTo(new Parser.Success<>("VALUE", "rest"));
	}

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
