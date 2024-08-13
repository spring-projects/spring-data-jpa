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

import static org.springframework.data.jpa.repository.query.QueryTokens.*;

import org.springframework.data.repository.query.ReturnedType;

/**
 * HQL Query Transformer that rewrites the query using constructor expressions.
 * <p>
 * Query rewriting from a plain property/object selection towards constructor expression only works if either:
 * <ul>
 * <li>The query selects its primary alias ({@code SELECT p FROM Person p})</li>
 * <li>The query specifies a property list ({@code SELECT p.foo, p.bar FROM Person p})</li>
 * </ul>
 *
 * @author Mark Paluch
 */
class DtoProjectionTransformerDelegate {

	private final ReturnedType returnedType;

	public DtoProjectionTransformerDelegate(ReturnedType returnedType) {
		this.returnedType = returnedType;
	}

	public QueryTokenStream transformSelectionList(QueryTokenStream selectionList) {

		if (!returnedType.isProjecting() || selectionList.stream().anyMatch(it -> it.equals(TOKEN_NEW))) {
			return selectionList;
		}

		QueryRenderer.QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(QueryTokens.TOKEN_NEW);
		builder.append(QueryTokens.token(returnedType.getReturnedType().getName()));
		builder.append(QueryTokens.TOKEN_OPEN_PAREN);

		// assume the selection points to the document
		if (selectionList.size() == 1) {

			builder.appendInline(QueryTokenStream.concat(returnedType.getInputProperties(), property -> {

				QueryRenderer.QueryRendererBuilder prop = QueryRenderer.builder();
				prop.append(QueryTokens.token(selectionList.getFirst().value()));
				prop.append(QueryTokens.TOKEN_DOT);
				prop.append(QueryTokens.token(property));

				return prop.build();
			}, QueryTokens.TOKEN_COMMA));

		} else {
			builder.appendInline(selectionList);
		}

		builder.append(QueryTokens.TOKEN_CLOSE_PAREN);

		return builder.build();
	}
}
