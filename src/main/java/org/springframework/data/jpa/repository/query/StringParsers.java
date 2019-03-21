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

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsers that parse Strings.
 *
 * @author Jens Schauder
 * @since 2.2.0
 */
class StringParsers {

	/**
	 * Produces a parser that can parse the keyword ignoring the case at the beginning of a {@code String}. No matter how
	 * the matched substring was written it is returned by the parser in exact the spelling as provided in the parameter.
	 *
	 * @param keyword the {@code String} to parse.
	 * @return a Parser.
	 */
	static Parser<String, String> keyword(String keyword) {

		return s -> new Regex(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE) //
				.parse(s) //
				.map(t -> keyword);
	}

	/**
	 * Produces a parser matching the beginning of a String that is matched by the provided regex.
	 *
	 * @param regexString the regular expression to be parsed by the parser.
	 * @return a Parser.
	 */
	static Parser<String, String> regex(String regexString) {

		return s -> new Regex(regexString) //
				.parse(s);
	}

	@Value
	@AllArgsConstructor
	private static final class Regex implements Parser<String, String> {

		Pattern compiled;

		Regex(String pattern, int flags) {
			this(Pattern.compile("^" + pattern, flags));
		}

		Regex(String patternString) {
			this(patternString, 0);
		}

		@Override
		public ParseResult<String, String> parse(String toParse) {

			Matcher matcher = compiled.matcher(toParse);

			if (matcher.find()) {
				return new Success<>(matcher.group(), toParse.substring(matcher.end()));
			} else {
				return new Failure<>(compiled + " does not match " + toParse, toParse);
			}
		}
	}
}
