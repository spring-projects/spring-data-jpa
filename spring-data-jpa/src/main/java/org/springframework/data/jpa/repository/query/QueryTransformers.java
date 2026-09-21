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

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;

/**
 * Utility class encapsulating common query transformations.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.2.5
 */
class QueryTransformers {

	/**
	 * Render the {@code select count(…)} projection of a count query using the given {@code countProjection} or the
	 * primary {@literal FROM} alias falling back to {@code __} if neither is available.
	 *
	 * @param countProjection the count projection to use, if any.
	 * @param primaryFromAlias the primary {@literal FROM} alias, if any.
	 */
	static QueryRendererBuilder selectCount(@Nullable String countProjection, @Nullable String primaryFromAlias) {

		QueryRendererBuilder builder = QueryRenderer.builder();
		builder.append(TOKEN_SELECT_COUNT);

		if (countProjection != null) {
			builder.append(QueryTokens.token(countProjection));
		} else if (primaryFromAlias != null) {
			builder.append(QueryTokens.token(primaryFromAlias));
		} else {
			builder.append(TOKEN_DOUBLE_UNDERSCORE);
		}

		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}
}
