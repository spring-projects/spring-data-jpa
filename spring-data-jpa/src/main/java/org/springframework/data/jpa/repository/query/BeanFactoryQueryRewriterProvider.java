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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.util.Lazy;

/**
 * A {@link BeanFactory}-based {@link QueryRewriterProvider}.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.0
 */
public class BeanFactoryQueryRewriterProvider implements QueryRewriterProvider {

	private final BeanFactory beanFactory;

	public BeanFactoryQueryRewriterProvider(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryRewriter getQueryRewriter(JpaQueryMethod method) {

		Class<? extends QueryRewriter> queryRewriter = method.getQueryRewriter();
		if (queryRewriter == QueryRewriter.IdentityQueryRewriter.class) {
			return QueryRewriter.IdentityQueryRewriter.INSTANCE;
		}

		Lazy<QueryRewriter> rewriter = Lazy.of(() -> beanFactory.getBeanProvider((Class<QueryRewriter>) queryRewriter)
				.getIfAvailable(() -> BeanUtils.instantiateClass(queryRewriter)));

		return new DelegatingQueryRewriter(rewriter);
	}
}
