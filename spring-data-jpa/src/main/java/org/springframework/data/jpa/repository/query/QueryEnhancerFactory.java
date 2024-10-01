/*
 * Copyright 2022-2024 the original author or authors.
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
import org.springframework.core.SpringProperties;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Encapsulates different strategies for the creation of a {@link QueryEnhancer} from a {@link DeclaredQuery}.
 *
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.7.0
 */
public final class QueryEnhancerFactory {

	private static final Log LOG = LogFactory.getLog(QueryEnhancerFactory.class);
	private static final NativeQueryEnhancer NATIVE_QUERY_ENHANCER;

	static {

		NATIVE_QUERY_ENHANCER = NativeQueryEnhancer.select();

		if (PersistenceProvider.ECLIPSELINK.isPresent()) {
			LOG.info("EclipseLink is in classpath; If applicable, EQL parser will be used.");
		}

		if (PersistenceProvider.HIBERNATE.isPresent()) {
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
			return getNativeQueryEnhancer(query);
		}

		if (PersistenceProvider.HIBERNATE.isPresent()) {
			return JpaQueryEnhancer.forHql(query);
		} else if (PersistenceProvider.ECLIPSELINK.isPresent()) {
			return JpaQueryEnhancer.forEql(query);
		} else {
			return JpaQueryEnhancer.forJpql(query);
		}
	}

	/**
	 * Get the native query enhancer for the given {@link DeclaredQuery query} based on {@link #NATIVE_QUERY_ENHANCER}.
	 *
	 * @param query the declared query.
	 * @return new instance of {@link QueryEnhancer}.
	 */
	private static QueryEnhancer getNativeQueryEnhancer(DeclaredQuery query) {

		if (NATIVE_QUERY_ENHANCER.equals(NativeQueryEnhancer.JSQLPARSER)) {
			return new JSqlParserQueryEnhancer(query);
		}

		return new DefaultQueryEnhancer(query);
	}

	/**
	 * Possible choices for the {@link #NATIVE_PARSER_PROPERTY}. Resolve the parser through {@link #select()}.
	 *
	 * @since 3.3.5
	 */
	enum NativeQueryEnhancer {

		AUTO, REGEX, JSQLPARSER;

		static final String NATIVE_PARSER_PROPERTY = "spring.data.jpa.query.native.parser";

		static final boolean JSQLPARSER_PRESENT = ClassUtils.isPresent("net.sf.jsqlparser.parser.JSqlParser", null);

		/**
		 * @return the current selection considering classpath availability and user selection via
		 *         {@link #NATIVE_PARSER_PROPERTY}.
		 */
		static NativeQueryEnhancer select() {

			NativeQueryEnhancer selected = resolve();

			if (selected.equals(NativeQueryEnhancer.JSQLPARSER)) {
				LOG.info("User choice: Using JSqlParser");
				return NativeQueryEnhancer.JSQLPARSER;
			}

			if (selected.equals(NativeQueryEnhancer.REGEX)) {
				LOG.info("Using Regex QueryEnhancer");
				return NativeQueryEnhancer.REGEX;
			}

			if (!JSQLPARSER_PRESENT) {
				return NativeQueryEnhancer.REGEX;
			}

			LOG.info("JSqlParser is in classpath; If applicable, JSqlParser will be used.");
			return NativeQueryEnhancer.JSQLPARSER;
		}

		/**
		 * Resolve {@link NativeQueryEnhancer} from {@link SpringProperties}.
		 *
		 * @return the {@link NativeQueryEnhancer} constant.
		 */
		private static NativeQueryEnhancer resolve() {

			String name = SpringProperties.getProperty(NATIVE_PARSER_PROPERTY);

			if (StringUtils.hasText(name)) {
				return ObjectUtils.caseInsensitiveValueOf(NativeQueryEnhancer.values(), name);
			}

			return AUTO;
		}
	}

}
