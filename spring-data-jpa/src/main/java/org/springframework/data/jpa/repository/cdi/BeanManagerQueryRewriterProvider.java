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
package org.springframework.data.jpa.repository.cdi;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import java.util.Iterator;

import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.DelegatingQueryRewriter;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;
import org.springframework.data.util.Lazy;

/**
 * A {@link BeanManager}-based {@link QueryRewriterProvider}.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.0
 */
public class BeanManagerQueryRewriterProvider implements QueryRewriterProvider {

	private final BeanManager beanManager;

	public BeanManagerQueryRewriterProvider(BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryRewriter getQueryRewriter(JpaQueryMethod method) {

		Class<? extends QueryRewriter> queryRewriter = method.getQueryRewriter();
		if (queryRewriter == QueryRewriter.IdentityQueryRewriter.class) {
			return QueryRewriter.IdentityQueryRewriter.INSTANCE;
		}

		Iterator<Bean<?>> iterator = beanManager.getBeans(queryRewriter).iterator();

		if (iterator.hasNext()) {

			Bean<QueryRewriter> bean = (Bean<QueryRewriter>) iterator.next();
			CreationalContext<QueryRewriter> context = beanManager.createCreationalContext(bean);
			Lazy<QueryRewriter> rewriter = Lazy
					.of(() -> (QueryRewriter) beanManager.getReference(bean, queryRewriter, context));

			return new DelegatingQueryRewriter(rewriter);
		}

		return BeanUtils.instantiateClass(queryRewriter);
	}

}
