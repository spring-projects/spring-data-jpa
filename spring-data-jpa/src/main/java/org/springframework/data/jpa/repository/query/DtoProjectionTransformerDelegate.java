/*
 * Copyright 2024-present the original author or authors.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.Lazy;

/**
 * HQL Query Transformer that rewrites the query using constructor expressions.
 * <p>
 * Query rewriting from a plain property/object selection towards constructor expression only works if either:
 * <ul>
 * <li>The query selects its primary alias ({@code SELECT p FROM Person p})</li>
 * <li>The query specifies a property list ({@code SELECT p.foo, p.bar FROM Person p},
 * {@code SELECT COUNT(p.foo), p.bar AS bar FROM Person p})</li>
 * </ul>
 *
 * @author Mark Paluch
 * @author HeeHoon Hong
 * @since 3.5
 */
class DtoProjectionTransformerDelegate {

	private final ReturnedType returnedType;
	private final Lazy<Boolean> applyRewriting;
	private final List<QueryTokenStream> selectItems = new ArrayList<>();

	public DtoProjectionTransformerDelegate(ReturnedType returnedType) {
		this.returnedType = returnedType;
		this.applyRewriting = Lazy.of(() -> returnedType.isDtoProjection()
				&& returnedType.needsCustomConstruction());
	}

	public boolean applyRewriting() {
		return applyRewriting.get();
	}

	public boolean canRewrite() {

		if (selectItems.isEmpty() || !applyRewriting()) {
			return false;
		}

		// Avoid rewriting if the selection list is already expanded and the target type is a record
		// to leverage JPA 4.0 implicit DTO mapping.
		boolean expansion = selectItems.size() == 1 && selectItems.get(0).size() == 1;

		if (!expansion && returnedType.getReturnedType().isRecord()) {
			return false;
		}

		return true;
	}

	public void appendSelectItem(QueryTokenStream selectItem) {

		if (applyRewriting()) {
			selectItems.add(new DetachedStream(selectItem));
		}
	}

	public QueryTokenStream getRewrittenSelectionList() {

		if (canRewrite()) {

			QueryRenderer.QueryRendererBuilder builder = QueryRenderer.builder();
			builder.append(QueryTokens.TOKEN_NEW);
			builder.append(QueryTokens.token(returnedType.getReturnedType().getName()));
			builder.append(QueryTokens.TOKEN_OPEN_PAREN);

			if (selectItems.size() == 1 && selectItems.get(0).size() == 1) {

				builder.appendInline(QueryTokenStream.concat(returnedType.getInputProperties(), property -> {

					QueryRenderer.QueryRendererBuilder prop = QueryRenderer.builder();
					prop.appendInline(selectItems.get(0));
					prop.append(QueryTokens.TOKEN_DOT);
					prop.append(QueryTokens.token(property));

					return prop.build();
				}, QueryTokens.TOKEN_COMMA));
			} else {
				builder.append(QueryTokenStream.concat(selectItems, Function.identity(), TOKEN_COMMA));
			}

			builder.append(TOKEN_CLOSE_PAREN);

			return builder.build();
		}

		return QueryTokenStream.empty();
	}

	private static class DetachedStream extends QueryRenderer {

		private final QueryTokenStream delegate;

		private DetachedStream(QueryTokenStream delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean isExpression() {
			return delegate.isExpression();
		}

		@Override
		public int size() {
			return delegate.size();
		}

		@Override
		public boolean isEmpty() {
			return delegate.isEmpty();
		}

		@Override
		public Iterator<QueryToken> iterator() {
			return delegate.iterator();
		}

		@Override
		public String render() {
			return delegate instanceof QueryRenderer ? ((QueryRenderer) delegate).render() : delegate.toString();
		}
	}

}
