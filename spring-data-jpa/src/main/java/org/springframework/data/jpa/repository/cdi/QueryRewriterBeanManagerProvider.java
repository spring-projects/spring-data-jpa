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
package org.springframework.data.jpa.repository.cdi;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;

/**
 * A {@link BeanManager}-based {@link QueryRewriterProvider}.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
public class QueryRewriterBeanManagerProvider extends QueryRewriterProvider {

	private static final Log LOGGER = LogFactory.getLog(QueryRewriterBeanManagerProvider.class);

	private final BeanManager beanManager;

	public QueryRewriterBeanManagerProvider(BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Override
	protected QueryRewriter extractQueryRewriterBean(Class<? extends QueryRewriter> queryRewriter) {

		try {
			Bean<QueryRewriter> bean = (Bean<QueryRewriter>) beanManager.getBeans(queryRewriter).iterator().next();
			CreationalContext<QueryRewriter> context = beanManager.createCreationalContext(bean);
			return (QueryRewriter) beanManager.getReference(bean, queryRewriter, context);
		} catch (Exception e) {
			LOGGER.error(e.toString());
			return null;
		}
	}
}
