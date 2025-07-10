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

import static org.springframework.data.jpa.repository.query.QueryTokens.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
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
	 * Compose a {@link QueryTokenStream} from a collection of elements. Expressions are rendered using space separators.
	 *
	 * @param elements collection of elements.
	 * @param visitor visitor function converting the element into a {@link QueryTokenStream}.
	 * @return the composed token stream.
	 * @since 4.0
	 */
	static <T> QueryTokenStream concatExpressions(Collection<T> elements, Function<T, QueryTokenStream> visitor) {

		if (CollectionUtils.isEmpty(elements)) {
			return QueryTokenStream.empty();
		}

		QueryRenderer.QueryRendererBuilder builder = QueryRenderer.builder();

		for (T child : elements) {

			if (child instanceof TerminalNode tn) {
				builder.append(QueryTokens.expression(tn));
			} else {
				builder.appendExpression(visitor.apply(child));
			}
		}

		return builder.build();
	}

	/**
	 * Compose a {@link QueryTokenStream} from a collection of expressions from a {@link Tree}. Expressions are rendered
	 * using space separators.
	 *
	 * @param elements collection of elements.
	 * @param visitor visitor function converting the element into a {@link QueryTokenStream}.
	 * @return the composed token stream.
	 * @since 4.0
	 */
	static QueryTokenStream concatExpressions(Tree elements, Function<? super ParseTree, QueryTokenStream> visitor) {

		int childCount = elements.getChildCount();
		if (childCount == 0) {
			return QueryTokenStream.empty();
		}

		QueryRenderer.QueryRendererBuilder builder = QueryRenderer.builder();

		for (int i = 0; i < childCount; i++) {

			Tree child = elements.getChild(i);
			if (child instanceof TerminalNode tn) {
				builder.append(QueryTokens.expression(tn));
			} else if (child instanceof ParseTree pt) {
				builder.appendExpression(visitor.apply(pt));
			} else {
				throw new IllegalArgumentException("Unsupported child type: " + child);
			}
		}

		return builder.build();
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
	 * Creates a {@link QueryTokenStream} that groups the given {@link QueryTokenStream nested token stream} in
	 * parentheses ({@code (…)}).
	 *
	 * @param nested the nested token stream to wrap in parentheses.
	 * @return a {@link QueryTokenStream} that groups the given {@link QueryTokenStream nested token stream} in
	 *         parentheses.
	 * @since 5.0
	 */
	static QueryTokenStream group(QueryTokenStream nested) {

		QueryRenderer.QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		return builder.build();
	}

	/**
	 * Creates a {@link QueryTokenStream} representing a function call including arguments wrapped in parentheses.
	 *
	 * @param functionName function name.
	 * @param arguments the arguments of the function call.
	 * @return a {@link QueryTokenStream} representing a function call.
	 * @since 5.0
	 */
	static QueryTokenStream ofFunction(TerminalNode functionName, QueryTokenStream arguments) {

		QueryRenderer.QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(QueryTokens.token(functionName));
		builder.append(TOKEN_OPEN_PAREN);
		builder.appendInline(arguments);
		builder.append(TOKEN_CLOSE_PAREN);

		return builder.build();
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
