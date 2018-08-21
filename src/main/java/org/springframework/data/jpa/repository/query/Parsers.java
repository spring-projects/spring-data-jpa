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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.data.util.Lazy;

/**
 * Generic ParserCombinators.
 *
 * @author Jens Schauder
 * @since 2.2.0
 */
@UtilityClass
class Parsers {
	/**
	 * Combines a list of parser that each parses a single element into a single parser that parses all the elements in
	 * order returnin a list of the elements.
	 *
	 * @param <S> the type to parse
	 * @param <T> the type of the elements parsed by each parser
	 */
	@SafeVarargs
	static <S, T> Parser<S, List<T>> concat(Supplier<Parser<S, ? extends T>>... parsers) {
		return new Concat<>(parsers);
	}

	/**
	 * Parser combining multiple parsers by using them one after the other until the first one succeeds.
	 *
	 * @param <S> the type of object to parse.
	 * @param <T> the result type of the parser.
	 */
	@SafeVarargs
	public static <S, T> Parser<S, T> either(Parser<S, ? extends T>... parsers) {
		return new Either<>(parsers);
	}

	/**
	 * Creates an optional parser from another parser, by returning a {@link Parser.Success} even when the original
	 * {@link Parser.ParseResult} was a {@link Parser.Failure}.
	 *
	 * @param <S> the type of object to parse.
	 * @param <T> the result type of the parser.
	 */
	static <S, T> Parser<S, java.util.Optional<T>> opt(Parser<S, T> parser) {
		return new Optional<>(parser);
	}

	static <S, T> Parser<S, List<T>> many(Parser<S, T> parser) {
		return new Many<>(parser);
	}

	static <S, T> Parser<S, List<T>> many(Parser<S, T> parser, int min) {
		return new Many<>(parser, min);
	}

	static <S, T> Parser<S, List<T>> many(Parser<S, T> parser, int min, int max) {
		return new Many<>(parser, min, max);
	}

	/**
	 * Combines a list of parser that each parses a single element into a single parser that parses all the elements in
	 * order returning a list of the elements.
	 *
	 * @param <S> the type to parse
	 * @param <T> the type of the elements parsed by each parser
	 */
	public static class Concat<S, T> implements Parser<S, List<T>> {

		private final List<Supplier<Parser<S, ? extends T>>> parsers;

		@SafeVarargs
		private Concat(Supplier<Parser<S, ? extends T>>... parsers) {

			this.parsers = Arrays.stream(parsers) //
					.map(Lazy::of) //
					.collect(Collectors.toList());
		}

		@SuppressWarnings("unchecked")
		@Override
		public ParseResult<S, List<T>> parse(S toParse) {

			BiFunction<ParseResult<S, List<T>>, Parser<S, ? extends T>, ParseResult<S, List<T>>> parseResultAccumulator = (
					parseResult, nextParser) -> {

				if (parseResult instanceof Failure) {
					return parseResult;
				}

				Success<S, List<T>> success = (Success<S, List<T>>) parseResult;
				ParseResult<S, ? extends T> nextResult = nextParser.parse(success.getRemainder());

				if (nextResult instanceof Failure) {
					return new Failure<>(((Failure<S, T>) nextResult).getMessage(), ((Failure<S, T>) nextResult).getRemainder());
				}
				Success<S, T> nextSuccess = (Success<S, T>) nextResult;

				List<T> previousResults = ((Success<S, List<T>>) parseResult).getResult();
				List<T> newResults = new ArrayList<>(previousResults);
				newResults.add(nextSuccess.getResult());
				return new Success<>(newResults, nextSuccess.getRemainder());
			};

			BinaryOperator<ParseResult<S, List<T>>> parseResultCombiner = (pr1, pr2) -> {
				throw new UnsupportedOperationException("can't combine parsers like this. This is a strict left fold");
			};

			return parsers.stream().map(Supplier::get).reduce(new Success<>(Collections.emptyList(), toParse),
					parseResultAccumulator, parseResultCombiner);
		}
	}

	/**
	 * Parser combining multiple parsers by using them one after the other until the first one succeeds.
	 *
	 * @param <S> the type of object to parse.
	 * @param <T> the result type of the parser.
	 */
	public static class Either<S, T> implements Parser<S, T> {

		private final List<Parser<S, ? extends T>> parsers;

		@SafeVarargs
		private Either(Parser<S, ? extends T>... parsers) {
			this.parsers = asList(parsers);
		}

		@SuppressWarnings("unchecked")
		@Override
		public ParseResult<S, T> parse(S toParse) {

			BiFunction<ParseResult<S, ? extends T>, ? super Parser<S, ? extends T>, ParseResult<S, ? extends T>> parseResultAccumulator = (
					r, p) -> {
				if (r instanceof Success) {
					return r;
				}

				return p.parse(toParse);
			};

			BinaryOperator<ParseResult<S, ? extends T>> combiner = (pr1, pr2) -> {
				throw new UnsupportedOperationException("can't combine parsers like this. This is a strict left fold");
			};

			return (ParseResult<S, T>) parsers.stream().reduce(new Failure<>("None of the options match", toParse),
					parseResultAccumulator, combiner);
		}
	}

	/**
	 * Creates an optional parser from another parser, by returning a {@link Success} even when the original
	 * {@link ParseResult} was a {@link Failure}.
	 *
	 * @param <S> the type of object to parse.
	 * @param <T> the result type of the parser.
	 */
	public static class Optional<S, T> implements Parser<S, java.util.Optional<T>> {

		/**
		 * Underlying parser. Must not be {@code null}.
		 */
		Parser<S, T> parser;

		private Optional(Parser<S, T> parser) {
			this.parser = parser;
		}

		@Override
		public ParseResult<S, java.util.Optional<T>> parse(S toParse) {

			ParseResult<S, T> parseResult = parser.parse(toParse);
			if (parseResult instanceof Failure) {
				return new Success<>(java.util.Optional.empty(), toParse);
			}
			return parseResult.map(java.util.Optional::ofNullable);
		}
	}

	@Value
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Many<S, T> implements Parser<S, List<T>> {

		Parser<S, T> parser;
		int minimumMatches;
		int maximumMatches;

		private Many(Parser<S, T> parser, int minimumMatches) {
			this(parser, minimumMatches, Integer.MAX_VALUE);
		}

		private Many(Parser<S, T> parser) {
			this(parser, 0, Integer.MAX_VALUE);
		}

		@Override
		public ParseResult<S, List<T>> parse(S toParse) {

			List<T> results = new ArrayList<>();

			S remainder = null;

			ParseResult<S, T> result = parser.parse(toParse);

			do {

				if (result instanceof Failure) {
					remainder = ((Failure<S, T>) result).getRemainder();
				} else {

					Success<S, T> success = (Success<S, T>) result;
					results.add(success.getResult());

					if (results.size() == maximumMatches) {

						remainder = success.getRemainder();
						break;
					}

					result = parser.parse(success.getRemainder());
				}

			} while (remainder == null && results.size() != maximumMatches);

			if (results.size() >= minimumMatches)
				return new Success<>(results, remainder);
			else {
				return new Failure<>("required %i matches but found only %i", toParse);
			}
		}
	}
}
