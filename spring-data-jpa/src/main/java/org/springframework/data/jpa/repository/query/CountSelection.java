/*
 * Copyright 2026-present the original author or authors.
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

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;

/**
 * Renders the {@code COUNT(…)} selection of a count query.
 * <p>
 * Holds the query facts driving the selection (count projection, primary alias, dialect capabilities) and delegates
 * grammar specifics to a {@link Grammar}.
 *
 * @param <S> select item type.
 * @param <C> constructor expression type.
 * @param <A> constructor argument type.
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.2
 */
class CountSelection<S extends ParseTree, C extends ParseTree, A extends ParseTree> {

	private final @Nullable String countProjection;
	private final @Nullable String primaryAlias;
	private final boolean primaryAliasCountable;
	private final boolean supportsCountStar;
	private final Grammar<S, C, A> grammar;

	/**
	 * @param countProjection the count projection to use instead of the selection, if any.
	 * @param primaryAlias the primary {@literal FROM} alias, if any.
	 * @param primaryAliasCountable whether the primary alias can be counted. Countable aliases typically stem from
	 *          regular queries ({@code SELECT COUNT(c) FROM …}). Non-countable aliases can refer to a CTE or a
	 *          {@code FROM function()} source and fall back to {@code COUNT(*)} or {@code COUNT(1)}.
	 * @param supportsCountStar whether the dialect supports {@code COUNT(*)}, otherwise, falls back to {@code COUNT(1)}
	 *          if the selection is empty.
	 * @param grammar grammar-specific accessors and renderer.
	 */
	CountSelection(@Nullable String countProjection, @Nullable String primaryAlias, boolean primaryAliasCountable,
			boolean supportsCountStar, Grammar<S, C, A> grammar) {

		this.countProjection = countProjection;
		this.primaryAlias = primaryAlias;
		this.primaryAliasCountable = primaryAliasCountable;
		this.supportsCountStar = supportsCountStar;
		this.grammar = grammar;
	}

	/**
	 * Render the {@code SELECT COUNT(…)} clause for a query without a select clause using the count projection or the
	 * primary alias, falling back to {@code __} if neither is available.
	 *
	 * @return the rendered {@code SELECT COUNT(…)} clause.
	 */
	QueryTokenStream render() {

		String primaryAlias = this.primaryAlias;
		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(TOKEN_SELECT_COUNT);

		if (countProjection != null) {
			builder.append(QueryTokens.token(countProjection));
		} else if (primaryAlias == null) {
			builder.append(TOKEN_DOUBLE_UNDERSCORE);
		} else if (primaryAliasCountable) {
			builder.append(QueryTokens.token(primaryAlias));
		} else {
			builder.append(QueryTokens.token("*"));
		}

		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	/**
	 * Render the {@code SELECT COUNT(…)} clause of a select query, applying {@code DISTINCT} if present.
	 *
	 * @param select the {@code SELECT} token of the select clause.
	 * @param distinct the {@code DISTINCT} token of the select clause, if any.
	 * @param selectItems the select items of the select clause.
	 * @return the rendered {@code SELECT COUNT(…)} clause.
	 */
	QueryTokenStream render(QueryToken select, @Nullable QueryToken distinct, List<S> selectItems) {

		QueryRendererBuilder nested = QueryRenderer.builder();

		if (distinct != null) {
			nested.append(distinct);
		}

		if (countProjection != null) {
			nested.append(QueryTokens.token(countProjection));
		} else {
			nested.append(distinct != null ? renderDistinctSelectItems(selectItems) : renderSelectItems(selectItems));
		}

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(select);
		builder.append(TOKEN_COUNT_FUNC);
		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	/**
	 * Render the arguments of the given constructor expression as selection to count against.
	 *
	 * @param constructorExpression must not be {@literal null}.
	 * @return the rendered constructor arguments.
	 */
	QueryTokenStream renderConstructor(C constructorExpression) {
		return QueryTokenStream.concat(grammar.getConstructorArguments(constructorExpression), grammar::visit, TOKEN_COMMA);
	}

	private QueryTokenStream renderSelectItems(List<S> selectItems) {

		String primaryAlias = this.primaryAlias;

		if (primaryAlias != null && primaryAliasCountable) {
			return QueryTokens.token(primaryAlias);
		}

		if (supportsCountStar) {
			return QueryTokens.token("*");
		}

		// cannot happen as per grammar, but you never know…
		if (selectItems.isEmpty()) {
			return QueryTokens.token("1");
		}

		// count(*) is not supported - use first select item
		return grammar.visit(selectItems.get(0));
	}

	private QueryTokenStream renderDistinctSelectItems(List<S> selectItems) {

		String primaryAlias = this.primaryAlias;

		if (primaryAlias != null && primaryAliasCountable && hasPathOnlyConstructorExpression(selectItems)) {
			return QueryTokens.token(primaryAlias);
		}

		return QueryTokenStream.concat(selectItems, grammar::visit, TOKEN_COMMA);
	}

	/**
	 * @return {@code true} if the selection contains a constructor expression and all of its arguments are plain paths.
	 */
	private boolean hasPathOnlyConstructorExpression(List<S> selectItems) {

		boolean containsConstructorExpression = false;

		for (S selectItem : selectItems) {

			C constructorExpression = grammar.getConstructorExpression(selectItem);

			if (constructorExpression == null) {
				continue;
			}

			containsConstructorExpression = true;

			for (A argument : grammar.getConstructorArguments(constructorExpression)) {
				if (!grammar.isPath(argument)) {
					return false;
				}
			}
		}

		return containsConstructorExpression;
	}

	/**
	 * Grammar-specific accessors for select items and constructor expressions. Select items and constructor arguments are
	 * rendered through {@link #visit(ParseTree)}.
	 *
	 * @param <S> select item type.
	 * @param <C> constructor expression type.
	 * @param <A> constructor argument type.
	 */
	interface Grammar<S extends ParseTree, C extends ParseTree, A extends ParseTree>
			extends ParseTreeVisitor<QueryTokenStream> {

		/**
		 * @param selectItem must not be {@literal null}.
		 * @return the constructor expression of the given {@code selectItem} or {@literal null} if the select item is none.
		 */
		@Nullable
		C getConstructorExpression(S selectItem);

		/**
		 * @param constructorExpression must not be {@literal null}.
		 * @return the arguments of the given constructor expression.
		 */
		List<A> getConstructorArguments(C constructorExpression);

		/**
		 * @param argument must not be {@literal null}.
		 * @return {@code true} if the given constructor argument is a plain path.
		 */
		boolean isPath(A argument);

	}

}
