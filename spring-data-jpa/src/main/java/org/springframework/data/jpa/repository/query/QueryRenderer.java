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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.antlr.v4.runtime.TokenStream;
import org.springframework.util.CompositeIterator;

/**
 * Abstraction to encapsulate query expressions and render a query.
 * <p>
 * Query rendering consists of multiple building blocks:
 * <ul>
 * <li>{@link JpaQueryParsingToken tokens} and
 * {@link org.springframework.data.jpa.repository.query.JpaQueryParsingToken.JpaExpressionToken expression tokens}</li>
 * <li>{@link QueryRenderer compositions} such as a composition of multiple tokens.</li>
 * <li>{@link QueryRenderer expressions} that are individual parts such as {@code SELECT} or {@code ORDER BY …}</li>
 * <li>{@link QueryRenderer inline expressions} such as composition of tokens and expressions such as function calls
 * with parenthesis {@code SOME_FUNCTION(ARGS)}</li>
 * </ul>
 *
 * @author Mark Paluch
 */
abstract class QueryRenderer implements QueryTokenStream<QueryToken> {

	/**
	 * Creates a QueryRenderer from a collection of {@link JpaQueryParsingToken}.
	 *
	 * @param tokens
	 * @return
	 */
	static QueryRenderer from(Collection<? extends QueryToken> tokens) {
		List<QueryToken> tokensToUse = new ArrayList<>(Math.max(tokens.size(), 32));
		tokensToUse.addAll(tokens);
		return new TokenRenderer(tokensToUse);
	}

	/**
	 * Creates a new empty {@link QueryRenderer}.
	 *
	 * @return
	 */
	public static QueryRenderer empty() {
		return EmptyQueryRenderer.INSTANCE;
	}

	/**
	 * Creates a new {@link QueryRendererBuilder}.
	 *
	 * @return
	 */
	static QueryRendererBuilder builder() {
		return new QueryRendererBuilder();
	}

	/**
	 * @return the rendered query.
	 */
	abstract String render();

	/**
	 * Append a {@link QueryRenderer} to create a composed renderer.
	 *
	 * @param renderer
	 * @return
	 */
	QueryRenderer append(QueryRenderer renderer) {
		return CompositeRenderer.combine(this, renderer);
	}

	/**
	 * @return {@code true} if the query renderer represents an expression.
	 */
	public boolean isExpression() {
		return false;
	}

	@Override
	public String toString() {
		return render();
	}

	/**
	 * Composed renderer consisting of one or more QueryRenderers.
	 */
	static class CompositeRenderer extends QueryRenderer {

		private final List<QueryRenderer> nested;
		private int size;

		static CompositeRenderer combine(QueryRenderer root, QueryRenderer nested) {

			List<QueryRenderer> queryRenderers = new ArrayList<>(32);
			queryRenderers.add(root);
			queryRenderers.add(nested);

			return new CompositeRenderer(queryRenderers, root.estimatedSize() + nested.estimatedSize());
		}

		private CompositeRenderer(List<QueryRenderer> nested, int size) {

			this.nested = nested;
			this.size = size;
		}

		@Override
		String render() {

			StringBuilder builder = new StringBuilder(2048);

			boolean lastExpression = false;
			for (QueryRenderer queryRenderer : nested) {

				if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != ' '
						&& (lastExpression || queryRenderer.isExpression())) {
					builder.append(' ');
				}

				builder.append(queryRenderer.render());
				lastExpression = queryRenderer.isExpression();
			}

			return builder.toString();
		}

		@Override
		QueryRenderer append(QueryRenderer renderer) {

			nested.add(renderer);
			this.size += renderer.estimatedSize();
			return this;
		}

		@Override
		public boolean isExpression() {
			return !nested.isEmpty() && nested.get(nested.size() - 1).isExpression();
		}

		@Override
		public Iterator<QueryToken> iterator() {

			CompositeIterator<QueryToken> iterator = new CompositeIterator<>();
			for (QueryRenderer renderer : nested) {
				iterator.add(renderer.iterator());
			}
			return iterator;
		}

		@Override
		public int estimatedSize() {
			return size;
		}
	}

	/**
	 * Renderer using {@link JpaQueryParsingToken}.
	 */
	static class TokenRenderer extends QueryRenderer {

		private final List<QueryToken> tokens;

		TokenRenderer(List<QueryToken> tokens) {
			this.tokens = tokens;
		}

		@Override
		String render() {
			return render(tokens);
		}

		@Override
		QueryRenderer append(QueryRenderer renderer) {

			if (renderer instanceof TokenRenderer tr) {
				this.tokens.addAll(tr.tokens);
				return this;
			}

			return super.append(renderer);
		}

		@Override
		public boolean isExpression() {
			return !tokens.isEmpty() && tokens.get(tokens.size() - 1).isExpression();
		}

		@Override
		public Stream<QueryToken> stream() {
			return tokens.stream();
		}

		@Override
		public Iterator<QueryToken> iterator() {
			return tokens.iterator();
		}

		@Override
		public List<QueryToken> toList() {
			return tokens;
		}

		@Override
		public int estimatedSize() {
			return tokens.size();
		}

		/**
		 * Render a list of {@link JpaQueryParsingToken}s into a string.
		 *
		 * @param tokens
		 * @return rendered string containing either a query or some subset of that query
		 */
		static String render(Object tokens) {

			if (tokens instanceof Collection tpr) {
				return render(tpr);
			}

			if(tokens instanceof QueryRendererBuilder qrb) {
				return qrb.build().render();
			}

			if(tokens instanceof QueryRenderer qr) {
				return qr.render();
			}

			throw new IllegalArgumentException("Unknown token type %s".formatted(tokens));
		}


		static String render(Collection<QueryToken> tokens) {

			StringBuilder results = new StringBuilder();

			boolean previousExpression = false;

			for (QueryToken jpaQueryParsingToken : tokens) {

				if (previousExpression) {
					if (!results.isEmpty() && results.charAt(results.length() - 1) != ' ') {
						results.append(' ');
					}
				}

				previousExpression = jpaQueryParsingToken.isExpression();
				results.append(jpaQueryParsingToken.value());
			}

			return results.toString();
		}

	}

	/**
	 * Builder for {@link QueryRenderer}.
	 */
	static class QueryRendererBuilder implements QueryTokenStream<QueryToken> {

		protected QueryRenderer current = QueryRenderer.empty();

		/**
		 * Compose a {@link QueryRendererBuilder} from a collection of inline elements that can be mapped to
		 * {@link QueryRendererBuilder}.
		 *
		 * @param elements
		 * @param visitor
		 * @param separator
		 * @return
		 * @param <T>
		 */
		public static <T> QueryRendererBuilder concat(Collection<T> elements, Function<T, QueryRendererBuilder> visitor,
			QueryToken separator) {
			return concat(elements, visitor, QueryRendererBuilder::toInline, separator);
		}

		/**
		 * Compose a {@link QueryRendererBuilder} from a collection of expression elements that can be mapped to
		 * {@link QueryRendererBuilder}.
		 *
		 * @param elements
		 * @param visitor
		 * @param separator
		 * @return
		 * @param <T>
		 */
		public static <T> QueryRendererBuilder concatExpressions(Collection<T> elements,
				Function<T, QueryRendererBuilder> visitor, QueryToken separator) {
			return concat(elements, visitor, QueryRendererBuilder::toExpression, separator);
		}

		/**
		 * Compose a {@link QueryRendererBuilder} from a collection of elements that can be mapped to
		 * {@link QueryRendererBuilder}.
		 *
		 * @param elements
		 * @param visitor
		 * @param postProcess post-processing function to convert {@link QueryRendererBuilder} into {@link QueryRenderer}.
		 * @param separator
		 * @return
		 * @param <T>
		 */
		public static <T> QueryRendererBuilder concat(Collection<T> elements, Function<T, QueryRendererBuilder> visitor,
				Function<QueryRendererBuilder, QueryRenderer> postProcess, QueryToken separator) {

			QueryRendererBuilder builder = new QueryRendererBuilder();
			for (T element : elements) {
				if (!builder.isEmpty()) {
					builder.append(separator);
				}
				builder.append(postProcess.apply(visitor.apply(element)));
			}

			return builder;
		}

		/**
		 * Create and initialize a QueryRendererBuilder from a {@link JpaQueryParsingToken}.
		 *
		 * @param token
		 * @return
		 */
		public static QueryRendererBuilder from(QueryToken token) {
			return new QueryRendererBuilder().append(token);
		}

		/**
		 * Append a {@link JpaQueryParsingToken}.
		 *
		 * @param token
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder append(QueryToken token) {
			return append(List.of(token));
		}

		/**
		 * Append a collection of {@link JpaQueryParsingToken}.
		 *
		 * @param tokens
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder append(Collection<? extends QueryToken> tokens) {
			return append(QueryRenderer.from(tokens));
		}

		QueryRendererBuilder append(QueryTokenStream<? extends QueryToken> tokens) {
			return append(QueryRenderer.from(tokens.toList()));
		}

		/**
		 * Append a QueryRendererBuilder.
		 *
		 * @param builder
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder append(QueryRendererBuilder builder) {
			return append(builder.current);
		}

		/**
		 * Append a QueryRendererBuilder as expression.
		 *
		 * @param builder
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder appendExpression(QueryRendererBuilder builder) {
			return appendExpression(builder.current);
		}

		/**
		 * Append a QueryRendererBuilder as inline.
		 *
		 * @param builder
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder appendInline(QueryRendererBuilder builder) {
			return appendInline(builder.current);
		}

		/**
		 * Append a QueryRenderer.
		 *
		 * @param renderer
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder append(QueryRenderer renderer) {

			if (renderer instanceof EmptyQueryRenderer) {
				return this;
			}

			current = current.append(renderer);

			return this;
		}

		/**
		 * Append a QueryRenderer inline.
		 *
		 * @param renderer
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder appendInline(QueryRenderer renderer) {

			if (renderer instanceof EmptyQueryRenderer) {
				return this;
			}

			current = current.append(!renderer.isExpression() ? renderer : new InlineRenderer(renderer));

			return this;
		}

		/**
		 * Append a QueryRenderer as expression.
		 *
		 * @param renderer
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder appendExpression(QueryRenderer renderer) {

			if (renderer instanceof EmptyQueryRenderer) {
				return this;
			}

			current = current.append(renderer.isExpression() ? renderer : new ExpressionRenderer(renderer));

			return this;
		}

		/**
		 * Return whet the builder is empty.
		 *
		 * @return
		 */
		public boolean isEmpty() {
			return current.isEmpty();
		}

		public QueryRenderer build() {
			return current;
		}

		@Override
		public String toString() {
			return current.render();
		}

		private QueryRenderer toExpression() {

			if (current instanceof ExpressionRenderer) {
				return current;
			}

			return new ExpressionRenderer(current);
		}

		public QueryRenderer toInline() {
			return new InlineRenderer(current);
		}

		@Override
		public List<QueryToken> toList() {
			return current.toList();
		}

		@Override
		public Stream<QueryToken> stream() {
			return current.stream();
		}

		@Override
		public int estimatedSize() {
			return current.estimatedSize();
		}

		@Override
		public Iterator<QueryToken> iterator() {
			return current.iterator();
		}
	}

	private static class InlineRenderer extends QueryRenderer {

		private final QueryRenderer delegate;

		private InlineRenderer(QueryRenderer delegate) {
			this.delegate = delegate;
		}

		@Override
		String render() {
			return delegate.render();
		}

		@Override
		public Stream<QueryToken> stream() {
			return delegate.stream();
		}

		@Override
		public List<QueryToken> toList() {
			return delegate.toList();
		}

		@Override
		public Iterator<QueryToken> iterator() {
			return delegate.iterator();
		}

		@Override
		public int estimatedSize() {
			return delegate.estimatedSize();
		}
	}

	private static class ExpressionRenderer extends QueryRenderer {

		private final QueryRenderer delegate;

		private ExpressionRenderer(QueryRenderer delegate) {
			this.delegate = delegate;
		}

		@Override
		String render() {
			return delegate.render();
		}

		@Override
		public boolean isExpression() {
			return true;
		}

		@Override
		public Stream<QueryToken> stream() {
			return delegate.stream();
		}

		@Override
		public List<QueryToken> toList() {
			return delegate.toList();
		}

		@Override
		public Iterator<QueryToken> iterator() {
			return delegate.iterator();
		}

		@Override
		public int estimatedSize() {
			return delegate.estimatedSize();
		}
	}

	private static class EmptyQueryRenderer extends QueryRenderer {

		public static final QueryRenderer INSTANCE = new EmptyQueryRenderer();

		@Override
		String render() {
			return "";
		}

		@Override
		QueryRenderer append(QueryRenderer renderer) {
			return renderer;
		}

		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public List<QueryToken> toList() {
			return Collections.emptyList();
		}

		@Override
		public Stream<QueryToken> stream() {
			return Stream.empty();
		}

		@Override
		public Iterator<QueryToken> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public int estimatedSize() {
			return 0;
		}
	}
}
