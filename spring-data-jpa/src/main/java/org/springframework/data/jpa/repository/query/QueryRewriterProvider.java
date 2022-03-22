/*
 * Copyright 2008-2022 the original author or authors.
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

import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.lang.Nullable;

/**
 * Provide a {@link QueryRewriter} based upon the {@link JpaQueryMethod} and the surrounding context (Spring, CDI, etc.)
 *
 * @author Greg Turnquist
 * @since 3.0
 */
public abstract class QueryRewriterProvider {

	/**
	 * Using a {@link JpaQueryMethod}, extract a potential {@link QueryRewriter}. Wrap all this in a {@link Supplier} to
	 * defer the lookup until needed.
	 * 
	 * @param method - JpaQueryMethod
	 * @return a {@link Supplier}-wrapped callback to fetch the {@link QueryRewriter}
	 */
	public Supplier<QueryRewriter> of(JpaQueryMethod method) {
		return () -> findQueryRewriter(method);
	}

	/**
	 * Using the {@link org.springframework.data.jpa.repository.QueryRewrite} annotation, look for a {@link QueryRewriter}
	 * and instantiate one. NOTE: If its {@link QueryRewriter.NoopQueryRewriter}, it will just return {@literal null} and
	 * NOT do any rewrite operations.
	 *
	 * @param method - {@link JpaQueryMethod} that has the annotation details
	 * @return a {@link QueryRewriter for the method or {@code null}
	 */
	@Nullable
	private QueryRewriter findQueryRewriter(JpaQueryMethod method) {

		Class<? extends QueryRewriter> queryRewriter = method.getQueryRewriter();

		if (queryRewriter == null || queryRewriter == QueryRewriter.NoopQueryRewriter.class) {
			return null;
		}

		return extractQueryRewriterBean(queryRewriter);
	}

	/**
	 * Extract an instance of {@link QueryRewriter} from the context. Implementations choose what context means, whether
	 * that is Spring, CDI, or whatever.
	 *
	 * @param queryRewriter
	 * @return a Java bean that implements {@link QueryRewriter}. {@literal null} is valid if no bean is found.
	 */
	@Nullable
	protected abstract QueryRewriter extractQueryRewriterBean(Class<? extends QueryRewriter> queryRewriter);
}
