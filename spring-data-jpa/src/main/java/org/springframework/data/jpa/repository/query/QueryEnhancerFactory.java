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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ClassUtils;

/**
 * Encapsulates different strategies for the creation of a {@link QueryEnhancer} from a {@link DeclaredQuery}.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 2.7.0
 */
public final class QueryEnhancerFactory {

	private static final Log LOG = LogFactory.getLog(QueryEnhancerFactory.class);

	private static final boolean jSqlParserPresent = ClassUtils.isPresent("net.sf.jsqlparser.parser.JSqlParser",
			QueryEnhancerFactory.class.getClassLoader());

	private static final boolean hibernatePresent = ClassUtils.isPresent("org.hibernate.query.TypedParameterValue",
			QueryEnhancerFactory.class.getClassLoader());

	static {

		if (jSqlParserPresent) {
			LOG.info("JSqlParser is in classpath; If applicable, JSqlParser will be used");
		}

		if (hibernatePresent) {
			LOG.info("Hibernate is in classpath; If applicable, HQL parser will be used.");
		}
	}

	private QueryEnhancerFactory() {}

	/**
	 * Creates a new {@link QueryEnhancer} for the given {@link DeclaredQuery}.
	 *
	 * @param query must not be {@literal null}.
	 * @return an implementation of {@link QueryEnhancer} that suits the query the most
	 */
	public static QueryEnhancer forQuery(DeclaredQuery query) {

		if (query.isNativeQuery()) {

			if (jSqlParserPresent) {
				/*
				 * If JSqlParser fails, throw some alert signaling that people should write a custom Impl.
				 */
				return new JSqlParserQueryEnhancer(query);
			}

			return new DefaultQueryEnhancer(query);
		}

		return hibernatePresent ? JpaQueryEnhancer.forHql(query) : JpaQueryEnhancer.forJpql(query);
	}

}
