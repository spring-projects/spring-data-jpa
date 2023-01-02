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

/**
 * Encapsulates different strategies for the creation of a {@link QueryEnhancer} from a {@link DeclaredQuery}.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @since 2.7.0
 */
public final class QueryEnhancerFactory {

	private static final Log LOG = LogFactory.getLog(QueryEnhancerFactory.class);

	private static final boolean JSQLPARSER_IN_CLASSPATH = isJSqlParserInClassPath();

	private QueryEnhancerFactory() {}

	/**
	 * Creates a new {@link QueryEnhancer} for the given {@link DeclaredQuery}.
	 *
	 * @param query must not be {@literal null}.
	 * @return an implementation of {@link QueryEnhancer} that suits the query the most
	 */
	public static QueryEnhancer forQuery(DeclaredQuery query) {

		if (qualifiesForJSqlParserUsage(query)) {
			return new JSqlParserQueryEnhancer(query);
		} else {
			return new DefaultQueryEnhancer(query);
		}
	}

	/**
	 * Checks if a given query can be process with the JSqlParser under the condition that the parser is in the classpath.
	 *
	 * @param query the query we want to check
	 * @return <code>true</code> if JSqlParser is in the classpath and the query is classified as a native query otherwise
	 *         <code>false</code>
	 */
	private static boolean qualifiesForJSqlParserUsage(DeclaredQuery query) {
		return JSQLPARSER_IN_CLASSPATH && query.isNativeQuery();
	}

	/**
	 * Checks whether JSqlParser is in classpath or not.
	 *
	 * @return <code>true</code> when in classpath otherwise <code>false</code>
	 */
	private static boolean isJSqlParserInClassPath() {

		try {
			Class.forName("net.sf.jsqlparser.parser.JSqlParser", false, QueryEnhancerFactory.class.getClassLoader());
			LOG.info("JSqlParser is in classpath; If applicable JSqlParser will be used");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
