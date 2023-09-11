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
 * @since 3.2.0
 */
public enum QueryEnhancerOption implements QueryEnhancerStrategy {

	/**
	 * Automatically pick the best fit based on availability of JSqlParser and Hibernate.
	 */
	AUTOMATIC_BEST_FIT {
		@Override
		public QueryEnhancer forQuery(DeclaredQuery query) {

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
	},

	/**
	 * Automatically pick the best fit based on availability of Hibernate. NOTE: JSqlParser is <strong>not</strong>
	 * considered, even if it's on the classpath.
	 */
	AUTOMATIC_BEST_FIT_WITHOUT_JSQL {
		@Override
		public QueryEnhancer forQuery(DeclaredQuery query) {

			if (query.isNativeQuery()) {

				LOG.warn("We are NOT evaluating if JSqlParser is on the classpath in order to handle '" + query.getQueryString()
						+ "'");

				return new DefaultQueryEnhancer(query);
			}

			return hibernatePresent ? JpaQueryEnhancer.forHql(query) : JpaQueryEnhancer.forJpql(query);
		}
	},

	MANUAL_HIBERNATE {
		@Override
		public QueryEnhancer forQuery(DeclaredQuery query) {

			LOG.warn("You have chosen HIBERNATE to handle '" + query.getQueryString() + "'");

			return JpaQueryEnhancer.forHql(query);
		}
	},

	MANUAL_JPA {
		@Override
		public QueryEnhancer forQuery(DeclaredQuery query) {

			LOG.warn("You have chose JPA to handle '" + query.getQueryString() + "'");

			return JpaQueryEnhancer.forJpql(query);
		}
	};

	private static final Log LOG = LogFactory.getLog(QueryEnhancerFactory.class);

	private static final boolean jSqlParserPresent = ClassUtils.isPresent("net.sf.jsqlparser.parser.JSqlParser",
			QueryEnhancerOption.class.getClassLoader());

	private static final boolean hibernatePresent = ClassUtils.isPresent("org.hibernate.query.TypedParameterValue",
			QueryEnhancerOption.class.getClassLoader());

	static {

		if (jSqlParserPresent) {
			LOG.info("JSqlParser is in classpath; If applicable, JSqlParser will be used");
		}

		if (hibernatePresent) {
			LOG.info("Hibernate is in classpath; If applicable, HQL parser will be used.");
		}
	}
}
