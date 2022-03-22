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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.jpa.repository.QueryRewriter;

/**
 * A {@link BeanFactory}-based {@link QueryRewriterProvider}.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
public class QueryRewriterBeanFactoryProvider extends QueryRewriterProvider {

	private static final Log LOGGER = LogFactory.getLog(QueryRewriterBeanFactoryProvider.class);

	private final BeanFactory beanFactory;

	public QueryRewriterBeanFactoryProvider(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	protected QueryRewriter extractQueryRewriterBean(Class<? extends QueryRewriter> queryRewriter) {

		try {
			return beanFactory.getBean(queryRewriter);
		} catch (BeansException e) {
			LOGGER.error(e.toString());
			return null;
		}
	}
}
