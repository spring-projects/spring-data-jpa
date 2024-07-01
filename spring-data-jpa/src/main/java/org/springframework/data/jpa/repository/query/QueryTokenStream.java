/*
 * Copyright 2024 the original author or authors.
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
import java.util.Iterator;
import java.util.function.Function;

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
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
	 *
	 * @return
	 */
	static QueryTokenStream empty() {
		return EmptyQueryTokenStream.INSTANCE;
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

	static <T> QueryTokenStream justAs(Collection<T> elements, Function<T, QueryToken> converter) {
		return concat(elements, it-> QueryRendererBuilder.from(converter.apply(it)), QueryRenderer::inline, QueryTokens.TOKEN_SPACE);
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
		return concat(elements, visitor, QueryRenderer::expression, separator);
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
	@Nullable
	default QueryToken getFirst() {

		Iterator<QueryToken> it = iterator();
		return it.hasNext() ? it.next() : null;
	}

	/**
	 * @return the last query token or {@code null} if empty.
	 */
	@Nullable
	default QueryToken getLast() {
		return CollectionUtils.lastElement(toList());
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
