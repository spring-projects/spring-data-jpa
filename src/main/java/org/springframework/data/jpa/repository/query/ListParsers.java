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

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Parsers with source type List that are useful for parsing the result of a Tokenizer. A parser must not change the
 * list it parses.
 *
 * @author Jens Schauder
 * @since 2.2.0
 */
@UtilityClass
final class ListParsers {

	/**
	 * Parses a single element from the list. By testing that it extends the provided class and then with the predicate.
	 * 
	 * @param <S> element type of the list to parse
	 * @param <T> result type of the parser.
	 */
	static <S, T> Parser<List<S>, T> element(Class<T> targetType, Predicate<T> predicate) {
		return new ElementParser<>(targetType, predicate);
	}

	/**
	 * Parses a single element from the list. By testing it with the predicate.
	 *
	 * @param <T> element type of the source list and result type of the parser.
	 */
	@SuppressWarnings("unchecked")
	static <T> Parser<List<T>, T> element(Predicate<T> predicate) {
		return new ElementParser<T, T>((Class<T>) Object.class, predicate);
	}

	/**
	 * Parses any element of a list.
	 *
	 * @param <T> element type of the source list and result type of the parser.
	 * @return a parser that can parse the first element of a list.
	 */
	static <T> Parser<List<T>, T> any() {
		return element(s -> true);
	}

	static <S> Parser<List<S>, ? extends String> keyword(String keyword) {
		return element(String.class, s -> s.equalsIgnoreCase(keyword));
	}

	static <S, T> Parser<List<S>, List<T>> filtering(Parser<List<S>, T> parser) {
		return new FilteringParser<>(parser);
	}

	/**
	 * Parses a single element from the list. By testing it with the predicate.
	 * 
	 * @param <S> element type of the list to parse
	 * @param <T> result type of the parser.
	 */
	private class ElementParser<S, T> implements Parser<List<S>, T> {

		private final Predicate<S> predicate;

		@SuppressWarnings("unchecked")
		private ElementParser(Class<T> targetType, Predicate<T> predicate) {
			this.predicate = s -> targetType.isAssignableFrom(s.getClass()) && predicate.test((T) s);
		}

		@SuppressWarnings("unchecked")
		@Override
		public ParseResult<List<S>, T> parse(List<S> toParse) {

			if (toParse.isEmpty()) {
				return new Failure<>("Nothing to parse.", toParse);
			}

			S element = toParse.get(0);
			if (predicate.test(element)) {
				return new Success<>((T) element, toParse.subList(1, toParse.size()));
			}

			return new Failure<>("Does not match.", toParse);
		}
	}

	/**
	 * A parser that scans a List for matches of the provided parser and returns all the parts of the list matched by that
	 * parser, discarding everything else.
	 *
	 * @param <S> element type of the list to parse.
	 * @param <T> result type of the parser.
	 */
	private class FilteringParser<S, T> implements Parser<List<S>, List<T>> {

		private final Parser<List<S>, T> parser;

		FilteringParser(Parser<List<S>, T> parser) {
			this.parser = parser;
		}

		@Override
		public ParseResult<List<S>, List<T>> parse(List<S> toParse) {

			List<T> results = new ArrayList<>();

			List<S> rest = toParse;
			while (rest.size() > 1) {

				ParseResult<List<S>, T> result = parser.parse(rest);
				if (result instanceof Success) {

					Success<List<S>, T> success = (Success<List<S>, T>) result;
					results.add(success.getResult());
					rest = success.getRemainder();
				} else {
					rest = rest.subList(1, rest.size());
				}
			}

			return new Success<>(results, Collections.emptyList());
		}
	}
}
