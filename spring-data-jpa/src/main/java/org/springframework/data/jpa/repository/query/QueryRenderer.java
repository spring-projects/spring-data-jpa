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
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Abstraction to encapsulate query expressions and render a query.
 * <p>
 * Query rendering consists of multiple building blocks:
 * <ul>
 * <li>{@link JpaQueryParsingToken tokens} and
 * {@link org.springframework.data.jpa.repository.query.JpaQueryParsingToken.JpaQueryExpression expression tokens}</li>
 * <li>{@link QueryRenderer compositions} such as a composition of multiple tokens.</li>
 * <li>{@link QueryRenderer expressions} that are individual parts such as {@code SELECT} or {@code ORDER BY …}</li>
 * <li>{@link QueryRenderer inline expressions} such as composition of tokens and expressions such as function calls
 * with parenthesis {@code SOME_FUNCTION(ARGS)}</li>
 * </ul>
 *
 * @author Mark Paluch
 */
abstract class QueryRenderer {

	/**
	 * Creates a QueryRenderer from a collection of {@link JpaQueryParsingToken}.
	 *
	 * @param tokens
	 * @return
	 */
	static QueryRenderer from(Collection<JpaQueryParsingToken> tokens) {
		List<JpaQueryParsingToken> tokensToUse = new ArrayList<>(32);
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
		List<QueryRenderer> objects = new ArrayList<>(32);
		objects.add(this);
		objects.add(renderer);
		return new CompositeRenderer(objects);
	}

	/**
	 * @return {@code true} if the query renderer represents an expression.
	 */
	public boolean isExpression() {
		return false;
	}

	/**
	 * @return stream of tokens.
	 */
	public Stream<JpaQueryParsingToken> stream() {
		return Stream.empty();
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

		CompositeRenderer(List<QueryRenderer> nested) {
			this.nested = new ArrayList<>(nested);
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
			return this;
		}

		@Override
		public boolean isExpression() {
			return !nested.isEmpty() && nested.get(nested.size() - 1).isExpression();
		}

		@Override
		public Stream<JpaQueryParsingToken> stream() {

			Stream<JpaQueryParsingToken> stream = Stream.empty();

			for (QueryRenderer renderer : nested) {
				stream = Stream.concat(stream, renderer.stream());
			}

			return stream;
		}
	}

	/**
	 * Renderer using {@link JpaQueryParsingToken}.
	 */
	static class TokenRenderer extends QueryRenderer {

		private final List<JpaQueryParsingToken> tokens;

		TokenRenderer(List<JpaQueryParsingToken> tokens) {
			this.tokens = tokens;
		}

		@Override
		String render() {
			return JpaQueryParsingToken.render(tokens);
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
			return !tokens.isEmpty() && tokens.get(tokens.size() - 1) instanceof JpaQueryParsingToken.JpaQueryExpression;
		}

		@Override
		public Stream<JpaQueryParsingToken> stream() {
			return tokens.stream();
		}
	}

	/**
	 * Builder for {@link QueryRenderer}.
	 */
	static class QueryRendererBuilder {

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
				JpaQueryParsingToken separator) {
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
				Function<T, QueryRendererBuilder> visitor, JpaQueryParsingToken separator) {
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
				Function<QueryRendererBuilder, QueryRenderer> postProcess, JpaQueryParsingToken separator) {

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
		public static QueryRendererBuilder from(JpaQueryParsingToken token) {
			return new QueryRendererBuilder().append(token);
		}

		/**
		 * Append a {@link JpaQueryParsingToken}.
		 *
		 * @param token
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder append(JpaQueryParsingToken token) {
			return append(List.of(token));
		}

		/**
		 * Append a collection of {@link JpaQueryParsingToken}.
		 *
		 * @param tokens
		 * @return {@code this} builder.
		 */
		QueryRendererBuilder append(Collection<JpaQueryParsingToken> tokens) {
			return append(QueryRenderer.from(tokens));
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
			return current instanceof EmptyQueryRenderer;
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
		public Stream<JpaQueryParsingToken> stream() {
			return delegate.stream();
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
		public Stream<JpaQueryParsingToken> stream() {
			return delegate.stream();
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
	}
}
