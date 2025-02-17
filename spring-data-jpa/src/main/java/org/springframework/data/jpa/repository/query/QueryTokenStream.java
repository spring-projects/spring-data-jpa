/*
 * Copyright 2024-2025 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;

import org.springframework.data.util.Streamable;
import org.springframework.util.CollectionUtils;

/**
 * Stream of {@link QueryToken}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.4
 */
interface QueryTokenStream extends Streamable<QueryToken> {

	/**
	 * Creates an empty stream.
	 */
	static QueryTokenStream empty() {
		return EmptyQueryTokenStream.INSTANCE;
	}

	/**
	 * Creates a QueryTokenStream from a {@link QueryToken}.
	 * @since 4.0
	 */
	static QueryTokenStream from(QueryToken token) {
		return QueryRenderer.from(Collections.singletonList(token));
	}

	/**
	 * Creates an token QueryRenderer from an AST {@link TerminalNode}.
	 * @since 4.0
	 */
	static QueryTokenStream ofToken(TerminalNode node) {
		return from(QueryTokens.token(node));
	}

	/**
	 * Creates an token QueryRenderer from an AST {@link Token}.
	 * @since 4.0
	 */
	static QueryTokenStream ofToken(Token node) {
		return from(QueryTokens.token(node));
	}

	/**
	 * Compose a {@link QueryTokenStream} from a collection of inline elements.
	 *
	 * @param elements collection of elements.
	 * @param visitor visitor function converting the element into a {@link QueryTokenStream}.
	 * @param separator separator token.
	 * @return the composed token stream.
	 */
	static <T> QueryTokenStream concat(Collection<T> elements, Function<T, QueryTokenStream> visitor,
			QueryToken separator) {
		return concat(elements, visitor, QueryRenderer::inline, separator);
	}

	/**
	 * Compose a {@link QueryTokenStream} from a collection of expression elements.
	 *
	 * @param elements collection of elements.
	 * @param visitor visitor function converting the element into a {@link QueryTokenStream}.
	 * @param separator separator token.
	 * @return the composed token stream.
	 */
	static <T> QueryTokenStream concatExpressions(Collection<T> elements, Function<T, QueryTokenStream> visitor,
			QueryToken separator) {
		return concat(elements, visitor, QueryRenderer::ofExpression, separator);
	}

	/**
	 * Compose a {@link QueryTokenStream} from a collection of elements.
	 *
	 * @param elements collection of elements.
	 * @param visitor visitor function converting the element into a {@link QueryTokenStream}.
	 * @param separator separator token.
	 * @param postProcess post-processing function to map {@link QueryTokenStream}.
	 * @return the composed token stream.
	 */
	static <T> QueryTokenStream concat(Collection<T> elements, Function<T, QueryTokenStream> visitor,
			Function<QueryTokenStream, QueryTokenStream> postProcess, QueryToken separator) {

		QueryRenderer.QueryRendererBuilder builder = null;
		QueryTokenStream firstElement = null;
		for (T element : elements) {

			QueryTokenStream tokenStream = postProcess.apply(visitor.apply(element));

			if (firstElement == null) {
				firstElement = tokenStream;
				continue;
			}

			if (builder == null) {
				builder = QueryRenderer.builder();
				builder.append(firstElement);
			}

			if (!builder.isEmpty()) {
				builder.append(separator);
			}
			builder.append(tokenStream);
		}

		if (builder != null) {
			return builder;
		}

		if (firstElement != null) {
			return firstElement;
		}

		return QueryTokenStream.empty();
	}

	/**
	 * @return the first query token or {@code null} if empty.
	 */
	default @Nullable QueryToken getFirst() {

		Iterator<QueryToken> it = iterator();
		return it.hasNext() ? it.next() : null;
	}

	/**
	 * @return the required first query token or throw {@link java.util.NoSuchElementException} if empty.
	 * @since 4.0
	 */
	default QueryToken getRequiredFirst() {

		QueryToken first = getFirst();

		if (first == null) {
			throw new NoSuchElementException("No token in the stream");
		}

		return first;
	}

	/**
	 * @return the last query token or {@code null} if empty.
	 */
	default @Nullable QueryToken getLast() {
		return CollectionUtils.lastElement(toList());
	}

	/**
	 * @return the required last query token or throw {@link java.util.NoSuchElementException} if empty.
	 * @since 4.0
	 */
	default QueryToken getRequiredLast() {

		QueryToken last = getLast();

		if (last == null) {
			throw new NoSuchElementException("No token in the stream");
		}

		return last;
	}

	/**
	 * @return {@code true} if this stream represents a query expression.
	 */
	boolean isExpression();

	/**
	 * @return the number of tokens.
	 */
	int size();

	/**
	 * @return {@code true} if this stream contains no tokens.
	 */
	boolean isEmpty();

}
