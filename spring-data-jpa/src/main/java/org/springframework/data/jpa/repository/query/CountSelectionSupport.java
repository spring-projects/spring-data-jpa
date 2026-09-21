/*
 * Copyright 2026 the original author or authors.
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

import static org.springframework.data.jpa.repository.query.QueryTokens.TOKEN_CLOSE_PAREN;
import static org.springframework.data.jpa.repository.query.QueryTokens.TOKEN_COMMA;
import static org.springframework.data.jpa.repository.query.QueryTokens.TOKEN_COUNT_FUNC;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.util.StringUtils;

/**
 * Support for rendering the selection of a {@code count(…)} query.
 * <p>
 * Abstracts grammar and dialect specifics to retain a common interface for rendering the selection of a count query.
 *
 * @param <S> selection type.
 * @param <C> constructor expression type.
 * @param <A> constructor argument type.
 * @author Christoph Strobl
 * @since 4.2
 */
interface CountSelectionSupport<S, C, A> {

	/**
	 * @return the primary alias, if any.
	 */
	@Nullable
	String getPrimaryAlias();

	/**
	 * @return the count projection (not the actual selection), if any.
	 */
	@Nullable
	String getCountProjection();

	/**
	 * @param selectItem must not be {@literal null}.
	 * @return the rendered {@code selectItem}.
	 */
	QueryTokenStream renderSelectItem(S selectItem);

	/**
	 * @param argument must not be {@literal null}.
	 * @return the rendered constructor {@code argument}.
	 */
	QueryTokenStream renderConstructorArgument(A argument);

	/**
	 * @param selectItems must not be {@literal null}.
	 * @return the fallback selection if the primary alias cannot be counted.
	 */
	QueryTokenStream renderCountSelectionFallback(List<S> selectItems);

	/**
	 * @return {@code true} if the primary alias can be counted.
	 */
	default boolean canUsePrimaryAliasForCount() {
		return true;
	}

	/**
	 * @param selectItem must not be {@literal null}.
	 * @return {@code true} if the given {@code selectItem} selects a constructor expression.
	 */
	default boolean hasConstructorExpression(S selectItem) {
		return getConstructorExpression(selectItem) != null;
	}

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
	 * @return {@code true} if the given constructor argument is a path.
	 */
	boolean isPath(A argument);

	/**
	 * Render the {@code count(…)} selection of a count query, applying {@code DISTINCT} if present.
	 *
	 * @param select the {@code SELECT} token of the select clause.
	 * @param distinct the {@code DISTINCT} token of the select clause, if any.
	 * @param selectItems the select items of the select clause.
	 */
	default QueryTokenStream renderCountSelection(QueryToken select, @Nullable QueryToken distinct, List<S> selectItems) {

		boolean isDistinct = distinct != null && !distinct.isEmpty();

		String countProjection = getCountProjection();
		QueryRendererBuilder nested = QueryRenderer.builder();

		if (isDistinct) {
			nested.append(distinct);
		}

		if (countProjection != null) {
			nested.append(QueryTokens.token(countProjection));
		} else {
			QueryTokenStream selection = isDistinct ? renderDistinctCountSelectItems(selectItems)
					: renderCountSelectItems(selectItems);
			nested.append(selection);
		}

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(select);
		builder.append(TOKEN_COUNT_FUNC);
		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

	/**
	 * Render the selection for {@literal count}.
	 *
	 * @param selectItems the select items of the select clause.
	 * @return the rendered selection items.
	 */
	default QueryTokenStream renderCountSelectItems(List<S> selectItems) {

		String primaryAlias = getPrimaryAlias();

		if (StringUtils.hasText(primaryAlias) && canUsePrimaryAliasForCount()) {
			return QueryTokens.token(primaryAlias);
		}

		return renderCountSelectionFallback(selectItems);
	}

	/**
	 * Render the selection for {@literal count distinct}.
	 * 
	 * @param selectItems the select items of the select clause.
	 * @return the rendered selection items.
	 */
	default QueryTokenStream renderDistinctCountSelectItems(List<S> selectItems) {

		String primaryFromAlias = getPrimaryAlias();

		if (primaryFromAlias != null && isPathOnlySelection(selectItems)) {
			return QueryTokens.token(primaryFromAlias);
		}

		return QueryTokenStream.concat(selectItems, this::renderSelectItem, TOKEN_COMMA);
	}

	/**
	 * Render the arguments of the given constructor expression as selection to count against.
	 * 
	 * @param constructorExpression must not be {@literal null}.
	 * @return the rendered constructor arguments.
	 */
	default QueryTokenStream renderConstructor(C constructorExpression) {
		return QueryTokenStream.concat(getConstructorArguments(constructorExpression), this::renderConstructorArgument,
				TOKEN_COMMA);
	}

	/**
	 * @return {@code true} if the selection contains a constructor expression and all of its arguments are plain paths.
	 */
	@SuppressWarnings("NullAway")
	default boolean isPathOnlySelection(List<S> selectItems) {

		boolean containsConstructorExpression = false;

		for (S selectItem : selectItems) {

			if (!hasConstructorExpression(selectItem)) {
				continue;
			}

			containsConstructorExpression = true;
			C constructorExpression = getConstructorExpression(selectItem);

			for (A argument : getConstructorArguments(constructorExpression)) {
				if (!isPath(argument)) {
					return false;
				}
			}
		}

		return containsConstructorExpression;
	}
}
