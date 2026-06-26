/*
 * Copyright 2022-present the original author or authors.
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

import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.repository.QueryRewriter;

/**
 * Provide a {@link QueryRewriter} based upon the {@link JpaQueryMethod}. {@code QueryRewriter} instances may be
 * contextual or plain objects that are not attached to a bean factory or CDI context.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @author YeongJae Min
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

		return new QueryRewriterProvider() {

			@Override
			public QueryRewriter getQueryRewriter(JpaQueryMethod method) {
				return getQueryRewriter(method.getQueryRewriter());
			}
		};
	}

	/**
	 * Obtain an instance of {@link QueryRewriter} for the given type.
	 *
	 * @param queryRewriter the {@link QueryRewriter} type.
	 * @return a Java bean that implements {@link QueryRewriter}.
	 * @since 4.1
	 */
	default QueryRewriter getQueryRewriter(Class<? extends QueryRewriter> queryRewriter) {
		return getQueryRewriter(queryRewriter, () -> BeanUtils.instantiateClass(queryRewriter));
	}

	/**
	 * Obtain an instance of {@link QueryRewriter} for the given type.
	 *
	 * @param queryRewriter the {@link QueryRewriter} type.
	 * @param fallback fallback to obtain an instance if no contextual instance is available.
	 * @return a Java bean that implements {@link QueryRewriter}.
	 * @since 4.1
	 */
	default QueryRewriter getQueryRewriter(Class<? extends QueryRewriter> queryRewriter,
			Supplier<QueryRewriter> fallback) {

		if (queryRewriter == QueryRewriter.IdentityQueryRewriter.class) {
			return QueryRewriter.IdentityQueryRewriter.INSTANCE;
		}

		return fallback.get();
	}

	/**
	 * Obtain an instance of {@link QueryRewriter} for a {@link JpaQueryMethod}.
	 *
	 * @param method the underlying JPA query method.
	 * @return a Java bean that implements {@link QueryRewriter}.
	 */
	QueryRewriter getQueryRewriter(JpaQueryMethod method);
}
