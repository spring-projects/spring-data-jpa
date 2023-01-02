/*
 * Copyright 2022-2023 the original author or authors.
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

import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.repository.QueryRewriter;

/**
 * Provide a {@link QueryRewriter} based upon the {@link JpaQueryMethod}. {@code QueryRewriter} instances may be
 * contextual or plain objects that are not attached to a bean factory or CDI context.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.0
 * @see QueryRewriter
 */
public interface QueryRewriterProvider {

	/**
	 * Return a simple {@code QueryRewriterProvider} that uses
	 * {@link org.springframework.beans.BeanUtils#instantiateClass(Class)} to obtain a {@link QueryRewriter} instance.
	 *
	 * @return a simple {@link QueryRewriterProvider}.
	 */
	static QueryRewriterProvider simple() {

		return method -> {

			Class<? extends QueryRewriter> queryRewriter = method.getQueryRewriter();

			if (queryRewriter == QueryRewriter.IdentityQueryRewriter.class) {
				return QueryRewriter.IdentityQueryRewriter.INSTANCE;
			}

			return BeanUtils.instantiateClass(queryRewriter);
		};
	}

	/**
	 * Obtain an instance of {@link QueryRewriter} for a {@link JpaQueryMethod}.
	 *
	 * @param method the underlying JPA query method.
	 * @return a Java bean that implements {@link QueryRewriter}.
	 */
	QueryRewriter getQueryRewriter(JpaQueryMethod method);
}
