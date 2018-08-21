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

import lombok.NonNull;
import lombok.Value;

import java.util.function.Function;

/**
 * A basic library for Parser Combinators. A parser is a function from the thing to parse (e.g. a {@code String} or a
 * {@code List<Object>} to the {@link ParseResult}. The {@link ParseResult} is either a {@link Success} or a
 * {@link Failure}. Both contain the unparsed remainder of the original object to parse and either the parsed element or
 * an error message.
 *
 * @author Jens Schauder
 * @since 2.2.0
 */
interface Parser<S, T> {

	/**
	 * Attempts to parse an input value.
	 *
	 * @param toParse the object to be parsed. Must not be {@code null}.
	 * @return the result of the parsing attempt.
	 */
	ParseResult<S, T> parse(S toParse);

	/**
	 * Creates a new {@code Parser} from the current one, by applying a function to the result of a successful parse
	 * attempt.
	 *
	 * @param function the function to apply. Must not be {@code null}.
	 * @param <U> the result type of the new {@code Parser}.
	 * @return a new Parser. Guaranteed to be not {@code null}.
	 */
	default <U> Parser<S, U> map(Function<T, U> function) {

		return toParse -> this.parse(toParse).map(function);
	}

	/**
	 * The result of a parse attempt.
	 *
	 * @param <S> the type that was attempted to parse
	 * @param <T> the result type of the parse attempt.
	 */
	interface ParseResult<S, T> {

		/**
		 * Maps a {@code ParseResult} to a new {@code ParseResult} by applying the function to the result.
		 *
		 * @param function used to map the result. Must not be {@code null}.
		 * @param <U> result type of the function and the resulting {@code ParseResult}.
		 * @return a new {@code ParseResult}
		 */
		<U> ParseResult<S, U> map(Function<T, U> function);

		/**
		 * The remaining part of the original object to be parsed that didn't get parsed.
		 *
		 * @return an Object that still needs parsing. Guaranteed to be not {@code null}.
		 */
		S getRemainder();
	}

	/**
	 * Signals a successful parse attempt. The meaning of success depends on the parser. Typically it means the parser
	 * consumed at least some of the input. But a parser parsing an optional token it might even succeed without consuming
	 * anything.
	 *
	 * @param <S> type that is getting parsed.
	 * @param <T> type of the result.
	 */
	@Value
	class Success<S, T> implements ParseResult<S, T> {

		/** The result of the parse attempt. May be {@code null}. */
		T result;
		@NonNull S remainder;

		@Override
		public <U> ParseResult<S, U> map(Function<T, U> f) {
			return new Success<>(f.apply(result), remainder);
		}
	}

	/**
	 * Signals a failure to parse the input.
	 *
	 * @param <S> type that is getting parsed.
	 * @param <T> type of the result.
	 */
	@Value
	class Failure<S, T> implements ParseResult<S, T> {

		/**
		 * Gives a reason and/or explanation for the failure to parse the input.
		 */
		@NonNull String message;
		@NonNull S remainder;

		@SuppressWarnings("unchecked")
		@Override
		public <U> ParseResult<S, U> map(Function<T, U> f) {
			return (ParseResult<S, U>) this;
		}
	}

}
